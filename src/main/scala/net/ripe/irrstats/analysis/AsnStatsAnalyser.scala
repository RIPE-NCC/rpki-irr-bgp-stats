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
package net.ripe.irrstats.analysis

import java.math.BigInteger

import net.ripe.ipresource.Asn
import net.ripe.irrstats.route.validation._

import scala.collection.immutable.Iterable

object AsnStatsAnalyser {
  private def announcementsPerAsn(announcements: Seq[BgpValidatedAnnouncement]): Map[Asn, Seq[BgpValidatedAnnouncement]] = announcements.groupBy { ann => ann.asn }

  def statsPerAsn(announcements: Seq[BgpValidatedAnnouncement]): Iterable[AsnStat] = {
    announcementsPerAsn(announcements).map {
      case (asn, an) => AsnStat.fromAnnouncements(asn, an)
    }
  }

}

case class AsnStat(asn: Asn,
                   numberOfAnnouncements: Int, numberValidAnnouncements: Int, numberInvalidAnnouncements: Int,
                   spaceAnnounced: BigInteger, spaceValid: BigInteger, spaceInvalid: BigInteger)

object AsnStat {
  def fromAnnouncements(asn: Asn, announcements: Seq[BgpValidatedAnnouncement]): AsnStat = {

    val valids = announcements.filter { a => a.validity == RouteValidity.Valid }
    val invalids = announcements.filter { a => a.validity == RouteValidity.InvalidAsn || a.validity == RouteValidity.InvalidLength }

    def spaceFor(announcements: Seq[BgpValidatedAnnouncement]): BigInteger =
      AnnouncementStats.getNumberOfAddresses(announcements.map(_.prefix))

    AsnStat(
      asn,
      announcements.size,
      valids.size,
      invalids.size,
      spaceFor(announcements),
      spaceFor(valids),
      spaceFor(invalids)
    )
  }
}
