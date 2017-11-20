a1_path=real
a2_path=real
a3_path=real
a4_path=norm
a5_path=norm


a1=$(grep "DOWNLOADED" $a1_path/logs/ic.log | tail -n 1  | cut -f 8)
a2=$(grep "DOWNLOADED" $a2_path/logs/ic.log | tail -n 1  | cut -f 8)
a3=$(grep "DOWNLOADED" $a3_path/logs/ic.log | tail -n 1  | cut -f 8)
a4=$(grep "DOWNLOADED" $a4_path/logs/ic.log | tail -n 1  | cut -f 8)
a5=$(grep "DOWNLOADED" $a5_path/logs/ic.log | tail -n 1  | cut -f 8)

if test $a1 -gt $a2
then
        min=$a2;
else
        min=$a1;
fi


if test $min -gt $a3
then
        min=$a3;
fi


if test $min -gt $a4
then
        min=$a4;
fi

if test $min -gt $a5
then
        min=$a5;
fi


echo "Downloaded Page $min"

if [ -d $a1_path ]; then
        echo "$a1_path HV: $(grep "HV:\W$min\W" $a1_path/logs/ic.log* | cut -f 9)";
fi


if [ -d $a2_path ]; then
        echo "$a2_path HV: $(grep "HV:\W$min\W" $a2_path/logs/ic.log* | cut -f 9)";
fi

if [ -d $a3_path ]; then
        echo "$a3_path HV: $(grep "HV:\W$min\W" $a3_path/logs/ic.log* | cut -f 9)";
fi

if [ -d $a4_path ]; then
        echo "$a4_path HV: $(grep "HV:\W$min\W" $a4_path/logs/ic.log* | cut -f 9)";
fi

if [ -d $a5_path ]; then
        echo "$a5_path HV: $(grep "HV:\W$min\W" $a5_path/logs/ic.log* | cut -f 9)";
fi



