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
stats_dir_nro='/ncc/archive2/certstats/rpki-bgp-nro'

DD=`date +%Y/%m/%d`

today_dir="$stats_dir/$DD"
today_dir_nro="$stats_dir_nro/$DD"

echo "Creating todays dirs $today_dir and $today_dir_nro"
mkdir -p $today_dir
mkdir -p $today_dir_nro

ipv4_bgp_dump="http://www.ris.ripe.net/dumps/riswhoisdump.IPv4.gz";
validator_url="https://rpki-validator.prepdev.ripe.net"
nro_ext_stats="https://www.nro.net/wp-content/uploads/apnic-uploads/delegated-extended"

function exit_if_no_file() {
  if [ ! -s $1 ]; then
    echo "Oops, file $1 wasn't downloaded, cannot continue"
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
certified_resources="$validator_url/api/rpki-objects/certificates.csv"
wget --quiet --header="Accept: text/csv" -O $today_dir_nro/certificates.csv $certified_resources
exit_if_no_file $today_dir_nro/certificates.csv

common_args="-b $today_dir/risdump-ipv4.txt -s $today_dir/nro-ext-stats.txt -r $today_dir/all-roas.csv -f $today_dir_nro/certificates.csv"

echo "Generating country adoption"
$rpki_stats_bin $common_args --country-adoption > $today_dir_nro/country-adoption.csv
cat $today_dir_nro/country-adoption.csv | column -s',' -t  > $today_dir_nro/country-adoption.txt

echo "Generating country activation"
$rpki_stats_bin $common_args --country-activation > $today_dir_nro/country-activation.csv
cat $today_dir_nro/country-activation.csv | column -s',' -t > $today_dir_nro/country-activation.txt

echo "Generating rir adoption"
$rpki_stats_bin $common_args --rir-adoption > $today_dir_nro/rir-adoption.csv
cat $today_dir_nro/rir-adoption.csv | column -s',' -t  > $today_dir_nro/rir-adoption.txt

echo "Generating rir activation"
$rpki_stats_bin $common_args --rir-activation  > $today_dir_nro/rir-activation.csv
cat $today_dir_nro/rir-activation.csv | column -s',' -t > $today_dir_nro/rir-activation.txt

echo "Generating web page"
$rpki_stats_bin $common_args -n > $today_dir_nro/nro-stats.html

echo "Generating world roas"
$rpki_stats_bin $common_args -w > $today_dir/world-roas.html
cp $today_dir/world-roas.html /export/certcontent/static/statistics/world-roas.html

# Export rsync password in RSYNC_PASSWORD !!! set it up for env variable of whoever doing cron jobs for this (most likely app-admin)
rsync_target="rpki@dragonstone.ripe.net::rpki"

export RSYNC_PASSWORD=

function rsync_if_not_empty() {
    if [ -s $1 ]
    then
        rsync $1 $rsync_target
        echo "Successfully rsynced $1"
    else
        echo "Failed to create $1, not rsyincing to NRO"
    fi
}

rsync_if_not_empty $today_dir_nro/country-adoption.txt
rsync_if_not_empty $today_dir_nro/country-adoption.csv
rsync_if_not_empty $today_dir_nro/country-activation.txt
rsync_if_not_empty $today_dir_nro/country-activation.csv
rsync_if_not_empty $today_dir_nro/rir-adoption.txt
rsync_if_not_empty $today_dir_nro/rir-adoption.csv
rsync_if_not_empty $today_dir_nro/rir-activation.txt
rsync_if_not_empty $today_dir_nro/rir-activation.csv
rsync_if_not_empty $today_dir_nro/nro-stats.html
