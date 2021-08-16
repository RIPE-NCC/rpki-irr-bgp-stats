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
package net.ripe.irrstats.reporting
import java.math.BigInteger

import MapUtils._

object NROStatsPage {

  // Returns an HTML page as a String with a Google Geomap world map and embedded data
  def printNROStatsPage(ipv4CountryAdoptionValues: Map[String, Double],
                        ipv6CountryAdoptionValues: Map[String, Double],
                        ipv4CountryBubbleData: Map[String, (Double, Int, Int, BigInteger)],
                        ipv6CountryBubbleData: Map[String, (Double, Int, Int, BigInteger)],
                        ipv4RIRAdoptionValues: Map[String, Double],
                        ipv6RIRAdoptionValues: Map[String, Double]
                       ): String = {

    // Yes, I am aware that better template frameworks exist, but I just have one simple thing to do, and prefer no deps. --Tim, 2016
    scala.io.Source.fromInputStream(getClass.getResourceAsStream("/nro-stats-template.html")).getLines().map { line =>

      line
        .replace("//***IPV4_COUNTRY_ADOPTION***//", convertValuesToArrayData(ipv4CountryAdoptionValues))
        .replace("//***IPV6_COUNTRY_ADOPTION***//", convertValuesToArrayData(ipv6CountryAdoptionValues))
        .replace("//***IPV4_COUNTRY_ADOPTION_BUBBLE***//", convertValuesToBubbleArrayData(ipv4CountryBubbleData))
        .replace("//***IPV6_COUNTRY_ADOPTION_BUBBLE***//", convertValuesToBubbleArrayData(ipv6CountryBubbleData))
        .replace("//***IPV4_RIR_ADOPTION***//", convertValuesToRIRArrayData(ipv4RIRAdoptionValues))
        .replace("//***IPV6_RIR_ADOPTION***//", convertValuesToRIRArrayData(ipv6RIRAdoptionValues))
        .replace("//***IPV4_RIR_ADOPTION_TABLE***//", convertValuesToArrayData(ipv4RIRAdoptionValues))
        .replace("//***IPV6_RIR_ADOPTION_TABLE***//", convertValuesToArrayData(ipv6RIRAdoptionValues))
    }.mkString("\n")
  }
}
