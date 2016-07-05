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
package net.ripe.irrstats.map


object WorldMapPage {

  private def convertValuesToArrayData(countryValues: Map[String, Double]) = {
    countryValues.map { entry => "['" + entry._1 + "', " + f"${entry._2 * 100}%3.2f]" }.mkString(",\n          ")
  }


  // Returns an HTML page as a String with a Google Geomap world map and embedded data
  def printWorldMapHtmlPage(prefixesAdoptionValues: Map[String, Double], prefixesValidValues: Map[String, Double], prefixesMatchingValues: Map[String, Double], adoptionValues: Map[String, Double], validValues: Map[String, Double], matchingValues: Map[String, Double]): String = {

    // Yes, I am aware that better template frameworks exist, but I just have one simple thing to do, and prefer no deps.
    scala.io.Source.fromInputStream(getClass.getResourceAsStream("/worldmap-template.html")).getLines().map { line =>

      line
        .replace("***COUNTRY_PREFIXES_ADOPTION***", convertValuesToArrayData(prefixesAdoptionValues))
        .replace("***COUNTRY_PREFIXES_VALID***", convertValuesToArrayData(prefixesValidValues))
        .replace("***COUNTRY_PREFIXES_MATCHING***", convertValuesToArrayData(prefixesMatchingValues))
        .replace("***COUNTRY_ADOPTION***", convertValuesToArrayData(adoptionValues))
        .replace("***COUNTRY_VALID***", convertValuesToArrayData(validValues))
        .replace("***COUNTRY_MATCHING***", convertValuesToArrayData(matchingValues))
    }.mkString("\n")
  }


}
