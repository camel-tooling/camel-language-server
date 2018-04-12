#!/usr/bin/env bash
export DEC_TOKEN=$1
openssl enc -aes-256-cbc -d -in $GPG_DIR/pubring.gpg.enc -out $GPG_DIR/pubring.gpg -k $DEC_TOKEN
openssl enc -aes-256-cbc -d -in $GPG_DIR/secring.gpg.enc -out $GPG_DIR/secring.gpg -k $DEC_TOKEN
