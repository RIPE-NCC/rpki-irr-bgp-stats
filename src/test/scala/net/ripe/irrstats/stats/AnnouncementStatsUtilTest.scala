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
package net.ripe.irrstats.stats

import java.math.BigInteger

import net.ripe.ipresource.{IpRange, IpResourceSet}
import org.scalatest.{Matchers, FunSuite}

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class AnnouncementStatsUtilTest extends FunSuite with Matchers {

  import scala.language.implicitConversions
  implicit def stringToIpRange(s: String) = IpRange.parse(s)

  test("Should count overlapping IP addresses in prefixes only once") {
    val prefixes: Seq[IpRange] = List("10.0.0.0/24", "10.0.0.0/23")
    AnnouncementStatsUtil.getNumberOfAddresses(prefixes) should equal (BigInteger.valueOf(512))
  }

  test("Should count IP addresses in different prefixes") {
    val prefixes: Seq[IpRange] = List("10.0.0.0/24", "10.1.0.0/23")
    AnnouncementStatsUtil.getNumberOfAddresses(prefixes) should equal (BigInteger.valueOf(256 + 512))
  }

}
