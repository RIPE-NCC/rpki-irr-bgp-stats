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

import akka.actor.ActorSystem
import net.ripe.irrstats.analysis.AsnStatsAnalyser
import net.ripe.irrstats.route.validation._

object ReportAsn {

  implicit val actorSystem: ActorSystem = akka.actor.ActorSystem()

  def report(announcements: Seq[BgpAnnouncement], authorisations: Seq[RtrPrefix], quiet: Boolean): Unit = {
    val validator = new BgpAnnouncementValidator()
    validator.startUpdate(announcements, authorisations)
    val validatedAnnouncements = validator.validatedAnnouncements

    if (!quiet) {
      println("Asn, PfxAnn, PfxValid, PfxInvalid, SpaceAnn, SpaceValid, SpaceInvalid")
    }

    AsnStatsAnalyser.statsPerAsn(validatedAnnouncements).toList.
      sortBy(_.spaceValid.negate()).
      take(100).
      foreach(stat =>
        println(s"${stat.asn}, ${stat.numberOfAnnouncements}, ${stat.numberValidAnnouncements}, ${stat.numberInvalidAnnouncements}, ${stat.spaceAnnounced}, ${stat.spaceValid}, ${stat.spaceInvalid}")
      )
  }

}
