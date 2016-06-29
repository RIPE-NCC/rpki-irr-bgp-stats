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
package net.ripe.irrstats.map

import org.scalatest.{Matchers, FunSuite}

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class WorldMapPageTest extends FunSuite with Matchers {

  test("Should render page") {

    val adoptionValues = Map("NL" -> 0.85.toFloat, "BE" -> 0.45.toFloat)
    val validValues = Map("NL" -> 0.84.toFloat, "BE" -> 0.44.toFloat)
    val matchingValues = Map("NL" -> 0.99.toFloat, "BE" -> 0.99.toFloat)

    val page = WorldMapPage.printWorldMapHtmlPage(adoptionValues, validValues, matchingValues)

    page should include ("['NL', 0.85]")
    page should include ("['NL', 0.84]")
    page should include ("['NL', 0.99]")

    page should include ("['BE', 0.45]")
    page should include ("['BE', 0.44]")
    page should include ("['BE', 0.99]")
  }

}
