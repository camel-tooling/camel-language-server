#!/usr/bin/env bash
mvn deploy -DskipTests -P sign,build-extras --settings cd/mvnsettings.xml

