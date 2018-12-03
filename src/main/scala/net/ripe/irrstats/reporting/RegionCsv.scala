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
package net.ripe.irrstats.reporting

import net.ripe.irrstats.analysis.{RegionAdoptionStats, ValidatedAnnouncementStats}

object RegionCsv {

  def printHeader(): Unit = println("date, RIR, authorisations, announcements, accuracy announcements, " +
    "fraction valid, fraction invalid length, fraction invalid asn, fraction unknown, " +
    "space announced, accuracy space, " +
    "fraction space valid, fraction space invalid length, fraction space invalid asn, " +
    "fraction space unknown, adoption IPs, IPv4 adoption, IPv6 adoption")

  def reportRegionQuality(region: String, stats: ValidatedAnnouncementStats, dateString: String, adoption: RegionAdoptionStats): Unit = {

    def string(fo: Option[Double]) = fo.map(f => f"$f%1.4f").getOrElse("")

    println(s"$dateString, $region, ${stats.numberOfAuthorisations}, ${stats.combined.count}, ${string(stats.accuracyAnnouncements)}, " +
      s"${string(stats.percentageValid)}, ${string(stats.percentageInvalidLength)}, ${string(stats.percentageInvalidAsn)}, " +
      s"${string(stats.percentageUnknown)}, ${stats.combined.numberOfIps}, ${string(stats.accuracySpace)}, " +
      s"${string(stats.percentageSpaceValid)}, ${string(stats.percentageSpaceInvalidLength)}, " +
      s"${string(stats.percentageSpaceInvalidAsn)}, ${string(stats.percentageSpaceUnknown)}, ${string(stats
        .percentageAdoptionAddresses)}, ${string(adoption.ipv4Adoption)},  ${string(adoption.ipv6Adoption)}")
  }

}
