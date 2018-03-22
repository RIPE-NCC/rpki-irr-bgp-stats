# rpki-irr-bgp-stats

## Using the tool

Build: mvn clean install

This produces an artifact: target/irrstats-0.1-SNAPSHOT-dist.tar.gz

Unpack this somewhere and run:

./rpki-irr-bgp-stats

This will then complain and list all the options.

## Analysis modes

### General working

This tool takes three inputs:
* NRO delegated extended stats
* BGP RIS dump with announcements
* An authorisation file, either with ROA data or route objects

The ROA data is expected to be in the CSV format of the RIPE NCC RPKI Validator 1.x command line tool.
I.e. it has the following columns:  ASN,IP Prefix,Max Length

Route objects can also be used. This is tested with the ROUTE splitfile from the RIPE DB, but should
work with other sources (famous last words..). It does insist on .txt. This was a quick hack, so no
additional config parameter would be needed. ROUTE objects will be treated as though they are ROAs
w.r.t. validation. Since ROUTE objects have no max length it is assumed to be the same as the prefix,
unless the "--loose" flag is used.. in that case it's assumed to be /24 (yes, hardcoded v4)

Generally speaking the script then does the following:

 * parse the extended stats and put the resources in a map where the keys are either RIRs or country codes
 * parse the announcements and and using the map above, create a map of RIR/CC -> list of announcements
 * parse the ROA.csv or ROUTE.txt file and create a full list of authorisations (not mapped)

 * Validate all announcements in relation to the authorisations
 ** This part uses the BGP preview code from our 2.x validator, copied over into this code
 ** The code uses a tree to make it slightly more efficient: it will only look at relevant authorisations
 ** Still.. if you run this with a full ROUTE.txt dump, it takes a while..

 * then report, see sections below..

Note that this is only really tested with IPv4 as input. The analysis done by this script could be
applied to IPv6, but given the huge size differences of IPv6 prefixes I am not sure what the numbers would mean..


### RIR based

When run in RIR mode the basic analysis as described above is done and report one line per RIR with the
following elements:

    date, (defaults to today, can be overriden to get past date stats)
    RIR,
    authorisations, (number of authorisations, VRPs, in RIR)
    announcements, (number of announcements for RIR space)
    accuracy announcements, (in terms of #announcements valid / #announcements covered)
    fraction valid, (#announcements valid / #announcements done for RIR space)
    fraction invalid length, (#announcements valid / #announcements done for RIR space)
    fraction invalid asn, (#announcements valid / #announcements done for RIR space)
    fraction unknown, (#announcements unknown / #announcements done for RIR space -> uptake = 1 - $this)
    space announced,  (total number of IP addresses in all announcements for RIR)
    accuracy space, (addresses in valid announcements / addresses in all covered announcements)
    fraction space valid, (addresses in valid announcements / space announced)
    fraction space invalid length, (addresses in invalid length announcements / space announced)
    fraction space invalid asn, (addresses in invalid asn announcements / space announced)
    fraction space unknown (addresses in unknown announcements / space announced, uptake = 1 - $this)

Note that the space fractions do not add up to 100%. This is because there can be valid and invalid announcements
for the same space.

### Country based

Exactly as RIR based, except that the country code is to group resources rather than the RIR

### World map

As country based, but output is an HTML file (with a RIPE NCC template) with worldmaps showing some stats.
Published here, daily:

https://lirportal.ripe.net/certification/content/static/statistics/world-roas.html

### Country details report

Different from above. It's just announcement based (not space) and shows a summary report for a specific country code,
and it goes on to list ALL invalid length / invalid ASN announcements for the country.

I thought of extending this to a report based on the resource holder, but didn't get to it. We only have opaque IDs
in the input. But in principle another source could be used, e.g. INETNUM dumps and "org:" attributes in the RIPE
DB, not sure about the others.

The idea was that this might be useful when doing outreach or training in a country, or when contacting a specific
member.

### ASN report

This groups announcements by ASN rather than country and shows the top 100 ASNs, based on space valid.

There is room to extend this.. e.g. split announcements to country of ASN rather than country of prefix. Or list all
of them. Or... your idea here ;)









