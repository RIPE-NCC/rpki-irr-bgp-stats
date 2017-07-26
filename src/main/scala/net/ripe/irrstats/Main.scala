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
import net.ripe.irrstats.parsing.ris.RisDumpUtil
import net.ripe.irrstats.parsing.roas.RoaUtil
import analysis._
import net.ripe.irrstats.parsing.rirs.{ExtendedStatsUtils, CountryHoldings, RIRHoldings}
import net.ripe.irrstats.parsing.route.RouteParser
import net.ripe.irrstats.reporting.{CountryDetails, WorldMapPage}
import net.ripe.rpki.validator.bgp.preview.{BgpAnnouncement, BgpAnnouncementValidator}
import net.ripe.rpki.validator.models.RtrPrefix
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
                    countries: Boolean = false,
                    countryDetails: Option[String] = None,
                    worldmap: Boolean = false,
                    asn: Boolean = false) {

    def routeAuthorisationType(): RouteAuthorisationDumpType = {
      if (routeAuthorisationFile.getName.endsWith(".csv")) {
        RoaCsvDump
      } else {
        RouteObjectDbDump
      }
    }
  }


  val cliOptionParser = new scopt.OptionParser[Config](ApplicationName) {
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
    opt[String]('x', "country-details")  optional() action { (x, c) => c.copy(countries = true, countryDetails = Some(x)) } text { "Do a detailed announcement report for country code" }

    opt[Unit]('c', "countries") optional() action { (x, c) => c.copy(countries = true) } text { "Do a report per country instead of per RIR" }
    opt[Unit]('w', "worldmap") optional() action { (x, c) => c.copy(worldmap = true, countries = true) } text { "Produce an HTML page with country stats projected on a number of world maps" }

    opt[Unit]('a', "asn") optional() action { (x, c) => c.copy(asn = true) } text { "Find and report top ASNs" }

    checkConfig { c =>
      if (!c.routeAuthorisationFile.getName.endsWith(".csv") && !c.routeAuthorisationFile.getName.endsWith(".txt") ) failure("option -r must refer roas.csv or route[6].db file") else success }

  }

  cliOptionParser.parse(args, Config()) match {
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

      val announcementsByRegion: Map[String, Seq[BgpAnnouncement]] = announcements.groupBy { ann => regionFor(ann.prefix, holdings) }
      val authorisationsByRegion: Map[String, Seq[RtrPrefix]] = authorisations.groupBy( pfx => regionFor(pfx.prefix, holdings))

      implicit val actorSystem = akka.actor.ActorSystem()

      def regionAnnouncementStats(region: String) = {
        val announcements = announcementsByRegion.getOrElse(region, Seq.empty)
        val authorisations = authorisationsByRegion.getOrElse(region, Seq.empty)

        val validator = new BgpAnnouncementValidator()
        validator.startUpdate(announcements, authorisations)
        val validatedAnnouncements = validator.validatedAnnouncements

        AnnouncementStatsUtil.analyseValidatedAnnouncements(validatedAnnouncements)
      }


      def reportRegionQuality(region: String) = {

        val authorisations = authorisationsByRegion.getOrElse(region, Seq.empty)
        val stats = regionAnnouncementStats(region)

        def doubleOptionToString(fo: Option[Double]) = fo match {
          case None => ""
          case Some(f) => f"${f}%1.4f"
        }

        println(s"${config.date}, ${region}, ${authorisations.size}, ${stats.combined.count}, ${doubleOptionToString(stats.accuracyAnnouncements)}, " +
                s"${doubleOptionToString(stats.percentageValid)}, ${doubleOptionToString(stats.percentageInvalidLength)}, " +
                s"${doubleOptionToString(stats.percentageInvalidAsn)}, " +
                s"${doubleOptionToString(stats.percentageUnknown)}, ${stats.combined.numberOfIps}, " +
                s"${doubleOptionToString(stats.accuracySpace)}, ${doubleOptionToString(stats.percentageSpaceValid)}, ${doubleOptionToString(stats.percentageSpaceInvalidLength)}, " +
                s"${doubleOptionToString(stats.percentageSpaceInvalidAsn)}, " +
                s"${doubleOptionToString(stats.percentageSpaceUnknown)}")
      }

      def printHeader(): Unit = println("date, RIR, authorisations, announcements, accuracy announcements, " +
                "fraction valid, fraction invalid length, fraction invalid asn, fraction unknown, " +
                "space announced, accuracy space, " +
                "fraction space valid, fraction space invalid length, fraction space invalid asn, " +
                "fraction space unknown")


      if (config.asn) {
        val validator = new BgpAnnouncementValidator()
        validator.startUpdate(announcements, authorisations)
        val validatedAnnouncements = validator.validatedAnnouncements

        println("Asn, PfxAnn, PfxValid, PfxInvalid, SpaceAnn, SpaceValid, SpaceInvalid")

        AsnStatsAnalyser.statsPerAsn(validatedAnnouncements).toList.sortBy(_.spaceValid).reverse.take(100).foreach(stat =>
          println(s"${stat.asn}, ${stat.numberOfAnnouncements}, ${stat.numberValidAnnouncements}, ${stat.numberInvalidAnnouncements}, ${stat.spaceAnnounced}, ${stat.spaceValid}, ${stat.spaceInvalid}")
        )

        sys.exit(0)
      }


      if (config.worldmap) {
        val countryStats = holdings.keys.map { cc =>
          WorldMapCountryStat.fromCcAndStats(cc, regionAnnouncementStats(cc))
        }

        val prefixesAdoptionValues = countryStats.filter(cs => cs.prefixesAdoption.isDefined).map{ cs => (cs.countryCode -> cs.prefixesAdoption.get) }.toMap
        val prefixesValidValues = countryStats.filter(cs => cs.prefixesValid.isDefined).map{ cs => (cs.countryCode -> cs.prefixesValid.get) }.toMap
        val prefixesMatchingValues = countryStats.filter(cs => cs.prefixesMatching.isDefined).map{ cs => (cs.countryCode -> cs.prefixesMatching.get) }.toMap
        val adoptionValues = countryStats.filter(cs => cs.adoption.isDefined).map{ cs => (cs.countryCode -> cs.adoption.get) }.toMap
        val validValues = countryStats.filter(cs => cs.valid.isDefined).map{ cs => (cs.countryCode -> cs.valid.get) }.toMap
        val matchingValues = countryStats.filter(cs => cs.matching.isDefined).map{ cs => (cs.countryCode -> cs.matching.get) }.toMap

        print(WorldMapPage.printWorldMapHtmlPage(prefixesAdoptionValues, prefixesValidValues, prefixesMatchingValues, adoptionValues, validValues, matchingValues))

      } else if (config.countryDetails != None) {
        val cc = config.countryDetails.get
        CountryDetails.printCountryAnnouncementReport(cc, regionAnnouncementStats(cc))
      } else {
        if (!config.quiet) {
          printHeader()
        }

        if (config.rir == "all") {
          for (rir <- holdings.keys) {
            reportRegionQuality(rir)
          }
        } else {
          reportRegionQuality(config.rir)
        }
      }

      sys.exit(0)

    }
  }




  sealed trait RouteAuthorisationDumpType
  case object RoaCsvDump extends RouteAuthorisationDumpType
  case object RouteObjectDbDump extends RouteAuthorisationDumpType
}
