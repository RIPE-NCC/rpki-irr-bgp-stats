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

import net.ripe.irrstats.parsing.holdings.{CountryHoldings, RIRHoldings}
import net.ripe.irrstats.parsing.ris.RisDumpUtil
import net.ripe.irrstats.parsing.roas.RoaUtil
import net.ripe.irrstats.parsing.route.RouteParser
import net.ripe.irrstats.route.validation.{BgpAnnouncement, RtrPrefix}

object Main extends App {

  val config = Config.config(args)

  val announcements: Seq[BgpAnnouncement] = RisDumpUtil.parseDumpFile(config.risDumpFile)

  val authorisations: Seq[RtrPrefix] = config.routeAuthorisationType() match {
    case RoaCsvDump => RoaUtil.parse(config.routeAuthorisationFile)
    case RouteObjectDbDump => if (config.looseRouteObjectValidation) {
      RouteParser.parse(config.routeAuthorisationFile).map(r => RtrPrefix(r.asn, r.prefix, Some(24)))
    } else {
      RouteParser.parse(config.routeAuthorisationFile).map(r => RtrPrefix(r.asn, r.prefix))
    }
  }

  // lazy vals are only initialised when used for the first time,
  // so there is no performance penalty defining all of the following
  lazy val rirHoldings = RIRHoldings.parse(config.statsFile)
  lazy val countryHoldings = CountryHoldings.parse(config.statsFile)

  config.analysisMode match {
    case AsnMode => ReportAsn.report(announcements, authorisations, config.quiet)
    case WorldMapMode =>  ReportWorldMap.report(announcements, authorisations, countryHoldings)
    case CountryDetailsMode => ReportCountry.reportCountryDetails(announcements, authorisations, countryHoldings, config.countryDetails.get)
    case CountryMode => ReportCountry.reportCountries(announcements, authorisations, countryHoldings, config.quiet, config.date)
    case RirMode => ReportRir.report(announcements, authorisations, rirHoldings, config.quiet, config.date, config.rir)
  }

  System.exit(0) // We're done here

}
