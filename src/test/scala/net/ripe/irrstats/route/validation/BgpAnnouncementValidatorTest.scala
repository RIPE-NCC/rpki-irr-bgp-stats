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
package net.ripe.irrstats.route.validation

import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import RouteValidity._
import net.ripe.ipresource.{Asn, IpRange}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner


@RunWith(classOf[JUnitRunner])
class BgpAnnouncementValidatorTest extends FunSuite with Matchers with BeforeAndAfterAll {

  import scala.language.implicitConversions
  implicit def IntToAsn(asn: Int): Asn = new Asn(asn)
  implicit def StringToAsn(asn: String): Asn = Asn.parse(asn)
  implicit def StringToIpRange(prefix: String): IpRange = IpRange.parse(prefix)
  implicit def TupleToBgpAnnouncement(x: (Int, String)): BgpAnnouncement = BgpAnnouncement(x._1, x._2)
  implicit def TupleToRtrPrefix(x: (Int, String)): RtrPrefix = RtrPrefix(x._1, x._2)
  implicit def TupleToRtrPrefix(x: (Int, String, Int)): RtrPrefix = RtrPrefix(x._1, x._2, Some(x._3))

  implicit val actorSystem = akka.actor.ActorSystem()
  private val subject = new BgpAnnouncementValidator

  test("should validate prefixes") {
    val announcements = Seq[BgpAnnouncement]((65001, "10.0.1.0/24"), (65002, "10.0.2.0/24"), (65003, "10.0.3.0/24"), (65004, "10.0.4.0/24"))
    val prefixes = Seq[RtrPrefix]((65001, "10.0.1.0/24"), (65001, "10.0.2.0/24"), (65003, "10.0.3.0/24", 20))

    subject.startUpdate(announcements, prefixes)

    subject.validatedAnnouncements should have size 4
    subject.validatedAnnouncements should be(Seq(
      BgpValidatedAnnouncement.make((65001, "10.0.1.0/24"), valids = Seq((65001, "10.0.1.0/24"))),
      BgpValidatedAnnouncement.make((65002, "10.0.2.0/24"), invalidsAsn = Seq((65001, "10.0.2.0/24"))),
      BgpValidatedAnnouncement.make((65003, "10.0.3.0/24"), invalidsLength = Seq((65003, "10.0.3.0/24", 20))),
      BgpValidatedAnnouncement.make((65004, "10.0.4.0/24"))))
  }

  test("validity should be Unknown if there are no RTR prefixes") {
    val announcements = Seq[BgpAnnouncement]((65001, "10.0.1.0/24"))
    val prefixes = Seq.empty

    subject.startUpdate(announcements, prefixes)

    subject.validatedAnnouncements.map(x=> (x.asn, x.prefix, x.validity)) should be(Seq((65001: Asn, "10.0.1.0/24": IpRange, Unknown)))
  }

  test("validity should be Valid if there is an appropriate RTR prefix") {
    val announcements = Seq[BgpAnnouncement]((65001, "10.0.1.0/24"))
    val prefixes = Seq[RtrPrefix]((65001, "10.0.1.0/24"))

    subject.startUpdate(announcements, prefixes)

    subject.validatedAnnouncements.map(x=> (x.asn, x.prefix, x.validity)) should be(Seq((65001: Asn, "10.0.1.0/24": IpRange, Valid)))
  }

  test("validity should be Invalid ASN if there is an appropriate RTR prefix") {
    val announcements = Seq[BgpAnnouncement]((65001, "10.0.1.0/24"))
    val prefixes = Seq[RtrPrefix]((65002, "10.0.1.0/24"))

    subject.startUpdate(announcements, prefixes)

    subject.validatedAnnouncements.map(x=> (x.asn, x.prefix, x.validity)) should be(Seq((65001: Asn, "10.0.1.0/24": IpRange, InvalidAsn)))
  }

  test("validity should be Invalid Length if there is an appropriate RTR prefix") {
    val announcements = Seq[BgpAnnouncement]((65001, "10.0.1.0/24"))
    val prefixes = Seq[RtrPrefix]((65001, "10.0.1.0/24", 20))

    subject.startUpdate(announcements, prefixes)

    subject.validatedAnnouncements.map(x=> (x.asn, x.prefix, x.validity)) should be(Seq((65001: Asn, "10.0.1.0/24": IpRange, InvalidLength)))
  }

  test("should fail to construct BgpValidatedAnnouncement if invalidsLength contains a VRP that refers to a different ASN") {
    val announcement = (65001, "10.0.1.0/24"): BgpAnnouncement
    val invalidsAsn = Seq[RtrPrefix]((65002, "10.0.1.0/24"))

    val e = the [IllegalArgumentException] thrownBy { BgpValidatedAnnouncement.make(announcement, invalidsLength = invalidsAsn) }
    e.getMessage should equal ("requirement failed: invalidsLength must only contain VRPs that refer to the same ASN")
  }

  test("should fail to construct BgpValidatedAnnouncement if invalidsAsn contains a VRP with the announced ASN") {
    val announcement = (65001, "10.0.1.0/24"): BgpAnnouncement
    val invalidsLength = Seq[RtrPrefix]((65001, "10.0.0.0/16", 20))

    val e = the [IllegalArgumentException] thrownBy { BgpValidatedAnnouncement.make(announcement, invalidsAsn = invalidsLength) }
    e.getMessage should equal ("requirement failed: invalidsAsn must not contain the announced ASN")
  }

  test("validity should be determined by RTR prefixes") {
    val announcement = (65001, "10.0.1.0/24"): BgpAnnouncement
    val valids = Seq[RtrPrefix]((65001, "10.0.1.0/24"))
    val invalidsAsn = Seq[RtrPrefix]((65002, "10.0.1.0/24"))
    val invalidsLength = Seq[RtrPrefix]((65001, "10.0.0.0/16", 20))

    BgpValidatedAnnouncement(announcement).validity should be(Unknown)

    BgpValidatedAnnouncement.make(announcement, invalidsLength = invalidsLength).validity should be(InvalidLength)
    BgpValidatedAnnouncement.make(announcement, invalidsAsn = invalidsAsn, invalidsLength = invalidsLength).validity should be(InvalidLength)

    BgpValidatedAnnouncement.make(announcement, invalidsAsn = invalidsAsn).validity should be(InvalidAsn)

    BgpValidatedAnnouncement.make(announcement, valids).validity should be(Valid)
    BgpValidatedAnnouncement.make(announcement, valids, invalidsAsn).validity should be(Valid)
    BgpValidatedAnnouncement.make(announcement, valids, invalidsLength = invalidsLength).validity should be(Valid)
    BgpValidatedAnnouncement.make(announcement, valids, invalidsAsn, invalidsLength).validity should be(Valid)
  }

  test("Should find stale authorisations") {

    val announcement = (65001, "10.0.0.0/24"): BgpAnnouncement
    val irrelevantAnnouncement = (65001, "192.168.0.0/16"): BgpAnnouncement

    val coveringRoaAllowingByMaxLength = RtrPrefix(65001, "10.0.0.0/20", Some(24))
    val coveringRoaNotAllowing = RtrPrefix(65001, "10.0.0.0/20")
    val exactRoa = RtrPrefix(65001, "10.0.0.0/24")
    val otherAsnRoa = RtrPrefix(65002, "10.0.0.0/24")
    val otherPrefixRoa = RtrPrefix(65001, "10.1.0.0/24")

    val validator = new BgpAnnouncementValidator()

    val staleAuthorisations = validator.staleness(Seq(announcement, irrelevantAnnouncement), Seq(coveringRoaAllowingByMaxLength, coveringRoaNotAllowing, exactRoa, otherAsnRoa, otherPrefixRoa)).stale

    staleAuthorisations should not contain (coveringRoaAllowingByMaxLength)
    staleAuthorisations should not contain (exactRoa)
    staleAuthorisations should contain (coveringRoaNotAllowing)
    staleAuthorisations should contain (otherAsnRoa)
    staleAuthorisations should contain (otherPrefixRoa)

  }

}
