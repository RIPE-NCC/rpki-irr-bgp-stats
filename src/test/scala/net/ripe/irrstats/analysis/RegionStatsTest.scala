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
/**
  * Copyright (c) 2015-2107 RIPE NCC
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
package net.ripe.irrstats.analysis

import java.math.BigInteger

import net.ripe.ipresource.{Asn, IpRange, IpResourceSet}
import net.ripe.irrstats.parsing.holdings.Holdings.Holdings
import net.ripe.irrstats.route.validation.{BgpAnnouncement, RtrPrefix}
import org.scalatest.{FunSuite, Matchers}

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class RegionStatsTest extends FunSuite with Matchers {

  import scala.language.implicitConversions
  implicit def stringToIpRange(s: String): IpRange = IpRange.parse(s)

  // Sizes
  val ipv4slash23 = new BigInteger("512")
  val ipv6slash32 = new BigInteger("2").pow(128 - 32)


  val allPrefixes: Seq[IpRange] = List("10.0.0.0/24", "10.0.0.0/23", "2001:200::/32", "2001:200:e101::/48")
  val authorized: Seq[IpRange] = List("10.0.0.0/24",  "2001:200::/32")
  val announced: Seq[IpRange] = List("10.0.0.0/24", "10.0.0.0/23", "2001:200::/32", "2001:200:e101::/48")

  val holdingSet: IpResourceSet = allPrefixes.foldLeft(new IpResourceSet()){
    case (res, next) => res.add(next); res
  }

  val authorisations: Seq[RtrPrefix] = authorized.zipWithIndex map {
    case (prefix, idx) => RtrPrefix(Asn.parse(s"AS$idx"), prefix)
  }

  val holdings: Holdings = Map("ripencc"-> holdingSet)
  val announcements: Seq[BgpAnnouncement] = announced.zipWithIndex map {
    case (prefix,idx) =>
      BgpAnnouncement(Asn.parse(s"AS$idx"), prefix)
  }

  test("Should calculate adoption ") {
    val regionStat = new RegionStats(holdings, announcements, authorisations)

    val ripeStat = regionStat.regionAdoptionStats("ripencc")

    ripeStat.ipv4HoldingSize should be(ipv4slash23)
    ripeStat.ipv6HoldingSize should be(ipv6slash32)

    ripeStat.ipv4Adoption should be(Some(0.5))
    ripeStat.ipv6Adoption should be(Some(1.0))
  }

  test("Should calculate announcement stat") {
    val regionStat = new RegionStats(holdings, announcements, authorisations)

    val announcementStats: ValidatedAnnouncementStats = regionStat.regionAnnouncementStats("ripencc")
    announcementStats.combined.count should be(4)
    announcementStats.valid.count should be(1)
    announcementStats.invalidAsn.count should be(2)
  }

  test("Should calculate world  stats ") {
    val regionStat = new RegionStats(holdings, announcements, authorisations)
    val ripeStat = regionStat.worldMapStats.head

    // Percentage of announcement that is not unknown
    ripeStat.prefixesAdoption should be(Some(0.75))

    // Precentage of valid among knowns (valid, invalid length, invalid asn)
    ripeStat.prefixesMatching should be(Some(1.0/3.0))

  }

}
