/*
 * Copyright (c) 2015-2021 RIPE NCC
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

import net.ripe.irrstats.analysis.RegionStats
import net.ripe.irrstats.parsing.holdings.Holdings._
import net.ripe.irrstats.reporting.{CountryDetails, RegionCsv}
import net.ripe.irrstats.route.validation.{BgpAnnouncement, RtrPrefix}

import scala.collection.parallel.CollectionConverters._

object ReportCountry {

  def reportCountryDetails(announcements: Seq[BgpAnnouncement], authorisations: Seq[RtrPrefix], countryHolding: Holdings, countryCode: String) = {
    val countryStats = new RegionStats(countryHolding, announcements, authorisations)
    CountryDetails.printCountryAnnouncementReport(countryCode, countryStats.regionAnnouncementStats(countryCode), countryStats.regionAdoptionStats(countryCode))
  }

  def reportCountries(announcements: Seq[BgpAnnouncement], authorisations: Seq[RtrPrefix], countryHolding: Holdings, quiet: Boolean, dateString: String) = {
    if (! quiet) {
      RegionCsv.printHeader()
    }

    val countryStats = new RegionStats(countryHolding, announcements, authorisations)

    countryHolding.keys.par.foreach(cc => RegionCsv.reportRegionQuality(cc, countryStats.regionAnnouncementStats(cc), dateString, countryStats.regionAdoptionStats(cc)))
  }

  def reportRIPECountryRoas(ripeHoldings: Holdings, authorisations:Seq[RtrPrefix]) = {

    val roaCountsPerCountryAll =
      authorisations.par.groupBy(pfx => regionFor(pfx.prefix, ripeHoldings)).mapValues(_.seq).seq.mapValues(_.size)

    println("Economy, #Roas")

    // Holdings are limited to RIPE, while ROAs are from all region.
    // For some of the Roas we would not know who hold it (regionFor above would return "?").
    val roaCountsPerCountryInRIPE = roaCountsPerCountryAll -- Seq("?")

    roaCountsPerCountryInRIPE.toSeq.sortBy(-_._2).foreach{
      case (country, roaCount) => println(s"$country, $roaCount")
    }

  }

  def reportCountryAdoption(announcements: Seq[BgpAnnouncement], authorisations: Seq[RtrPrefix],
                          countryHolding: Holdings, quiet: Boolean, dateString: String) = {
    if (! quiet) {
      RegionCsv.printAdoptionHeader("Economy")
    }

    val countryStats = new RegionStats(countryHolding, announcements, authorisations)

    countryHolding.keys.par.foreach(cc => RegionCsv.reportRegionAdoption(cc, dateString, countryStats.regionAdoptionStats(cc)))
  }
}
