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

import java.io.File

import org.joda.time.DateTime

object Config {

  private val ApplicationName = "rpki-irr-bgp-stats"
  private val ApplicationVersion = "0.1"

  private val cliOptionParser = new scopt.OptionParser[Config](ApplicationName) {
    head(ApplicationName, ApplicationVersion)

    opt[File]('b', "bgp-announcements") required() valueName "<file>" action { (x, c) =>
      c.copy(risDumpFile = x)
    } text {
      "Location of a RIS Dump style file with BGP announcements "
    }
    opt[File]('s', "extended-delegated-stats") required() valueName "<file>" action { (x, c) =>
      c.copy(statsFile = x)
    } text {
      "Location of a copy of the NRO extended delegated stats"
    }
    opt[File]('r', "route-authorisations") required() valueName "<file>" action { (x, c) =>
      c.copy(routeAuthorisationFile = x)
    } text {
      "Location of a file with either ROA export from the RIPE NCC RPKI Validator (.csv) or route[6] objects (.txt)"
    }
    opt[File]('f', "certified-resources") required() valueName "<file>" action { (x, c) =>
      c.copy(certifiedResourceFile = x)
    } text {
      "Location of a file with dump of certified resources from RIPE NCC Validator (.csv) file."
    }
    opt[Unit]('q', "quiet") optional() action { (x, c) => c.copy(quiet = true) } text {
      "Quiet output, just (real) numbers"
    }
    opt[String]('d', "date") optional() action { (x, c) => c.copy(date = x) } text {
      "Override date string, defaults to today"
    }
    opt[String]('r', "rir") optional() action { (x, c) => c.copy(rir = x) } text {
      "Only show results for specified rir, defaults to all"
    }

    opt[String]('x', "country-details") optional() action { (x, c) => c.copy(analysisMode = CountryDetailsMode, countryDetails = Some(x)) } text {
      "Do a detailed announcement report for country code"
    }
    opt[Unit]('c', "countries") optional() action { (x, c) => c.copy(analysisMode = CountryMode) } text {
      "Do a report per country instead of per RIR"
    }
    opt[Unit]('w', "worldmap") optional() action { (x, c) => c.copy(analysisMode = WorldMapMode) } text {
      "Produce an HTML page with country stats projected on a number of world maps"
    }
    opt[Unit]('n', "nro-stats") optional() action { (x, c) => c.copy(analysisMode = NROStatsMode) } text {
      "Produce an HTML page with country adoption projected on world maps"
    }
    opt[Unit]('a', "asn") optional() action { (x, c) => c.copy(analysisMode = AsnMode) } text {
      "Find and report top ASNs"
    }
    opt[Unit]('l', "loose") optional() action { (x, c) => c.copy(looseRouteObjectValidation = true) } text {
      "Accept all more specific announcments for ROUTE ojects (defaults to strict)"
    }

    opt[Unit]("country-adoption") optional() action { (x,c) => c.copy(analysisMode = CountryAdoptionMode)} text {
      "Do report country adoptions "
    }

    opt[Unit]("rir-adoption") optional() action { (x,c) => c.copy(analysisMode = RirAdoptionMode)} text {
      "Do report rir adoptions "
    }

    opt[Unit]("country-activation") optional() action { (x,c) => c.copy(analysisMode = CountryActivationMode)} text {
      "Do report country activation "
    }

    opt[Unit]("rir-activation") optional() action { (x,c) => c.copy(analysisMode = RirActivationMode)} text {
      "Do report rir activation "
    }


    checkConfig { c =>
      if (!c.routeAuthorisationFile.getName.endsWith(".csv") && !c.routeAuthorisationFile.getName.endsWith(".txt")) failure("option -r must refer roas.csv or route[6].db file") else success
    }

  }

  def config(args: Array[String]) = cliOptionParser.parse(args, Config()).getOrElse {
    sys.exit(1)
  }

}

case class Config(risDumpFile: File = new File("."),
                  statsFile: File = new File("."),
                  routeAuthorisationFile: File = new File("."),
                  certifiedResourceFile: File = new File("."),
                  looseRouteObjectValidation: Boolean = true,
                  quiet: Boolean = false,
                  date: String = DateTime.now().toString("YYYYMMdd"),
                  analysisMode: AnalysisMode = RirMode,
                  rir: String = "all",
                  countryDetails: Option[String] = None) {

  def routeAuthorisationType(): RouteAuthorisationDumpType = {
    if (routeAuthorisationFile.getName.endsWith(".csv")) {
      RoaCsvDump
    } else {
      RouteObjectDbDump
    }
  }
}

sealed trait RouteAuthorisationDumpType

case object RoaCsvDump extends RouteAuthorisationDumpType

case object RouteObjectDbDump extends RouteAuthorisationDumpType

sealed trait AnalysisMode

case object CountryMode extends AnalysisMode

case object CountryAdoptionMode extends AnalysisMode

case object CountryDetailsMode extends AnalysisMode

case object RirMode extends AnalysisMode

case object RirAdoptionMode extends AnalysisMode

case object WorldMapMode extends AnalysisMode

case object NROStatsMode extends AnalysisMode

case object AsnMode extends AnalysisMode

case object RirActivationMode extends AnalysisMode

case object CountryActivationMode extends AnalysisMode