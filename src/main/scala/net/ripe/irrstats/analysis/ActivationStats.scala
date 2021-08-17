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

import grizzled.slf4j.Logging
import net.ripe.ipresource.IpResourceSet
import net.ripe.irrstats.Time
import net.ripe.irrstats.analysis.StatsUtil._
import net.ripe.irrstats.parsing.holdings.Holdings.{CertificateResources, EntityRegion, EntityRegionHoldings}

import scala.collection.parallel.immutable.ParIterable

object ActivationStats extends Logging {

  case class EntityRegionSubject(entity:String, region:String, subject:String)

  def regionActivation(entityRegionHoldings: EntityRegionHoldings, certifiedResourcesMap: CertificateResources): collection.Map[String, Int] = {

    def collectCoveringCertifiedSubjects( entityRegion: EntityRegion, resources: IpResourceSet): ParIterable[EntityRegionSubject] = {
         certifiedResourcesMap.par.flatMap { case (subject, certifiedResources) =>
           if(certifiedResources.hasCommonResourceWith(resources)) {
             List(EntityRegionSubject(entityRegion._1, entityRegion._2, subject))
           } else List()
         }
    }

    var counter = 0
    val (activationResult, time) = Time.timed {
      val entityRegionSubjects = entityRegionHoldings.toSeq.par.flatMap {
        case (entityRegion, resources) =>
          counter += 1
          if (counter % 100 == 0) logger.debug(s"Entity processed : $counter out of ${entityRegionHoldings.size}")
          collectCoveringCertifiedSubjects(entityRegion, resources)
      }

      entityRegionSubjects.groupBy(_.subject)
        .filter(_._2.size == 1)   // Remove subjects used multiple times, or not used at all.
        .values                   // This will be list of EntityRegionSubject with exactly one member
        .map(_.head)              // Therefore we care only about its head.
        .groupBy(_.region)        // Now we wanted to count per region, how many are those.
        .mapValues(_.size)        // So we count the size.
    }

    logger.info(s"Elapsed time for activation calculation $time")
    activationResult.seq          // All data structure was par data structure so we go back to sequential.

  }
}
