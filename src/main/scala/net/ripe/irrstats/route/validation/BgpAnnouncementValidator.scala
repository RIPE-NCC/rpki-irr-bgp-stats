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

import grizzled.slf4j.Logging
import net.ripe.ipresource.{Asn, IpRange}
import NumberResources.{NumberResourceInterval, NumberResourceIntervalTree}
import org.joda.time.DateTime
import RouteValidity._
import net.ripe.irrstats.Time

import scala.collection.parallel.CollectionConverters._
import scala.concurrent.stm.Ref

case class BgpAnnouncementSet(url: String, lastModified: Option[DateTime] = None, entries: Seq[BgpAnnouncement] = Seq.empty)

case class BgpAnnouncement(asn: Asn, prefix: IpRange) {
  def interval = NumberResourceInterval(prefix.getStart, prefix.getEnd)
}

object BgpValidatedAnnouncement {
  def make(announced: BgpAnnouncement, valids: Seq[RtrPrefix] = Seq.empty,
           invalidsAsn: Seq[RtrPrefix] = Seq.empty,
           invalidsLength: Seq[RtrPrefix] = Seq.empty) = BgpValidatedAnnouncement(announced,
    valids.map((Valid, _)) ++ invalidsAsn.map((InvalidAsn, _)) ++ invalidsLength.map((InvalidLength, _)))
}

case class StalenessStat(authorisations: Seq[RtrPrefix], stale: Seq[RtrPrefix]) {
  def fraction: Double = {
    if (authorisations.nonEmpty) {
      stale.size.toDouble / authorisations.size.toDouble
    } else {
      1.0
    }
  }
}

case class BgpValidatedAnnouncement(announced: BgpAnnouncement, prefixes: Seq[(RouteValidity, RtrPrefix)] = List.empty) {
  require(!invalidsAsn.exists(_.asn == announced.asn), "invalidsAsn must not contain the announced ASN")
  require(!invalidsLength.exists(_.asn != announced.asn), "invalidsLength must only contain VRPs that refer to the same ASN")

  def asn = announced.asn

  def prefix = announced.prefix

  def validity = {
    if (valids.nonEmpty) RouteValidity.Valid
    else if (invalidsLength.nonEmpty) RouteValidity.InvalidLength
    else if (invalidsAsn.nonEmpty) RouteValidity.InvalidAsn
    else RouteValidity.Unknown
  }

  def valids = prefixes.collect { case (Valid, p) => p }

  def invalidsAsn = prefixes.collect { case (InvalidAsn, p) => p }

  def invalidsLength = prefixes.collect { case (InvalidLength, p) => p }
}

object BgpAnnouncementValidator {
  val VISIBILITY_THRESHOLD = 5

  def validate(announcement: BgpAnnouncement, prefixes: Seq[RtrPrefix]): BgpValidatedAnnouncement =
    validate(announcement, NumberResourceIntervalTree(prefixes: _*))

  def validate(announcement: BgpAnnouncement, prefixTree: NumberResourceIntervalTree[RtrPrefix]): BgpValidatedAnnouncement = {
    val matchingPrefixes = prefixTree.findExactAndAllLessSpecific(announcement.interval)
    val groupedByValidity = matchingPrefixes.map { prefix =>
      val validity = if (hasInvalidAsn(prefix, announcement))
        InvalidAsn
      else if (hasInvalidPrefixLength(prefix, announcement)) InvalidLength else Valid
      (validity, prefix)
    }
    BgpValidatedAnnouncement(announcement, groupedByValidity)
  }

  private def hasInvalidAsn(prefix: RtrPrefix, announced: BgpAnnouncement) =
    prefix.asn != announced.asn

  private def hasInvalidPrefixLength(prefix: RtrPrefix, announced: BgpAnnouncement) =
    prefix.maxPrefixLength.getOrElse(prefix.prefix.getPrefixLength) < announced.prefix.getPrefixLength
}

class BgpAnnouncementValidator() extends Logging {

  private val _validatedAnnouncements = Ref(IndexedSeq.empty[BgpValidatedAnnouncement])

  def validatedAnnouncements: IndexedSeq[BgpValidatedAnnouncement] = _validatedAnnouncements.single.get

  def startUpdate(announcements: Seq[BgpAnnouncement], prefixes: Seq[RtrPrefix]): Unit = {
    val v = validate(announcements, prefixes)
    _validatedAnnouncements.single.set(v)
  }

  def staleness(announcements: Seq[BgpAnnouncement], authorisations: Seq[RtrPrefix]): StalenessStat = {

    // TODO: Use a tree to limit the searching below to relevant announcements only
    val staleAuthorisations = authorisations.flatMap { auth =>
      if(announcements.exists(
          ann => ann.asn.equals(auth.asn) &&
          auth.prefix.contains(ann.prefix) &&
          auth.effectiveMaxPrefixLength >= ann.prefix.getPrefixLength)) {
        None // Return only stale announcements
      } else {
        Some(auth)
      }
    }

    StalenessStat(authorisations, staleAuthorisations)
  }

  private def validate(announcements: Seq[BgpAnnouncement], prefixes: Seq[RtrPrefix]): IndexedSeq[BgpValidatedAnnouncement] = {
    val (prefixTree, _) = Time.timed(NumberResourceIntervalTree(prefixes: _*))
    announcements.par.map(BgpAnnouncementValidator.validate(_, prefixTree)).seq.toIndexedSeq
  }
}
