#!/usr/bin/env bash


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
