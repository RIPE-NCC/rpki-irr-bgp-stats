#!/usr/bin/env bash


#
# RPKI vs BGP stats
#
# Talk to Tim!
#
# For configuration see inline below, before the first 'sub'..

#
# Specify the base dir for the script.
RPKI_IRR_BGP_STATS_BIN="/home/app-admin/cert-stats/world-stats/irrstats-0.1-SNAPSHOT/rpki-irr-bgp-stats"


#
# This script will store results like:
#
#  /ncc/archive2/certstats/rpki-bgp
#                            /YYYY/MM/DD
#                              risdump-ipv4.txt
#                              nro-ext-stats.txt
#                              all-roas.csv
#                              world-roas.html
#
STATS_DIR="/ncc/archive2/certstats/rpki-bgp"

IPV4_BGP_DUMP="http://www.ris.ripe.net/dumps/riswhoisdump.IPv4.gz"

RSYNC_TARGET="/export/certcontent/static/statistics"

DD=`date +%Y/%m/%d`

#sub compose_directory_path_for_date($) {
#   my $tal_dir = shift;
#   my $year = `date +%Y`; chomp($year);
#   my $mnth = `date +%m`; chomp($mnth);
#   my $day = `date +%d`; chomp($day);
#   return $tal_dir."/".$year."/".$mnth."/".$day;
#}

TODAYS_STAT_DIR="$STATS_DIR/$DD"
mkdir -p $TODAYS_STAT_DIR


cd $TODAYS_STAT_DIR
curl $IPV4_BGP_DUMP -o risdump-ipv4.txt.gz 2>/dev/null
gunzip -f risdump-ipv4.txt.gz

NRO_EXT_STATS="https://www.nro.net/wp-content/uploads/apnic-uploads/delegated-extended"
curl $NRO_EXT_STATS -o nro-ext-stats.txt 2>/dev/null

for TAL in 'afrinic.tal' 'arin.tal' 'lacnic.tal' 'ripencc.tal' 'localcert.tal' 'apnic.tal' ; do
    TAL_DIR="/ncc/archive2/certstats/$TAL/$DD"
    tail -n +2 $TAL_DIR/roas.csv | awk -F ',' '{print $2","$3","$4}' >> $TODAYS_STAT_DIR/all-roas.csv
done

#my @tals = ( 'afrinic.tal', 'arin.tal', 'lacnic.tal', 'ripencc.tal', 'apnic.tal' );
#foreach my $tal (@tals) {
#  my $tal_dir = compose_directory_path_for_date("/ncc/archive2/certstats/$tal");
#  system("tail -n +2 $tal_dir/roas.csv | awk -F ',' '{print \$2\",\"\$3\",\"\$4}' >> $todays_stat_dir/all-roas.csv");
#}

$RPKI_IRR_BGP_STATS_BIN -b $TODAYS_STAT_DIR/risdump-ipv4.txt \
                        -s $TODAYS_STAT_DIR/nro-ext-stats.txt \
                        -r $TODAYS_STAT_DIR/all-roas.csv -w > $TODAYS_STAT_DIR/world-roas.html 2>/dev/null

cp $TODAYS_STAT_DIR/world-roas.html /export/certcontent/static/statistics/world-roas.html


