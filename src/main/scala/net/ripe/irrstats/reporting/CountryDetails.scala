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

import net.ripe.irrstats.analysis.ValidatedAnnouncementStats
import net.ripe.irrstats.route.validation.RouteValidity

object CountryDetails {

  def printCountryAnnouncementReport(cc: String, validatedAnnouncementStats: ValidatedAnnouncementStats) = {

    println(s"Detailed announcement report for country code: ${cc}")
    println("")
    println("Summary of announcements:")
    println(s"   Valid          : ${validatedAnnouncementStats.valid.count}" )
    println(s"   Invalid Length : ${validatedAnnouncementStats.invalidLength.count}" )
    println(s"   Invalid ASN    : ${validatedAnnouncementStats.invalidAsn.count}" )
    println(s"   Unknown        : ${validatedAnnouncementStats.unknown.count}" )
    println("")



    val invalidLengths = validatedAnnouncementStats.announcements.filter(_.validity == RouteValidity.InvalidLength)
    if (invalidLengths.size > 0 ) {
      println("Details on invalid length:")
      invalidLengths.foreach { ann =>
        println(s"    ${ann.prefix} ${ann.asn}")
      }
      println("")
    }

    val invalidAsns = validatedAnnouncementStats.announcements.filter(_.validity == RouteValidity.InvalidAsn)
    if (invalidAsns.size > 0 ) {
      println("Details on invalid ASN:")
      invalidAsns.foreach { ann =>
        println(s"    ${ann.prefix} ${ann.asn}")
      }
      println("")
    }




  }

}
