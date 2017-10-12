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
/**
  * Copyright (c) 2015 RIPE NCC
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions are met:
  *   - Redistributions of source code must retain the above copyright notice,
  * this list of conditions and the following disclaimer.
  *   - Redistributions in binary form must reproduce the above copyright notice,
  * this list of conditions and the following disclaimer in the documentation
  * and/or other materials provided with the distribution.
  *   - Neither the name of this software, nor the names of its contributors, nor
  * the names of the contributors' employers may be used to endorse or promote
  * products derived from this software without specific prior written
  * permission.
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

import net.ripe.irrstats.parsing.ris.RisDumpUtil
import net.ripe.irrstats.parsing.roas.RoaUtil
import analysis._
import net.ripe.irrstats.parsing.rirs.{CountryHoldings, ExtendedStatsUtils, RIRHoldings}
import net.ripe.irrstats.parsing.route.RouteParser
import net.ripe.irrstats.reporting.{CountryDetails, RegionCsv, WorldMapPage}
import net.ripe.rpki.validator.bgp.preview.{BgpAnnouncement, BgpAnnouncementValidator, BgpValidatedAnnouncement}
import net.ripe.rpki.validator.models.RtrPrefix

import scala.language.postfixOps


object Main extends App {

  import ExtendedStatsUtils._

  val config = Config.config(args)

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
  val authorisationsByRegion: Map[String, Seq[RtrPrefix]] = authorisations.groupBy(pfx => regionFor(pfx.prefix, holdings))
  
  implicit val actorSystem = akka.actor.ActorSystem()

  def regionAnnouncementStats(region: String): ValidatedAnnouncementStats = {

    val announcements = announcementsByRegion.getOrElse(region, Seq.empty)
    val authorisations = authorisationsByRegion.getOrElse(region, Seq.empty)

    val validator = new BgpAnnouncementValidator()
    validator.startUpdate(announcements, authorisations)
    val validatedAnnouncements = validator.validatedAnnouncements

    AnnouncementStatsUtil.analyseValidatedAnnouncements(validatedAnnouncements, authorisations.size)
  }

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

    val prefixesAdoptionValues = countryStats.filter(cs => cs.prefixesAdoption.isDefined).map { cs => (cs.countryCode -> cs.prefixesAdoption.get) }.toMap
    val prefixesValidValues = countryStats.filter(cs => cs.prefixesValid.isDefined).map { cs => (cs.countryCode -> cs.prefixesValid.get) }.toMap
    val prefixesMatchingValues = countryStats.filter(cs => cs.prefixesMatching.isDefined).map { cs => (cs.countryCode -> cs.prefixesMatching.get) }.toMap
    val adoptionValues = countryStats.filter(cs => cs.adoption.isDefined).map { cs => (cs.countryCode -> cs.adoption.get) }.toMap
    val validValues = countryStats.filter(cs => cs.valid.isDefined).map { cs => (cs.countryCode -> cs.valid.get) }.toMap
    val matchingValues = countryStats.filter(cs => cs.matching.isDefined).map { cs => (cs.countryCode -> cs.matching.get) }.toMap

    print(WorldMapPage.printWorldMapHtmlPage(prefixesAdoptionValues, prefixesValidValues, prefixesMatchingValues, adoptionValues, validValues, matchingValues))

  } else if (config.countryDetails != None) {
    val cc = config.countryDetails.get
    CountryDetails.printCountryAnnouncementReport(cc, regionAnnouncementStats(cc))
  } else {
    if (!config.quiet) {
      RegionCsv.printHeader()
    }

    if (config.rir == "all") {
      for (rir <- holdings.keys) {
        RegionCsv.reportRegionQuality(rir, regionAnnouncementStats(rir), config.date)
      }
    } else {
      RegionCsv.reportRegionQuality(config.rir, regionAnnouncementStats(config.rir), config.date)
    }
  }

  sys.exit(0)
}
