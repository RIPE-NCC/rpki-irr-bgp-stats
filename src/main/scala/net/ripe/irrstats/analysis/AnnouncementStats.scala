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
package net.ripe.irrstats.analysis

import java.math.BigInteger

import net.ripe.irrstats.analysis.StatsUtil._
import net.ripe.irrstats.route.validation.{BgpValidatedAnnouncement, RouteValidity}


case class WorldMapCountryStat(countryCode: String,
                               prefixesAdoption: Option[Double], prefixesValid: Option[Double], prefixesMatching: Option[Double],
                               adoption: Option[Double], valid: Option[Double], matching: Option[Double], totalPrefixes: Integer, totalAddresses: BigInteger)

object WorldMapCountryStat {
  def fromCcAndStats(cc: String, stats: ValidatedAnnouncementStats): WorldMapCountryStat = {
    new WorldMapCountryStat(cc,
                            stats.percentageAdoptionCount, stats.percentageValid, stats.accuracyAnnouncements,
                            stats.percentageSpaceAdoption, stats.percentageSpaceValid, stats.accuracySpace,
                            stats.combined.count, stats.combined.numberOfIps)
  }
}

case class ValidatedAnnouncementStat(count: Integer, numberOfIps: BigInteger)
case class ValidatedAnnouncementStats(announcements: Seq[BgpValidatedAnnouncement],
                                      combined: ValidatedAnnouncementStat,
                                      covered: ValidatedAnnouncementStat,
                                      valid: ValidatedAnnouncementStat,
                                      invalidLength: ValidatedAnnouncementStat,
                                      invalidAsn: ValidatedAnnouncementStat,
                                      unknown: ValidatedAnnouncementStat,
                                      numberOfAuthorisations: Int
                                     ) {

  private def safePercentageIpSpace(fraction: ValidatedAnnouncementStat) = safePercentage(fraction.numberOfIps, combined.numberOfIps)
  private def safePercentageAnnouncements(fraction: ValidatedAnnouncementStat) = safePercentage(fraction.count, combined.count)

  def accuracyAnnouncements: Option[Double] = safePercentage(valid.count , valid.count + invalidAsn.count + invalidLength.count)
  def accuracySpace: Option[Double] = safePercentage(valid.numberOfIps, covered.numberOfIps)

  def percentageValid: Option[Double] = safePercentageAnnouncements(valid)
  def percentageInvalidLength: Option[Double] = safePercentageAnnouncements(invalidLength)
  def percentageInvalidAsn: Option[Double] = safePercentageAnnouncements(invalidAsn)

  def percentageUnknown: Option[Double] = safePercentageAnnouncements(unknown)
  def percentageAdoptionCount: Option[Double] =
    safePercentage(valid.count + invalidAsn.count + invalidLength.count, combined.count)
  def percentageAdoptionAddresses: Option[Double] =
    safePercentage(valid.numberOfIps.add(invalidAsn.numberOfIps).add(invalidLength.numberOfIps), combined.numberOfIps)

  def percentageSpaceValid: Option[Double] = safePercentageIpSpace(valid)
  def percentageSpaceInvalidLength: Option[Double] = safePercentageIpSpace(invalidLength)
  def percentageSpaceInvalidAsn: Option[Double] = safePercentageIpSpace(invalidAsn)
  def percentageSpaceUnknown: Option[Double] = safePercentageIpSpace(unknown)
  def percentageSpaceAdoption: Option[Double] = safePercentage(covered.numberOfIps, combined.numberOfIps)
}

object AnnouncementStats {


  def analyseValidatedAnnouncements(announcements: Seq[BgpValidatedAnnouncement], numberOfAuthorisations: Int): ValidatedAnnouncementStats = {

    val valid = announcements.filter(_.validity == RouteValidity.Valid)
    val invalidLength = announcements.filter(_.validity == RouteValidity.InvalidLength)
    val invalidAsn = announcements.filter(_.validity == RouteValidity.InvalidAsn)
    val unknown = announcements.filter(_.validity == RouteValidity.Unknown)

    ValidatedAnnouncementStats(
      announcements = announcements,
      combined = ValidatedAnnouncementStat(announcements.size, addressesCount(announcements.map(_.prefix))),
      covered = ValidatedAnnouncementStat(valid.size + invalidLength. size + invalidAsn.size,
                                          addressesCount(valid.map(_.prefix) ++ invalidLength.map(_.prefix) ++ invalidAsn.map(_.prefix))),
      valid = ValidatedAnnouncementStat(valid.size, addressesCount(valid.map(_.prefix))),
      invalidLength = ValidatedAnnouncementStat(invalidLength.size, addressesCount(invalidLength.map(_.prefix))),
      invalidAsn = ValidatedAnnouncementStat(invalidAsn.size, addressesCount(invalidAsn.map(_.prefix))),
      unknown = ValidatedAnnouncementStat(unknown.size, addressesCount(unknown.map(_.prefix))),
      numberOfAuthorisations = numberOfAuthorisations
    )
  }
}