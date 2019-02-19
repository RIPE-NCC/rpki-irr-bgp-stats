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

# Without any argument, will run for today, otherwise it will run it for the date specified in $1
if [ -z "$1" ]; then
	DD=`date +%Y/%m/%d`
else
	DD=$1
fi

today_dir="$stats_dir/$DD"
today_dir_nro="$stats_dir_nro/$DD"

function exit_if_no_file() {
  if [ ! -s $1 ]; then
    echo "Oops, file $1 wasn't downloaded, cannot continue"
    exit 1;
  fi
}

cd $today_dir;

exit_if_no_file $PWD/risdump-ipv4.txt
exit_if_no_file $PWD/nro-ext-stats.txt
exit_if_no_file $PWD/all-roas.csv  


args="-b $today_dir/risdump-ipv4.txt -s $today_dir/nro-ext-stats.txt -r $today_dir/all-roas.csv --ripe-country-roa"


echo "Generating country roa counts for ripe on date $DD "
$rpki_stats_bin $args > $today_dir_nro/ripe-country-roa.csv
cat $today_dir_nro/ripe-country-roa.csv | column -s',' -t  > $today_dir_nro/ripe-country-roa.txt


