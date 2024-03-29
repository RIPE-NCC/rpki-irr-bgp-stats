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
package net.ripe.irrstats.reporting

import org.scalatest._
import matchers._

@org.junit.runner.RunWith(classOf[org.scalatestplus.junit.JUnitRunner])
class WorldMapPageTest extends funsuite.AnyFunSuite with should.Matchers {

  test("Should render page") {

    val prefixesAdoptionValues = Map("NL" -> 0.861, "BE" -> 0.46).view
    val prefixesValidValues = Map("NL" -> 0.8512, "BE" -> 0.45).view
    val prefixesMatchingValues = Map("NL" -> 0.8400, "BE" -> 0.44).view
    val adoptionValues = Map("NL" -> 0.83, "BE" -> 0.43).view
    val validValues = Map("NL" -> 0.82, "BE" -> 0.42).view
    val matchingValues = Map("NL" -> 0.81, "BE" -> 0.41).view
    val stalenessValues = Map("NL" -> 0.80, "BE" -> 0.40).view

    val page = WorldMapPage.printWorldMapHtmlPage(prefixesAdoptionValues, prefixesValidValues, prefixesMatchingValues, adoptionValues, validValues, matchingValues, stalenessValues)

    page should include ("['NL', 86.10]")
    page should include ("['NL', 85.12]")
    page should include ("['NL', 84.00]")
    page should include ("['NL', 83.00]")
    page should include ("['NL', 82.00]")
    page should include ("['NL', 81.00]")
    page should include ("['NL', 80.00]")

    page should include ("['BE', 46.00]")
    page should include ("['BE', 45.00]")
    page should include ("['BE', 44.00]")
    page should include ("['BE', 43.00]")
    page should include ("['BE', 42.00]")
    page should include ("['BE', 41.00]")
    page should include ("['BE', 40.00]")

  }

}
