#!/usr/bin/env bash
mv ~/.gnupg ~/dot.gnupg  
openssl aes-256-cbc -K $encrypted_5b0511f84952_key -iv $encrypted_5b0511f84952_iv -in $GPG_DIR/codesigning.gpg.enc -out $GPG_DIR/codesigning.gpg -d
gpg --batch --import $GPG_DIR/codesigning.gpg
