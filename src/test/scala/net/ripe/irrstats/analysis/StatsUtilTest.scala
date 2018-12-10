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

import net.ripe.ipresource.{IpRange, IpResourceSet}
import org.scalatest.{Matchers, FunSuite}

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class StatsUtilTest extends FunSuite with Matchers {

  import scala.language.implicitConversions

  implicit def stringToIpRange(s: String): IpRange = IpRange.parse(s)

  test("Should count overlapping IP addresses in prefixes only once") {
    val prefixes: Seq[IpRange] = List("10.0.0.0/24", "10.0.0.0/23")
    StatsUtil.addressesCount(prefixes) should equal(BigInteger.valueOf(512))
  }

  test("Should count IP addresses in different prefixes") {
    val prefixes: Seq[IpRange] = List("10.0.0.0/24", "10.1.0.0/23")
    StatsUtil.addressesCount(prefixes) should equal(BigInteger.valueOf(256 + 512))
  }

  test("Should count number of Ipv4 and its address size") {
    val prefixes: Seq[IpRange] = List("10.0.0.0/24", "10.0.0.0/23", "10.1.0.0/23")
    val set = new IpResourceSet()
    prefixes.foreach(set.add)
    StatsUtil.ipv4DisjointRangesCounts(set) should be(2)
    StatsUtil.ipv4AddressSize(set) should be(new BigInteger("1024"))
  }

  test("Should count number of Ipv6 and its address size") {
    val prefixes: Seq[IpRange] = List("2001:200::/32", "2001:200:e101::/48")
    val set = new IpResourceSet()

    prefixes.foreach(set.add)

    // Those two prefixes will be merged and counted as one big /32
    StatsUtil.ipv6DisjointRangesCounts(set) should be(1)
    val slash32 = new BigInteger("2").pow(128 - 32)
    StatsUtil.ipv6AddressSize(set) should be(slash32)
  }

  test("Should calculate safe percentage") {
    StatsUtil.safePercentage(BigInteger.ONE, BigInteger.TEN) should be(Some(0.1))
    StatsUtil.safePercentage(BigInteger.ONE, BigInteger.ZERO) should be(None)
    StatsUtil.safePercentage(2, 10) should be(Some(0.2))
    StatsUtil.safePercentage(2, 0) should be(None)
  }
}
