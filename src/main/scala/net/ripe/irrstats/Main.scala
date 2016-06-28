/**
 * Copyright (c) 2015 RIPE NCC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *   - Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   - Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   - Neither the name of this software, nor the names of its contributors, nor
 *     the names of the contributors' employers may be used to endorse or promote
 *     products derived from this software without specific prior written
 *     permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package net.ripe.irrstats

import java.io.File
import java.math.BigInteger

import net.ripe.ipresource.{IpRange, IpResourceSet}
import net.ripe.irrstats.rirs.{ExtendedStatsUtils, CountryHoldings, RIRHoldings}
import net.ripe.irrstats.ris.RisDumpUtil
import net.ripe.irrstats.roas.RoaUtil
import net.ripe.irrstats.route.RouteParser
import net.ripe.rpki.validator.bgp.preview.{BgpAnnouncement, BgpValidatedAnnouncement, BgpAnnouncementValidator}
import net.ripe.rpki.validator.models.{RouteValidity, RtrPrefix}
import org.joda.time.DateTime

import scala.language.postfixOps


object Main extends App {

  private val ApplicationName = "rpki-irr-bgp-stats"
  private val ApplicationVersion = "0.1"

  case class Config(risDumpFile: File = new File("."),
                    statsFile: File = new File("."),
                    routeAuthorisationFile: File = new File("."),
                    quiet: Boolean = false,
                    date: String = DateTime.now().toString("YYYYMMdd"),
                    rir: String = "all",
                    countries: Boolean = false) {

    def routeAuthorisationType(): RouteAuthorisationDumpType = {
      if (routeAuthorisationFile.getName.endsWith(".csv")) {
        RoaCsvDump
      } else {
        RouteObjectDbDump
      }
    }
  }


  val parser = new scopt.OptionParser[Config](ApplicationName) {
    head(ApplicationName, ApplicationVersion)

    opt[File]('b', "bgp-announcements") required() valueName("<file>") action { (x, c) =>
      c.copy(risDumpFile = x) } text { "Location of a RIS Dump style file with BGP announcements " }
    opt[File]('s', "extended-delegated-stats") required() valueName("<file>") action { (x, c) =>
      c.copy(statsFile = x) } text { "Location of a copy of the NRO extended delegated stats" }
    opt[File]('r', "route-authorisations") required() valueName("<file>") action { (x, c) =>
      c.copy(routeAuthorisationFile = x) } text { "Location of a file with either ROA export from the RIPE NCC RPKI Validator (.csv) or route[6] objects (.txt)" }
    opt[Unit]('q', "quiet") optional() action { (x, c) => c.copy(quiet = true) } text { "Quiet output, just (real) numbers" }
    opt[String]('d', "date") optional() action { (x, c) => c.copy(date = x) } text { "Override date string, defaults to today"}
    opt[String]('r', "rir") optional() action { (x, c) => c.copy(rir = x) } text { "Only show results for specified rir, defaults to all"}

    opt[Unit]('c', "countries") optional() action { (x, c) => c.copy(countries = true) } text { "Do a report per country instead of per RIR" }

    checkConfig { c =>
      if (!c.routeAuthorisationFile.getName.endsWith(".csv") && !c.routeAuthorisationFile.getName.endsWith(".txt") ) failure("option -r must refer roas.csv or route[6].db file") else success }

  }

  parser.parse(args, Config()) match {
    case None => // Ignore - help text will be printed  by library if required arguments are not passed
    case Some(config) => {

      // Main method..

      import ExtendedStatsUtils._

      val announcements: Seq[BgpAnnouncement] = RisDumpUtil.parseDumpFile(config.risDumpFile)

      val authorisations: Seq[RtrPrefix] = config.routeAuthorisationType() match {
        case RoaCsvDump => RoaUtil.parse(config.routeAuthorisationFile)
        case RouteObjectDbDump => RouteParser.parse(config.routeAuthorisationFile).map(r => RtrPrefix(r.asn, r.prefix))
      }

      val holdings = if (config.countries == false) {
        RIRHoldings.parse(config.statsFile)
      } else {
        CountryHoldings.parse(config.statsFile)
      }

      val announcementsPerHolder: Map[String, Seq[BgpAnnouncement]] = announcements.groupBy { ann => majorityFor(ann.prefix, holdings) }
      val authorisationsPerHolder: Map[String, Seq[RtrPrefix]] = authorisations.groupBy( pfx => majorityFor(pfx.prefix, holdings))


      implicit val actorSystem = akka.actor.ActorSystem()

      def reportRiRQuality(rir: String) = {

        val rirAnnouncements = announcementsPerHolder.getOrElse(rir, Seq.empty)
        val rirAuthorisations = authorisationsPerHolder.getOrElse(rir, Seq.empty)

        val validator = new BgpAnnouncementValidator()
        validator.startUpdate(rirAnnouncements, rirAuthorisations)
        val validatedAnnouncements = validator.validatedAnnouncements

        val stats = analyseValidatedAnnouncements(validatedAnnouncements)

        // Returns "-" for division by zero
        def safePercentage(fraction: Int, total: Int) = if (total == 0) {
          ""
        } else {
          f"${(fraction.toFloat / total)}%1.4f"
        }

        def safePercentageBig(fraction: BigInteger, total: BigInteger) = if (total.equals(BigInteger.ZERO)) {
          ""
        } else {
          f"${fraction.multiply(BigInteger.valueOf(10000)).divide(total).floatValue / 10000}%1.4f"
        }

        def safePercentageIpSpace(fraction: ValidatedAnnouncementStat) = safePercentageBig(fraction.numberOfIps, (stats.combined.numberOfIps))
        def safePercentageAnnouncements(fraction: ValidatedAnnouncementStat) = safePercentage(fraction.count, stats.combined.count)

        val accuracyAnnouncements = safePercentage(stats.valid.count , (stats.valid.count + stats.invalidAsn.count + stats.invalidLength.count))
        val accuracyAnnouncementsFiltered = safePercentage(stats.valid.count, (stats.valid.count + stats.invalidAsnFiltered.count + stats.invalidLengthFiltered.count))
        val accuracySpace = safePercentageBig(stats.valid.numberOfIps, (stats.valid.numberOfIps.add(stats.invalidAsn.numberOfIps).add(stats.invalidLength.numberOfIps)))
        val accuracySpaceFiltered = safePercentageBig(stats.valid.numberOfIps, (stats.valid.numberOfIps.add(stats.invalidAsnFiltered.numberOfIps).add(stats.invalidLengthFiltered.numberOfIps)))

        println(s"${config.date}, ${rir}, ${rirAuthorisations.size}, ${stats.combined.count}, ${accuracyAnnouncements}, " +
                s"${accuracyAnnouncementsFiltered}, ${safePercentageAnnouncements(stats.valid)}, ${safePercentageAnnouncements(stats.invalidLength)}, " +
                s"${safePercentageAnnouncements(stats.invalidLengthFiltered)}, ${safePercentageAnnouncements(stats.invalidAsn)}, " +
                s"${safePercentageAnnouncements(stats.invalidAsnFiltered)}, ${safePercentageAnnouncements(stats.unknown)}, ${stats.combined.numberOfIps}, " +
                s"${accuracySpace}, ${accuracySpaceFiltered}, ${safePercentageIpSpace(stats.valid)}, ${safePercentageIpSpace(stats.invalidLength)}, " +
                s"${safePercentageIpSpace(stats.invalidLengthFiltered)}, ${{safePercentageIpSpace(stats.invalidAsn)}}, " +
                s"${{safePercentageIpSpace(stats.invalidAsnFiltered)}}, ${{safePercentageIpSpace(stats.unknown)}}")


      }

      def printHeader(): Unit = println("date, RIR, authorisations, announcements, accuracy announcements, accuracy announcements filtered, " +
                "fraction valid, fraction invalid length, fraction invalid length filtered, fraction invalid asn, fraction invalid asn filtered, fraction unknown, " +
                "space announced, accuracy space, accuracy space filtered, " +
                "fraction space valid, fraction space invalid length, fraction space invalid length filtered, fraction space invalid asn, " +
                "fraction space invalid asn filtered, fraction space unknown")

      if(!config.quiet) {
        printHeader()
      }

      if(config.rir == "all") {
        for (rir <- holdings.keys) {
          reportRiRQuality(rir)
        }
      } else {
        reportRiRQuality(config.rir)
      }

      sys.exit(0)

    }
  }

  case class ValidatedAnnouncementStat(count: Integer, numberOfIps: BigInteger)
  case class ValidatedAnnouncementStats(combined: ValidatedAnnouncementStat,
                                        valid: ValidatedAnnouncementStat,
                                        invalidLength: ValidatedAnnouncementStat,
                                        invalidLengthFiltered: ValidatedAnnouncementStat,
                                        invalidAsn: ValidatedAnnouncementStat,
                                        invalidAsnFiltered: ValidatedAnnouncementStat,
                                        unknown: ValidatedAnnouncementStat
                                       )

  def analyseValidatedAnnouncements(announcements: Seq[BgpValidatedAnnouncement]) = {

    import scala.collection.JavaConverters._

    def getNumberOfAddresses(prefixes: Seq[IpRange]) = {
      val resourceSet = new IpResourceSet()
      prefixes.foreach(pfx => resourceSet.addAll(new IpResourceSet(pfx)))
      resourceSet.iterator().asScala.foldLeft(BigInteger.ZERO)((r, c) => {
        r.add(c.getEnd.getValue.subtract(c.getStart.getValue).add(BigInteger.ONE))
      })
    }

    val valid = announcements.filter(_.validity == RouteValidity.Valid)
    val invalidLength = announcements.filter(_.validity == RouteValidity.InvalidLength)
    val invalidAsn = announcements.filter(_.validity == RouteValidity.InvalidAsn)
    val unknown = announcements.filter(_.validity == RouteValidity.Unknown)
    val filteredInvalidLength = invalidLength.filter(a => valid.find(_.prefix.overlaps(a.prefix)) == None) // count if no covering valid found
    val filteredInvalidAsn = invalidAsn.filter(a => valid.find(_.prefix.overlaps(a.prefix)) == None) // count if no covering valid found

    ValidatedAnnouncementStats(
      combined = ValidatedAnnouncementStat(announcements.size, getNumberOfAddresses(announcements.map(_.prefix))),
      valid = ValidatedAnnouncementStat(valid.size, getNumberOfAddresses(valid.map(_.prefix))),
      invalidLength = ValidatedAnnouncementStat(invalidLength.size, getNumberOfAddresses(invalidLength.map(_.prefix))),
      invalidLengthFiltered = ValidatedAnnouncementStat(filteredInvalidLength.size, getNumberOfAddresses(filteredInvalidLength.map(_.prefix))),
      invalidAsn = ValidatedAnnouncementStat(invalidAsn.size, getNumberOfAddresses(invalidAsn.map(_.prefix))),
      invalidAsnFiltered = ValidatedAnnouncementStat(filteredInvalidAsn.size, getNumberOfAddresses(filteredInvalidAsn.map(_.prefix))),
      unknown = ValidatedAnnouncementStat(unknown.size, getNumberOfAddresses(unknown.map(_.prefix)))
    )
  }


  sealed trait RouteAuthorisationDumpType
  case object RoaCsvDump extends RouteAuthorisationDumpType
  case object RouteObjectDbDump extends RouteAuthorisationDumpType
}
