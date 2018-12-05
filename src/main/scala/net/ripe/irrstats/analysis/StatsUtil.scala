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

import java.math.BigInteger

import net.ripe.ipresource.{IpRange, IpResource, IpResourceSet, IpResourceType}

import scala.collection.JavaConverters._

object StatsUtil {

  def addressesCount(prefixes: Seq[IpRange]): BigInteger = {
    val resourceSet = new IpResourceSet()
    prefixes.foreach(pfx => resourceSet.addAll(new IpResourceSet(pfx)))
    addressesCount(resourceSet)
  }

  def addressesCount(resourceSet: IpResourceSet): BigInteger =
    addressesCount(resourceSet.iterator().asScala)

  private def addressesCount(ipResources: Iterator[IpResource]): BigInteger = {
    ipResources.foldLeft(BigInteger.ZERO)((r, c) => {
      r.add(c.getEnd.getValue.subtract(c.getStart.getValue).add(BigInteger.ONE))
    })
  }

  def addressCountPerType(resourceSet: IpResourceSet, resourceType: IpResourceType): BigInteger = {
    addressesCount(resourceSet.iterator().asScala.filter(_.getType == resourceType))
  }

  def ipv4Count(resourceSet: IpResourceSet) = resourceSet.iterator().asScala.filter(_.getType == IpResourceType.IPv4).size
  def ipv6Count(resourceSet: IpResourceSet) = resourceSet.iterator().asScala.filter(_.getType == IpResourceType.IPv6).size

  def ipv4Size(resourceSet : IpResourceSet) : BigInteger = addressCountPerType(resourceSet, IpResourceType.IPv4)
  def ipv6Size(resourceSet : IpResourceSet) : BigInteger = addressCountPerType(resourceSet, IpResourceType.IPv6)

  def safePercentage(fraction: BigInteger, total: BigInteger): Option[Double] = {
    if (total == BigInteger.ZERO) {
      None
    } else {
      Some(fraction.doubleValue / total.doubleValue)
    }
  }

  def safePercentage(fraction: Int, total: Int): Option[Double] =
    safePercentage(BigInteger.valueOf(fraction), BigInteger.valueOf(total))

}