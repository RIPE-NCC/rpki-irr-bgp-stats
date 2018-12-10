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
package net.ripe.irrstats.analysis

import net.ripe.ipresource.IpResourceSet
import net.ripe.irrstats.parsing.holdings.Holdings.{EntityRegion, EntityRegionHoldings}
import scala.collection.JavaConverters._

object ActivationStats {

  // Compute set of (Entity, Region) pairs that has at least one certified resource.
  def certifiedEntityRegion(entityRegionHoldings: EntityRegionHoldings,
                            certifiedResources : IpResourceSet) : Set[EntityRegion] = {

    def hasCertifiedResource(entityResources: IpResourceSet) : Boolean =
      entityResources.iterator().asScala.exists(certifiedResources.contains)

    entityRegionHoldings.mapValues(hasCertifiedResource).filter{
      case (_, isCertified) => isCertified
    }.keys.toSet
  }

  def regionActivation(entityRegionHoldings: EntityRegionHoldings, certifiedResources : IpResourceSet): Map[String, Int] = {

    val certifiedEntities: Set[EntityRegion] = certifiedEntityRegion(entityRegionHoldings, certifiedResources)
    val regionActivation: Map[String, Int] = certifiedEntities.groupBy{ case (_, region) => region}.mapValues(_.size)

    regionActivation
  }
}
