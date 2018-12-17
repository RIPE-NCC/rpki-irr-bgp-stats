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



STATS_SCRIPT_DIR="/home/app-admin/cert-stats"

# Specify the full path to the validator
VALIDATOR_BIN="$STATS_SCRIPT_DIR/validator/bin/certification-validator"

#
# Specify the base directory where this script will archive the
# validation output and summary.
#
# The script will store results like this:
#
#  /path/to/base/data/dir
#           /example.tal
#                 stats-summary.txt
#                 /20110131   (date)
#                     validator.log
#                     validator.err
#                     roas.csv
#                     out/... (normal validator out work-directory)
#                 /... (more dates)
#           /... (more TALs)
#
BASE_DATA_DIR="/ncc/archive2/certstats"

# The script will store the summary output for each tal in a separate file
# and sync it over to whelk. Do not use trailing slashes.. they are added by the code..
SUMMARIES_DIR="$BASE_DATA_DIR/summaries"
RSYNC_TARGET="/export/certcontent/static/statistics"

CERTIFIED_RESOURCES_URL=https://rpki-validator.prepdev.ripe.net/api/rpki-objects/certified.csv

# curl -s -X GET --header 'Accept: text/csv' $CERTIFIED_RESOURCES_URL > certified-resources.csv


DD=`date +%Y/%m/%d`

for TAL in 'afrinic.tal' 'arin.tal' 'lacnic.tal' 'ripencc.tal' 'localcert.tal' 'apnic.tal' ; do
    TAL_STATS_DIR="$BASE_DATA_DIR/$TAL"
    TODAYS_STAT_DIR="$TAL_STATS_DIR/$DD"
    mkdir -p "$TODAYS_STAT_DIR"

    VALIDATOR_LOG_FILE="$TODAYS_STAT_DIR/validator.log"
    VALIDATOR_ERR_FILE="$TODAYS_STAT_DIR/validator.err"
    TAL_STATS_FILE="$TODAYS_STAT_DIR/$TAL.txt"

    $VALIDATOR_BIN -t $STATS_SCRIPT_DIR/tal/$TAL -o $TODAYS_STAT_DIR/out -r $TODAYS_STAT_DIR/roas.csv >$VALIDATOR_LOG_FILE 2>$VALIDATOR_ERR_FILE

    tail -1 $VALIDATOR_LOG_FILE >> $TAL_STATS_FILE

    # will complain about trailing slashes otherwise
    tar cf $TODAYS_STAT_DIR/validator-work.tar $TODAYS_STAT_DIR/out 2>/dev/null
    rm -rf $TODAYS_STAT_DIR/out
done

rsync -qa $SUMMARIES_DIR/* $RSYNC_TARGET
