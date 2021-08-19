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
package net.ripe.irrstats.parsing.announcements

import java.io.File

import net.ripe.irrstats.parsing.ris.RisDumpUtil
import net.ripe.irrstats.route.validation.BgpAnnouncement
import org.scalatest._
import matchers._

@org.junit.runner.RunWith(classOf[org.scalatestplus.junit.JUnitRunner])
class RisDumpUtilTest extends funsuite.AnyFunSuite with should.Matchers {

  test("Should parse ipv4 dump file") {
    val dumpFile = new File(Thread.currentThread().getContextClassLoader().getResource("ris-ipv4.txt").getFile)
    val announcements: Seq[BgpAnnouncement] = RisDumpUtil.parseDumpFile(dumpFile)

    // File contains 35 entries, 6 entries with peers < 5
    announcements.length should be(29)
  }

  test("Should parse ipv6 dump file") {
    val dumpFile = new File(Thread.currentThread().getContextClassLoader().getResource("ris-ipv6.txt").getFile)
    val announcements: Seq[BgpAnnouncement] = RisDumpUtil.parseDumpFile(dumpFile)

    // File contains 35 entries, 13 entries with peers < 5
    announcements.length should be(22)
  }

}
