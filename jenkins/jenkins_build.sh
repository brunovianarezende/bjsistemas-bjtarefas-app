#!/bin/sh

KEYSTORE=$1
STOREPASS=$2
KEYPASS=$3
BASEPATH=$4
INPUT=$BASEPATH/app-release-unsigned.apk
ZIPALIGNED=$BASEPATH/app-release-zipaligned.apk
OUTPUT=$BASEPATH/app-release-signed.apk


zipalign 4 $INPUT $ZIPALIGNED
apksigner sign --ks $KEYSTORE --ks-pass $STOREPASS -ks-key-alias tasksapp --keypass $KEYPASS --out $OUTPUT
