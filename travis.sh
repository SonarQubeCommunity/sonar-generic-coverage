#!/bin/bash

set -euo pipefail

function installTravisTools {
  mkdir ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v16 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}

case "$TESTS" in

ci)
  mvn verify -B -e -V
  ;;

its)
  installTravisTools

  mvn package -Dsource.skip=true -Denforcer.skip=true -Danimal.sniffer.skip=true -Dmaven.test.skip=true

  if [ "$SQ_VERSION" = "DEV" ] ; then
    build_snapshot "SonarSource/sonarqube"
  fi

  cd its/plugin
  mvn -DjavascriptVersion="LATEST_RELEASE" -DjavaVersion="LATEST_RELEASE" -Dsonar.runtimeVersion="$SQ_VERSION" -Dmaven.test.redirectTestOutputToFile=false test
  ;;

*)
  echo "Unexpected TESTS mode (ci, its expected): $TESTS"
  exit 1
  ;;

esac
