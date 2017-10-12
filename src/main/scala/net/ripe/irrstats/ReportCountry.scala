/**
 * Copyright (c) 2015-2107 RIPE NCC
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

import net.ripe.irrstats.analysis.RegionStatsUtil
import net.ripe.irrstats.reporting.{CountryDetails, RegionCsv}
import net.ripe.rpki.validator.bgp.preview.BgpAnnouncement
import net.ripe.rpki.validator.models.RtrPrefix

object ReportCountry {

  def reportCountryDetails(announcements: Seq[BgpAnnouncement], authorisations: Seq[RtrPrefix], statsFile: File, countryCode: String) = {
    CountryDetails.printCountryAnnouncementReport(countryCode, new RegionStatsUtil(CountryDetailsMode, statsFile, announcements, authorisations).regionAnnouncementStats(countryCode))
  }

  def reportCountries(announcements: Seq[BgpAnnouncement], authorisations: Seq[RtrPrefix], statsFile: File, quiet: Boolean, dateString: String) = {
    if (! quiet) {
      RegionCsv.printHeader()
    }

    val countryStats = new RegionStatsUtil(CountryDetailsMode, statsFile, announcements, authorisations)

    for (cc <- countryStats.holdings.keys) {
      RegionCsv.reportRegionQuality(cc, countryStats.regionAnnouncementStats(cc), dateString)
    }

  }

}
