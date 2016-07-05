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
package net.ripe.irrstats.analysis

import java.math.BigInteger

import net.ripe.ipresource.{IpResourceSet, IpRange}
import net.ripe.rpki.validator.bgp.preview.BgpValidatedAnnouncement
import net.ripe.rpki.validator.models.RouteValidity
import scala.collection.JavaConverters._

case class WorldMapCountryStat(countryCode: String,
                               prefixesAdoption: Option[Double], prefixesValid: Option[Double], prefixesMatching: Option[Double],
                               adoption: Option[Double], valid: Option[Double], matching: Option[Double])

object WorldMapCountryStat {
  def fromCcAndStats(cc: String, stats: ValidatedAnnouncementStats) = {
    new WorldMapCountryStat(cc, stats.percentageAdoption, stats.percentageValid, stats.accuracyAnnouncements, stats.percentageSpaceAdoption, stats.percentageSpaceValid, stats.accuracySpace)
  }
}

case class ValidatedAnnouncementStat(count: Integer, numberOfIps: BigInteger)
case class ValidatedAnnouncementStats(announcements: Seq[BgpValidatedAnnouncement],
                                      combined: ValidatedAnnouncementStat,
                                      covered: ValidatedAnnouncementStat,
                                      valid: ValidatedAnnouncementStat,
                                      invalidLength: ValidatedAnnouncementStat,
                                      invalidLengthFiltered: ValidatedAnnouncementStat,
                                      invalidAsn: ValidatedAnnouncementStat,
                                      invalidAsnFiltered: ValidatedAnnouncementStat,
                                      unknown: ValidatedAnnouncementStat
                                     ) {

  private def safePercentageBig(fraction: BigInteger, total: BigInteger): Option[Double] = {
    if (total.equals(BigInteger.ZERO)) {
      None
    } else {
      Some(fraction.doubleValue / total.doubleValue)
    }
  }

  private def safePercentage(fraction: Int, total: Int): Option[Double] =
    safePercentageBig(BigInteger.valueOf(fraction), BigInteger.valueOf(total))

  private def safePercentageIpSpace(fraction: ValidatedAnnouncementStat) = safePercentageBig(fraction.numberOfIps, (combined.numberOfIps))
  private def safePercentageAnnouncements(fraction: ValidatedAnnouncementStat) = safePercentage(fraction.count, combined.count)

  def accuracyAnnouncements = safePercentage(valid.count , (valid.count + invalidAsn.count + invalidLength.count))
  def accuracyAnnouncementsFiltered = safePercentage(valid.count, (valid.count + invalidAsnFiltered.count + invalidLengthFiltered.count))
  def accuracySpace = safePercentageBig(valid.numberOfIps, covered.numberOfIps)

  // TODO: verify if this does not count IP addresses more than once (there may be overlap in the sets in particular invalid length and same address also for invalid asn)
  def accuracySpaceFiltered = safePercentageBig(valid.numberOfIps, (valid.numberOfIps.add(invalidAsnFiltered.numberOfIps).add(invalidLengthFiltered.numberOfIps)))

  def percentageValid = safePercentageAnnouncements(valid)
  def percentageInvalidLength = safePercentageAnnouncements(invalidLength)
  def percentageInvalidAsn = safePercentageAnnouncements(invalidAsn)
  def percentageInvalidLengthFiltered = safePercentageAnnouncements(invalidLengthFiltered)
  def percentageInvalidAsnFiltered = safePercentageAnnouncements(invalidAsnFiltered)
  def percentageUnknown = safePercentageAnnouncements(unknown)
  def percentageAdoption = safePercentage(valid.count + invalidAsn.count + invalidLength.count, combined.count)

  def percentageSpaceValid = safePercentageIpSpace(valid)
  def percentageSpaceInvalidLength = safePercentageIpSpace(invalidLength)
  def percentageSpaceInvalidAsn = safePercentageIpSpace(invalidAsn)
  def percentageSpaceInvalidLengthFiltered = safePercentageIpSpace(invalidLengthFiltered)
  def percentageSpaceInvalidAsnFiltered = safePercentageIpSpace(invalidAsnFiltered)
  def percentageSpaceUnknown = safePercentageIpSpace(unknown)
  def percentageSpaceAdoption = safePercentageBig(covered.numberOfIps, combined.numberOfIps)
}

object AnnouncementStatsUtil {

  def getNumberOfAddresses(prefixes: Seq[IpRange]) = {
    val resourceSet = new IpResourceSet()
    prefixes.foreach(pfx => resourceSet.addAll(new IpResourceSet(pfx)))
    resourceSet.iterator().asScala.foldLeft(BigInteger.ZERO)((r, c) => {
      r.add(c.getEnd.getValue.subtract(c.getStart.getValue).add(BigInteger.ONE))
    })
  }

  def analyseValidatedAnnouncements(announcements: Seq[BgpValidatedAnnouncement]) = {

    val valid = announcements.filter(_.validity == RouteValidity.Valid)
    val invalidLength = announcements.filter(_.validity == RouteValidity.InvalidLength)
    val invalidAsn = announcements.filter(_.validity == RouteValidity.InvalidAsn)
    val unknown = announcements.filter(_.validity == RouteValidity.Unknown)
    val filteredInvalidLength = invalidLength.filter(a => valid.find(_.prefix.overlaps(a.prefix)) == None) // count if no covering valid found
    val filteredInvalidAsn = invalidAsn.filter(a => valid.find(_.prefix.overlaps(a.prefix)) == None) // count if no covering valid found

    ValidatedAnnouncementStats(
      announcements = announcements,
      combined = ValidatedAnnouncementStat(announcements.size, getNumberOfAddresses(announcements.map(_.prefix))),
      covered = ValidatedAnnouncementStat(valid.size + invalidLength. size + invalidAsn.size, getNumberOfAddresses(valid.map(_.prefix) ++ invalidLength.map(_.prefix) ++ invalidAsn.map(_.prefix))),
      valid = ValidatedAnnouncementStat(valid.size, getNumberOfAddresses(valid.map(_.prefix))),
      invalidLength = ValidatedAnnouncementStat(invalidLength.size, getNumberOfAddresses(invalidLength.map(_.prefix))),
      invalidLengthFiltered = ValidatedAnnouncementStat(filteredInvalidLength.size, getNumberOfAddresses(filteredInvalidLength.map(_.prefix))),
      invalidAsn = ValidatedAnnouncementStat(invalidAsn.size, getNumberOfAddresses(invalidAsn.map(_.prefix))),
      invalidAsnFiltered = ValidatedAnnouncementStat(filteredInvalidAsn.size, getNumberOfAddresses(filteredInvalidAsn.map(_.prefix))),
      unknown = ValidatedAnnouncementStat(unknown.size, getNumberOfAddresses(unknown.map(_.prefix)))
    )
  }
}