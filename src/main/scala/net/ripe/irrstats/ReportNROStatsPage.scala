/**
 * Copyright (c) 2015-2107 RIPE NCC
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
package net.ripe.irrstats

import net.ripe.ipresource.IpResourceSet
import net.ripe.irrstats.analysis.RegionStats
import net.ripe.irrstats.parsing.holdings.Holdings._
import net.ripe.irrstats.reporting.NROStatsPage
import net.ripe.irrstats.route.validation.{BgpAnnouncement, RtrPrefix}

import scala.collection.JavaConverters._

object ReportNROStatsPage {

  def report(announcements: Seq[BgpAnnouncement],
             authorisations: Seq[RtrPrefix],
             countryHolding: Holdings,
             allRirHoldings: Holdings,
             entityCountryHoldings: EntityRegionHoldings,
             entityRirHoldings: EntityRegionHoldings,
             certifiedResources : IpResourceSet
            ) = {

    val rirHoldings = allRirHoldings.filterNot(_._1.contains("Reserved"))

    val countryStats = new RegionStats(countryHolding, announcements, authorisations)
    val rirStats = new RegionStats(rirHoldings, announcements, authorisations)

    val countryAdoptions = countryHolding.keys.par.map(cc => (cc, countryStats.regionAdoptionStats(cc))).seq.toMap
    val rirAdoptions = rirHoldings.keys.par.map(cc => (cc, rirStats.regionAdoptionStats(cc))).seq.toMap

    val ipv4CountryAdoptionValues = countryAdoptions.mapValues(_.ipv4Adoption.getOrElse(0.0))
    val ipv6CountryAdoptionValues = countryAdoptions.mapValues(_.ipv6Adoption.getOrElse(0.0))

    val ipv4RIRAdoptionValues = rirAdoptions.mapValues(_.ipv4Adoption.getOrElse(0.0))
    val ipv6RIRAdoptionValues = rirAdoptions.mapValues(_.ipv6Adoption.getOrElse(0.0))

    val countryActivation  = regionActivation(entityCountryHoldings, certifiedResources)
    val rirActivation  = regionActivation(entityRirHoldings, certifiedResources)

    val ipv4CountryBubbleData = countryAdoptions.mapValues(c => (c.ipv4Adoption.getOrElse(0.0), countryActivation.getOrElse(c.region, 0), c.ipv4HoldingCount, c.ipv4HoldingSize))
    val ipv6CountryBubbleData = countryAdoptions.mapValues(c => (c.ipv6Adoption.getOrElse(0.0), countryActivation.getOrElse(c.region, 0), c.ipv6HoldingCount, c.ipv6HoldingSize))

    print(NROStatsPage.printNROStatsPage(
      ipv4CountryAdoptionValues,
      ipv6CountryAdoptionValues,
      ipv4CountryBubbleData,
      ipv6CountryBubbleData,
      ipv4RIRAdoptionValues,
      ipv6RIRAdoptionValues))
  }


  def reportActivation(entityRegionHoldings: EntityRegionHoldings, certifiedResources : IpResourceSet): Unit = {

    val activationData = regionActivation(entityRegionHoldings, certifiedResources).toSeq.sortBy(-_._2)
    println("Region, Active Entity Count")
    println(activationData.map { case (region, activeCounts) => s"$region,$activeCounts" }.mkString("\n"))
  }

  def regionActivation(entityRegionHoldings: EntityRegionHoldings, certifiedResources : IpResourceSet): Map[String, Int] = {

    val certifiedEntities: Set[EntityRegion] = certifiedEntityRegion(entityRegionHoldings, certifiedResources)
    val regionActivation: Map[String, Int] = certifiedEntities.groupBy{ case (_, region) => region}.mapValues(_.size)

    regionActivation
  }

  // Compute set of (Entity, Region) pairs that has at least one certified resource.
  def certifiedEntityRegion(entityRegionHoldings: EntityRegionHoldings,
                            certifiedResources : IpResourceSet) : Set[EntityRegion] = {

    def hasCertifiedResource(entityResources: IpResourceSet) : Boolean =
      entityResources.iterator().asScala.exists(certifiedResources.contains)

    entityRegionHoldings.mapValues(hasCertifiedResource).filter{
      case (_, isCertified) => isCertified
    }.keys.toSet
  }

}
