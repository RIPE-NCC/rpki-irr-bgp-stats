function create_todays_stat_dir() {
   local year=`date +%Y`; 
   local mnth=`date +%m`; 
   local day=`date +%d`; 
   echo "Base $1"
   eval "$2=$1/$year/$mnth/$day"
}

rpki_stats_bin='/home/awibisono/cert-stats/irrstats-0.1-SNAPSHOT/rpki-irr-bgp-stats'
stats_dir='/home/awibisono/cert-stats/nro-stat'

create_todays_stat_dir $stats_dir todays_stat_dir

echo "Creating todays dir $todays_stat_dir"
mkdir -p $todays_stat_dir


ipv4_bgp_dump="http://www.ris.ripe.net/dumps/riswhoisdump.IPv4.gz";

echo "Getting ris dump";
cd $todays_stat_dir;
curl $ipv4_bgp_dump -o risdump-ipv4.txt.gz 2>/dev/null
gunzip -f risdump-ipv4.txt.gz

echo "Getting nro extended stats";
nro_ext_stats="https://www.nro.net/wp-content/uploads/apnic-uploads/delegated-extended"
curl $nro_ext_stats -o nro-ext-stats.txt 2>/dev/null

echo "Getting roas from validator 3";
all_roas="https://rpki-validator.prepdev.ripe.net/api/export.csv"
curl -XGET --header 'Accept: text/csv' $all_roas -o all-roas.csv 2>/dev/null

echo "Getting certified resources from validator 3";
certified_resources="https://rpki-validator.prepdev.ripe.net/api/rpki-objects/certified.csv"
curl -X GET --header 'Accept: text/csv'  $certified_resources -o certified.csv 2>/dev/null

echo "Generating country adoption"
$rpki_stats_bin -b $todays_stat_dir/risdump-ipv4.txt -s $todays_stat_dir/nro-ext-stats.txt -r $todays_stat_dir/all-roas.csv -f $todays_stat_dir/certified.csv --country-adoption | column -s',' -t  > $todays_stat_dir/country-adoption.txt  2>/dev/null
echo "Generating country activation"
$rpki_stats_bin -b $todays_stat_dir/risdump-ipv4.txt -s $todays_stat_dir/nro-ext-stats.txt -r $todays_stat_dir/all-roas.csv -f $todays_stat_dir/certified.csv --country-activation | column -s',' -t > $todays_stat_dir/country-activation.txt  2>/dev/null
echo "Generating rir adoption"
$rpki_stats_bin -b $todays_stat_dir/risdump-ipv4.txt -s $todays_stat_dir/nro-ext-stats.txt -r $todays_stat_dir/all-roas.csv -f $todays_stat_dir/certified.csv --rir-adoption | column -s',' -t  > $todays_stat_dir/rir-adoption.txt  2>/dev/null
echo "Generating rir activation"
$rpki_stats_bin -b $todays_stat_dir/risdump-ipv4.txt -s $todays_stat_dir/nro-ext-stats.txt -r $todays_stat_dir/all-roas.csv -f $todays_stat_dir/certified.csv --rir-activation | column -s',' -t > $todays_stat_dir/rir-activation.txt  2>/dev/null
echo "Generating web page"
$rpki_stats_bin -b $todays_stat_dir/risdump-ipv4.txt -s $todays_stat_dir/nro-ext-stats.txt -r $todays_stat_dir/all-roas.csv -f $todays_stat_dir/certified.csv -n > $todays_stat_dir/nro-stats.html 2>/dev/null

# Export rsync password in RSYNC_PASSWORD
rsync_target="rpki@dragonstone.ripe.net::rpki"

function rsync_if_not_empty(){
    if [ -s $todays_stat_dir/$1 ] 
    then
        rsync $todays_stat_dir/$1 $rsync_target
        echo "Successfully rsynced $1"
    else
        echo "Failed to create $1, not rsyincing to NRO"
    fi
}

rsync_if_not_empty country-adoption.txt 
rsync_if_not_empty country-activation.txt 
rsync_if_not_empty rir-adoption.txt 
rsync_if_not_empty rir-activation.txt 
rsync_if_not_empty nro-stats.html 
