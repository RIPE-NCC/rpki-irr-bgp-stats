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
package net.ripe.irrstats.parsing.route

import java.io.File

import org.scalatest._
import matchers._

@org.junit.runner.RunWith(classOf[org.scalatestplus.junit.JUnitRunner])
class RouteParserTest extends funsuite.AnyFunSuite with should.Matchers {

  test("regex should split") {
    val routeLine = "mnt-by:          MNT-SOMETHING # bla"
    val otherLine = "# bla"
    val keyValueRegex = """^([\w\-]+):\s*(\S*).*$""".r

    routeLine match {
      case keyValueRegex(key, value) => {
        key should equal("mnt-by")
        value should equal ("MNT-SOMETHING")
      }
      case _ => fail("Should have matched")
    }

    otherLine match {
      case keyValueRegex(k, v) => fail("Should not match")
      case _ => // okay
    }

  }

  test("should parse routes file") {
    val routesFile = new File(Thread.currentThread().getContextClassLoader().getResource("ripe-db-route.txt").getFile)
    val routes = RouteParser.parse(routesFile)
    routes.size should equal(62)
  }

  test("should parse routes6 file") {
    val route6sFile = new File(Thread.currentThread().getContextClassLoader().getResource("ripe-db-route6.txt").getFile)
    val route6s = RouteParser.parse(route6sFile)
          route6s.size should equal(62)
  }
}
