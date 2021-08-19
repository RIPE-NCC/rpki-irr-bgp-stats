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
/**
  * Copyright (c) 2015-2107 RIPE NCC
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions are met:
  *   - Redistributions of source code must retain the above copyright notice,
  * this list of conditions and the following disclaimer.
  *   - Redistributions in binary form must reproduce the above copyright notice,
  * this list of conditions and the following disclaimer in the documentation
  * and/or other materials provided with the distribution.
  *   - Neither the name of this software, nor the names of its contributors, nor
  * the names of the contributors' employers may be used to endorse or promote
  * products derived from this software without specific prior written
  * permission.
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

import scala.jdk.CollectionConverters._

object StatsUtil {

  def safePercentage(fraction: BigInteger, total: BigInteger): Option[Double] = {
    if (total == BigInteger.ZERO) {
      None
    } else {
      Some(fraction.doubleValue / total.doubleValue)
    }
  }

  def safePercentage(fraction: Int, total: Int): Option[Double] =
    safePercentage(BigInteger.valueOf(fraction), BigInteger.valueOf(total))

  def addressesCount(ranges: Seq[IpRange]): BigInteger =
    new IpResourceSet().addAll(ranges).addressesSize()

  // This helper allows readable syntax when dealing with IPResourceSet (existing java library)
  // Here's for example what it allows you to do:
  //    ipResourceSet.addressCount()
  // instead of doing
  //    StatsUtil.addressCount(ipResourceSet)
  implicit class IpResourceSetHelper(resourceSet: IpResourceSet) {

    def addAll(ipranges: Seq[IpResource]): IpResourceSet = {
      ipranges.foreach(resourceSet.add)
      resourceSet
    }

    // Return iterator of underlying resources in resource Set
    def resources(): Iterator[IpResource] = resourceSet.iterator().asScala

    // Return ipv4 iterator of underlying resources in resource Set
    def ipv4Resources: Iterator[IpResource] =
      resourceSet.resources().filter(_.getType == IpResourceType.IPv4)

    // Return ipv6 iterator of underlying resources in resource Set
    def ipv6Resources: Iterator[IpResource] =
      resourceSet.resources().filter(_.getType == IpResourceType.IPv6)

    // Return number of Ipv4 prefixes that are not overlapping
    def ipv4ResourcesCounts(): Int = ipv4Resources.size

    // Return number of Ipv6 prefixes that are not overlapping
    def ipv6ResourcesCounts(): Int = ipv6Resources.size

    // Calculate size of unique IP address contained in this Set
    def addressesSize(): BigInteger = accumulateSize(resourceSet.resources())

    // Calculate size of unique IPv4 Addresses contained in this Set
    def ipv4AddressSize(): BigInteger = accumulateSize(ipv4Resources)

    // Calculate size of unique IPv6 Addresses contained in this Set
    def ipv6AddressSize(): BigInteger = accumulateSize(ipv6Resources)

    private def accumulateSize(resources: Iterator[IpResource]): BigInteger =
      resources.foldLeft(BigInteger.ZERO)((r, c) => {
        r.add(c.getEnd.getValue.subtract(c.getStart.getValue).add(BigInteger.ONE))
      })


    def hasCommonResourceWith(thatSet: IpResourceSet): Boolean = {

      def nextOrNull(iter : Iterator[IpResource]) = if (iter.hasNext) iter.next() else null

      // These iterators are sorter by IpResource range end.
      val thatIter: Iterator[IpResource] = thatSet.resources()

      val thisIter: Iterator[IpResource] = resources()

      var thatResource = nextOrNull(thatIter)

      var thisResource = nextOrNull(thisIter)

      // Same logic as in IpResourceSet.retainAll, we just terminate successfully if there is any intersection.
      while (thatResource != null && thisResource != null) {

        if (thatResource.intersect(thisResource) != null) return true

        val compareTo = thisResource.getEnd.compareTo(thatResource.getEnd)

        if (compareTo <= 0) thisResource = nextOrNull(thisIter)
        if (compareTo >= 0) thatResource = nextOrNull(thatIter)
      }

      false
    }

    }

}
