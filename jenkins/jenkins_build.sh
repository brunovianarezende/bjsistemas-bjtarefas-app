#!/bin/sh

KEYSTORE=$1
STOREPASS=$2
KEYPASS=$3
BASEPATH=$4
INPUT=$BASEPATH/app-release-unsigned.apk
OUTPUT=$BASEPATH/app-release-signed.apk

apksigner sign --ks $KEYSTORE --ks-pass $STOREPASS -ks-key-alias tasksapp --key-pass $KEYPASS --out $OUTPUT $INPUT
