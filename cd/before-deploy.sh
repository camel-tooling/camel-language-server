#!/usr/bin/env bash
export DEC_TOKEN=$1
mv ~/.gnupg ~/dot.gnupg  

openssl aes-256-cbc -K $encrypted_5b0511f84952_key -iv $encrypted_5b0511f84952_iv -in cd/codesigning.asc.enc -out cd/codesigning.asc -d
gpg --fast-import cd/codesigning.asc


#openssl enc -aes-256-cbc -d -in $GPG_DIR/pubring.gpg.enc -out $GPG_DIR/pubring.gpg -k $DEC_TOKEN
#openssl enc -aes-256-cbc -d -in $GPG_DIR/secring.gpg.enc -out $GPG_DIR/secring.gpg -k $DEC_TOKEN
#gpg --import $GPG_DIR/pubring.gpg
#gpg --import $GPG_DIR/secring.gpg
