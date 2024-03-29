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
package net.ripe.irrstats.parsing.holdings

import java.io.File

import net.ripe.ipresource.{IpRange, IpResource, IpResourceSet, Ipv4Address}
import net.ripe.irrstats.parsing.holdings.Holdings._

import scala.collection.mutable
import scala.io.Source

object Holdings {


  // Map of region to a set of resources
  type Holdings = Map[String, IpResourceSet]

  // A tuple that can be (OpaqueId, RIR), or (OpaqueId, Country)
  type EntityRegion = (String, String)

  // Resources belonging to an entity within certain region.
  type EntityRegionHoldings = Map[EntityRegion, IpResourceSet]

  type Subject = String

  type CertificateResources = Map[Subject, IpResourceSet]

  def read(statsFile: File): Seq[String] = Source.fromFile(statsFile, "ASCII").getLines().toSeq

  def parseIpv4(base: String, addresses: String): IpResourceSet = {
    val baseAddress = Ipv4Address.parse(base)
    val endAddress = new Ipv4Address(baseAddress.longValue() + addresses.toLong - 1)

    new IpResourceSet(IpRange.range(baseAddress, endAddress))
  }

  def parseResourceLine(tokens: Array[String]) = tokens(2) match {
    case "asn" => IpResourceSet.parse("AS" + tokens(3))
    case "ipv4" => parseIpv4(tokens(3), tokens(4))
    case "ipv6" => IpResourceSet.parse(s"${tokens(3)}/${tokens(4)}")
    case _ => new IpResourceSet() // empty
  }

  def regionFor(resource: IpResource, holdings: Holdings): String = {
    holdings.find(_._2.contains(resource)).map(_._1).getOrElse("?")
  }
}


object RIRHoldings {

  def parse(statLines: Seq[String]): Holdings = {

    val afrinic = new IpResourceSet()
    val afrinicReserved = new IpResourceSet()
    val apnic = new IpResourceSet()
    val apnicReserved = new IpResourceSet()
    val arin = new IpResourceSet()
    val arinReserved = new IpResourceSet()
    val lacnic = new IpResourceSet()
    val lacnicReserved = new IpResourceSet()
    val ripencc = new IpResourceSet()
    val ripenccReserved = new IpResourceSet()

    AnalysedHoldings.parse(statLines) { tokens =>

      if (tokens.length >= 7) {
        tokens(6) match {
          case "assigned" => tokens(0) match {
            case "afrinic" => afrinic.addAll(parseResourceLine(tokens))
            case "apnic" => apnic.addAll(parseResourceLine(tokens))
            case "arin" => arin.addAll(parseResourceLine(tokens))
            case "lacnic" => lacnic.addAll(parseResourceLine(tokens))
            case "ripencc" => ripencc.addAll(parseResourceLine(tokens))
            case _ => // move along
          }
          case "reserved" => tokens(0) match {
            case "afrinic" => afrinicReserved.addAll(parseResourceLine(tokens))
            case "apnic" => apnicReserved.addAll(parseResourceLine(tokens))
            case "arin" => arinReserved.addAll(parseResourceLine(tokens))
            case "lacnic" => lacnicReserved.addAll(parseResourceLine(tokens))
            case "ripencc" => ripenccReserved.addAll(parseResourceLine(tokens))
            case _ => // move along
          }
          case _ => // move along
        }
      }
    }

    Map("afrinic" -> afrinic, "apnic" -> apnic, "arin" -> arin, "lacnic" -> lacnic, "ripencc" -> ripencc,
      "afrinicReserved" -> afrinicReserved, "apnicReserved" -> apnicReserved, "arinReserved" -> arinReserved, "lacnicReserved" -> lacnicReserved, "ripenccReserved" -> ripenccReserved)
  }


}

/**
  * Finds all the resources 'assigned' by RIRs per country
  */
object CountryHoldings {

  def parse(statLines: Seq[String]): Holdings = {
    val countryMap = collection.mutable.Map[String, IpResourceSet]()
    AnalysedHoldings.parse(statLines) { tokens =>
      tokenConsumer(countryMap, tokens)
    }
    countryMap.toMap
  }

  def tokenConsumer(countryMap: mutable.Map[String, IpResourceSet], tokens: Array[String]) = {
    if (tokens.length >= 7 && tokens(6) == "assigned") {
      val cc = tokens(1)
      if (!countryMap.contains(cc)) {
        countryMap += (cc -> new IpResourceSet())
      }
      countryMap(cc).addAll(parseResourceLine(tokens))
    }
  }
}


/**
  * Finds all the resources 'assigned' by RIRs per country
  */
object EntityHoldings {

  def parse(statLines: Seq[String]): (EntityRegionHoldings, EntityRegionHoldings) = {
    val entityCountryMap = mutable.Map[EntityRegion, IpResourceSet]()
    val entityRIRMap = mutable.Map[EntityRegion, IpResourceSet]()
    AnalysedHoldings.parse(statLines) { tokens =>
      tokenConsumer(entityCountryMap, entityRIRMap, tokens)
    }
    (entityCountryMap.toMap, entityRIRMap.toMap)
  }

  def tokenConsumer(entityCountryMap: mutable.Map[EntityRegion, IpResourceSet],
                    entityRIRMap: mutable.Map[EntityRegion, IpResourceSet],
                    tokens: Array[String]) = {
      if (tokens.length >= 8 && tokens(6) == "assigned") {
        val rir = tokens(0)
        val country = tokens(1)
        val entity = tokens(7)

        if (!entityCountryMap.contains((entity, country))) {
          entityCountryMap += ((entity, country) -> new IpResourceSet())
        }
        entityCountryMap((entity, country)).addAll(parseResourceLine(tokens))

        if (!entityRIRMap.contains((entity, rir))) {
          entityRIRMap += ((entity, rir) -> new IpResourceSet())
        }
        entityRIRMap((entity, rir)).addAll(parseResourceLine(tokens))
      }
  }
}

object AnalysedHoldings {
  def parse[T](statLines: Seq[String])(consumeTokens: Array[String] => T): Unit =
    statLines.foreach { line =>
      consumeTokens(line.split('|'))
    }

}
