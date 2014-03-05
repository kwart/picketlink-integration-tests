#!/bin/bash

EAPZIP=$1
UNZIPDIST=$2

cd dist
unzip -q $EAPZIP
cd $UNZIPDIST/bin
./standalone.sh &
sleep 30
./jboss-cli.sh -c --file=../../../add-security-domains.cli
cd ../..
zip -u $EAPZIP $UNZIPDIST/standalone/configuration/standalone.xml

# clean up
rm -rf $UNZIPDIST
cd ..
