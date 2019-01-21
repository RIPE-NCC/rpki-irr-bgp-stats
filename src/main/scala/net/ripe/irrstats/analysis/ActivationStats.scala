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
import net.ripe.irrstats.Time
import net.ripe.irrstats.analysis.StatsUtil._
import net.ripe.irrstats.parsing.holdings.Holdings.{CertificateResources, EntityRegion, EntityRegionHoldings}

import scala.collection.mutable

object ActivationStats {

  def regionActivation(entityRegionHoldings: EntityRegionHoldings, certifiedResourcesMap: CertificateResources): Map[String, Int] = {
    val certSubjectAndEntityRegionListMap  = mutable.Map[String, List[EntityRegion]]().withDefaultValue(List())
    var counter = 0
    val total = entityRegionHoldings.size

    def checkAndUpdateSubject( entityRegion: EntityRegion, resources: IpResourceSet): Unit = {
         certifiedResourcesMap.par.foreach { case (subject, certifiedResources) =>
           if(certifiedResources.hasCommonResourceWith(resources))
             certSubjectAndEntityRegionListMap(subject) ::= entityRegion
         }
    }

    val (activationResult, time) = Time.timed {
      entityRegionHoldings.par.foreach {
        case (entityRegion, resources) => {
          counter += 1
          if (counter % 100 == 0) System.err.println(s"Entity processed : $counter out of $total")
          checkAndUpdateSubject(entityRegion, resources)
        }
      }

      val entityRegionsWithCertifiedWithSingleCertSubject = certSubjectAndEntityRegionListMap.filter(_._2.size == 1).values.map(_.head)
      val regionActivation = entityRegionsWithCertifiedWithSingleCertSubject.groupBy(_._2).mapValues(_.size)

      regionActivation
    }

    System.err.println(s"Elapsed time for activation calculation $time")
    activationResult

  }
}
