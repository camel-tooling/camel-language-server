#!/usr/bin/env bash
#export DEC_TOKEN=$1
echo $GPG_DIR
openssl enc -aes-256-cbc -d -in cd/pubring.gpg.enc -out cd/pubring.gpg -k $DEC_TOKEN
openssl enc -aes-256-cbc -d -in cd/secring.gpg.enc -out cd/secring.gpg -k $DEC_TOKEN
#openssl aes-256-cbc -k $OPENSSLPASS -in $GPG_DIR/pubring.gpg.enc -out $GPG_DIR/pubring.gpg -d -v
#openssl aes-256-cbc -k $OPENSSLPASS -in $GPG_DIR/secring.gpg.enc -out $GPG_DIR/secring.gpg -d -v

