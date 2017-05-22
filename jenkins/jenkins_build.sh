#!/bin/sh

KEYSTORE=$1
STOREPASS=$2
KEYPASS=$3
BASEPATH=$4
INPUT=$BASEPATH/app-release-unsigned.apk
OUTPUTDIR=$BASEPATH/outputdir
OUTPUT=$OUTPUTDIR/app-release-signed.apk

rm -rf $OUTPUTDIR
mkdir $OUTPUTDIR
zipalign 4 $INPUT $OUTPUT
apksigner sign --ks $KEYSTORE --ks-pass $STOREPASS -ks-key-alias tasksapp --key-pass $KEYPASS $OUTPUT
