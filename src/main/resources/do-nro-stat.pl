#!/usr/bin/perl

use warnings;
use strict;

use File::Path; # Provides mkdir -p like functionality

#
# RPKI vs BGP stats
#
# Talk to Tim!
#
# For configuration see inline below, before the first 'sub'..

#
# Specify the base dir for the script.
my $rpki_irr_bgp_stats_bin="/home/awibisono/cert-stats/irrstats-0.1-SNAPSHOT/rpki-irr-bgp-stats";


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
my $stats_dir = "/home/awibisono/cert-stats/nro-stat";

sub compose_directory_path_for_date($) {
   my $tal_dir = shift;
   my $year = `date +%Y`; chomp($year);
   my $mnth = `date +%m`; chomp($mnth);
   my $day = `date +%d`; chomp($day);
   return $tal_dir."/".$year."/".$mnth."/".$day;
}

my $todays_stat_dir = compose_directory_path_for_date($stats_dir);
mkpath($todays_stat_dir);

my $ipv4_bgp_dump = "http://www.ris.ripe.net/dumps/riswhoisdump.IPv4.gz";

print "\nGetting ris dump";
chdir $todays_stat_dir;
system("curl $ipv4_bgp_dump -o risdump-ipv4.txt.gz 2>/dev/null");
system("gunzip -f risdump-ipv4.txt.gz");

print "\nGetting nro extended stats";
my $nro_ext_stats = "https://www.nro.net/wp-content/uploads/apnic-uploads/delegated-extended";
system("curl $nro_ext_stats -o nro-ext-stats.txt 2>/dev/null");

print "\nGetting roas from validator 3";
my $all_roas = "https://rpki-validator.prepdev.ripe.net/api/export.csv";
system("curl -XGET --header 'Accept: text/csv' $all_roas -o all-roas.csv 2>/dev/null");

print "\nGetting certified resources from validator 3";
my $certified_resources = "https://rpki-validator.prepdev.ripe.net/api/rpki-objects/certified.csv";
system("curl -X GET --header 'Accept: text/csv'  $certified_resources -o certified.csv 2>/dev/null");

system("$rpki_irr_bgp_stats_bin -b $todays_stat_dir/risdump-ipv4.txt -s $todays_stat_dir/nro-ext-stats.txt -r $todays_stat_dir/all-roas.csv -f $todays_stat_dir/certified.csv --rir-adoption | column -s',' -t  > $todays_stat_dir/adoption.txt  2>/dev/null");
system("$rpki_irr_bgp_stats_bin -b $todays_stat_dir/risdump-ipv4.txt -s $todays_stat_dir/nro-ext-stats.txt -r $todays_stat_dir/all-roas.csv -f $todays_stat_dir/certified.csv --rir-activation | column -s',' -t > $todays_stat_dir/activation.txt  2>/dev/null");
system("$rpki_irr_bgp_stats_bin -b $todays_stat_dir/risdump-ipv4.txt -s $todays_stat_dir/nro-ext-stats.txt -r $todays_stat_dir/all-roas.csv -f $todays_stat_dir/certified.csv -n > $todays_stat_dir/nro-stats.html 2>/dev/null");

# Export rsync password in RSYNC_PASSWORD
my $rsync_target = "rpki\@dragonstone.ripe.net::rpki";

system("rsync $todays_stat_dir/adoption.txt $rsync_target");
system("rsync $todays_stat_dir/activation.txt $rsync_target");
system("rsync $todays_stat_dir/nro-stats.html $rsync_target");

exit;
