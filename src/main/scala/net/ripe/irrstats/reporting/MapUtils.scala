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
package net.ripe.irrstats.reporting

object MapUtils {

  val ShowMatchingThreshold = 0.001 // 0.1%

  // Capturing RIR in hardcoded subcontinents code
  // Subcontinents code supported by geochart: https://developers.google.com/chart/interactive/docs/gallery/geochart#continent-hierarchy-and-codes
  // We can switch to more granular country code level if this is not good enough to represent RIRs
  val subcontinents= Map(
    "apnic" -> List(30,35,53,54,57,61),
    "afrinic" -> List(11,14,15,17,18),
    "ripencc" -> List(34,151,154,155,39,143,145),
    "arin" -> List(21,29),
    "lacnic" -> List(5,13)
  )

  def findMatchingValuesAboveAdoptionThreshold(adoptionValues: Map[String, Double], matchingValues: Map[String, Double]): Map[String, Double] = {
    matchingValues.filter(mv => adoptionValues.isDefinedAt(mv._1) && adoptionValues.get(mv._1).get > ShowMatchingThreshold)
  }

  def convertValuesToArrayData(countryValues: Map[String, Double]) = {
    countryValues.map { case entry => "['" + entry._1 + "', " + f"${entry._2 * 100}%3.2f]" }.mkString(",\n          ")
  }

  def convertValuesToRIRArrayData(rirValues: Map[String, Double]) = {
    rirValues.flatMap { case (rirRegion, fraction) => subcontinents(rirRegion).flatMap { subContinentCode =>
       val scode = "%03d" format subContinentCode
       val fract = "%3.2f" format (fraction*100)
       Seq(s"['$scode', '${rirRegion.toUpperCase}', $fract]")
    }}.mkString(",\n          ")
  }

}
