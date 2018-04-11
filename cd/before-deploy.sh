#!/usr/bin/env bash
#if [ "$TRAVIS_BRANCH" = 'master' ] && [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
    openssl aes-256-cbc -K $PP -in cd/codesigning.asc.enc -out cd/codesigning.asc -d -v
    gpg --fast-import cd/codesigning.asc
#fi

