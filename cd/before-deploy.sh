#!/usr/bin/env bash
mv ~/.gnupg ~/dot.gnupg  
openssl aes-256-cbc -K $encrypted_5b0511f84952_key -iv $encrypted_5b0511f84952_iv -in cd/codesigning.asc.enc -out cd/codesigning.asc -d
gpg --fast-import cd/codesigning.asc
