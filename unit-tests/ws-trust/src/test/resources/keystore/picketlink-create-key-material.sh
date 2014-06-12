#!/bin/bash

# set -x # echo on

PASS=keypass
VALIDITY=7300

function create_keystore
{
  ALIAS=$1
  KEY_FILE=$ALIAS.jks
  keytool -validity $VALIDITY -genkey -alias $ALIAS -keyalg RSA -keystore $KEY_FILE -storepass $PASS -keypass $PASS -dname cn=$ALIAS
  # export as PKCS12
  # keytool -importkeystore -srckeystore $KEY_FILE -destkeystore $ALIAS.p12 -deststoretype PKCS12 -srcstorepass $PASS -deststorepass $PASS
  # export certificate
  keytool -export -alias $ALIAS -keystore $KEY_FILE -storepass $PASS -file $ALIAS.crt
}

function import_cert
{
  KEY_FILE=$1.jks
  ALIAS=$2
  keytool -import -noprompt -alias $ALIAS -keystore $KEY_FILE -storepass $PASS -file $ALIAS.crt
}

create_keystore "sts"
create_keystore "service1"
create_keystore "service2"
create_keystore "localhost"

keytool -importkeystore -srckeystore sts.jks -destkeystore stspub.jks -srcstorepass $PASS -deststorepass $PASS

import_cert "stspub" "service1"
import_cert "stspub" "service2"

keytool -importkeystore -srckeystore stspub.jks -destkeystore sts_keystore.jks -srcstorepass $PASS -deststorepass testpass

import_cert "stspub" "localhost"
