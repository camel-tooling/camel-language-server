#!/usr/bin/env bash
if [ "$TRAVIS_BRANCH" = 'master' ] && [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
    print $SALT > cd/codesigning.asc
    gpg --fast-import cd/codesigning.asc
fi

