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
package net.ripe.irrstats.parsing.holdings

import java.io.File

import net.ripe.ipresource.{IpResource, IpResourceSet}
import Holdings._
import org.scalatest._
import matchers._

@org.junit.runner.RunWith(classOf[org.scalatestplus.junit.JUnitRunner])
class HoldingsParseTest extends funsuite.AnyFunSuite with should.Matchers {

  test("Should parse IPv4") {
    parseIpv4("213.154.64.0", "8192") should equal (IpResourceSet.parse("213.154.64.0/19"))
  }

  private val file = new File(Thread.currentThread().getContextClassLoader.getResource("extended-delegated-stats.txt").getFile)

  test("Should parse extended delegated stats") {

    val holdings = RIRHoldings.parse(Holdings.read(file))

    holdings("apnic").contains(IpResource.parse("1.0.0.0/24")) should be (true)

    regionFor(IpResource.parse("1.0.0.0/24"), holdings) should equal("apnic")
    regionFor(IpResource.parse("2.0.0.0/20"), holdings) should equal("ripencc")
  }

  test("Should parse country holding from extended delegated stats"){
    val holdings = CountryHoldings.parse(Holdings.read(file))

    val countries = holdings.keySet
    countries should be (Set("US", "AU", "GB", "FR", "EU", "IT", "JP", "CN"))

  }

  test("Should parse entity holding from extended delegated stats"){
    val (entityCountryHoldings: EntityRegionHoldings, _) = EntityHoldings.parse(Holdings.read(file))
    val countryHoldings = CountryHoldings.parse(Holdings.read(file))

    val entityCountryCombined = entityCountryHoldings.groupBy { case ((_, country), _) => country }
      .view
      .mapValues { entry =>
        val combined = new IpResourceSet()
        entry.values.foreach(combined.addAll)
        combined
      }

    // EntityRegion parser when grouped per country should be consistent with country holding parser.
    countryHoldings.keys.foreach{ country =>
      entityCountryCombined(country) should be(countryHoldings(country))
    }

  }
}
