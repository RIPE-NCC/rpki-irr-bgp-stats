#!/usr/bin/env bash
#
# Copyright (c) 2015-2107 RIPE NCC
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#   - Redistributions of source code must retain the above copyright notice,
#     this list of conditions and the following disclaimer.
#   - Redistributions in binary form must reproduce the above copyright notice,
#     this list of conditions and the following disclaimer in the documentation
#     and/or other materials provided with the distribution.
#   - Neither the name of this software, nor the names of its contributors, nor
#     the names of the contributors' employers may be used to endorse or promote
#     products derived from this software without specific prior written
#     permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
# ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
# LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
# CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
# SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
# INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
# CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
# ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.
#


rpki_stats_bin='/home/app-admin/cert-stats/world-stats/irrstats-0.1-SNAPSHOT/rpki-irr-bgp-stats'
stats_dir='/ncc/archive2/certstats/rpki-bgp'
DD=`date +%Y/%m/%d`

today_dir="$stats_dir/$DD"

echo "Creating todays dir $today_dir"
mkdir -p $today_dir

ipv4_bgp_dump="http://www.ris.ripe.net/dumps/riswhoisdump.IPv4.gz";
validator_url="https://rpki-validator.prepdev.ripe.net"
nro_ext_stats="https://www.nro.net/wp-content/uploads/apnic-uploads/delegated-extended"

function exit_if_no_file() {
  if [ ! -s $1 ]; then
    echo "Oops, file $1 wasn't downloaded, cannot continue" 1>&2
    exit 1;
  fi
}

cd $today_dir;

echo "Getting ris dump";
wget --quiet -O risdump-ipv4.txt.gz $ipv4_bgp_dump
exit_if_no_file risdump-ipv4.txt.gz
gzip -df risdump-ipv4.txt.gz

echo "Getting nro extended stats";
wget --quiet -O nro-ext-stats.txt $nro_ext_stats
exit_if_no_file nro-ext-stats.txt

echo "Getting roas from validator 3";
all_roas="$validator_url/api/export.csv"
wget --quiet --header="Accept: text/csv" -O all-roas.csv $all_roas
exit_if_no_file all-roas.csv

echo "Getting certified resources from validator 3";
certified_resources="$validator_url/api/rpki-objects/certified.csv"
wget --quiet --header="Accept: text/csv" -O certified.csv $certified_resources
exit_if_no_file certified.csv

common_args="-b $today_dir/risdump-ipv4.txt -s $today_dir/nro-ext-stats.txt -r $today_dir/all-roas.csv -f $today_dir/certified.csv"

echo "Generating country adoption"
$rpki_stats_bin $common_args --country-adoption > $today_dir/country-adoption.csv
cat $today_dir/country-adoption.csv | column -s',' -t  > $today_dir/country-adoption.txt

echo "Generating country activation"
$rpki_stats_bin $common_args --country-activation > $today_dir/country-activation.csv
cat $today_dir/country-activation.csv | column -s',' -t > $today_dir/country-activation.txt

echo "Generating rir adoption"
$rpki_stats_bin $common_args --rir-adoption > $today_dir/rir-adoption.csv
cat $today_dir/rir-adoption.csv | column -s',' -t  > $today_dir/rir-adoption.txt

echo "Generating rir activation"
$rpki_stats_bin $common_args --rir-activation  > $today_dir/rir-activation.csv
cat $today_dir/rir-activation.csv | column -s',' -t > $today_dir/rir-activation.txt

echo "Generating nro-stats web page"
$rpki_stats_bin $common_args -n > $today_dir/nro-stats.html

echo "Generating world roas"
$rpki_stats_bin $common_args -w > $today_dir/world-roas.html
cp $todays_stat_dir/world-roas.html /export/certcontent/static/statistics/world-roas.html

rsync_target="rpki@dragonstone.ripe.net::rpki"

# Fill in nro RSYNC Password!!
export RSYNC_PASSWORD=

function rsync_if_not_empty() {
    if [ -s $today_dir/$1 ]
    then
        rsync $today_dir/$1 $rsync_target
        echo "Successfully rsynced $1"
    else
        echo "Failed to create $1, not rsyincing to NRO"
    fi
}

rsync_if_not_empty country-adoption.txt
rsync_if_not_empty country-adoption.csv
rsync_if_not_empty country-activation.txt
rsync_if_not_empty country-activation.csv
rsync_if_not_empty rir-adoption.txt
rsync_if_not_empty rir-adoption.csv
rsync_if_not_empty rir-activation.txt
rsync_if_not_empty rir-activation.csv
rsync_if_not_empty nro-stats.html
