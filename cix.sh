#!/bin/bash
set -euo pipefail
echo "Running $TEST with SQ=$SQ_VERSION"

case "$TEST" in
  plugin)  
    
  cd its/$TEST
  mvn -Pit-$TEST -DjavascriptVersion="LATEST_RELEASE" -DjavaVersion="LATEST_RELEASE" -Dsonar.runtimeVersion="$SQ_VERSION" -Dmaven.test.redirectTestOutputToFile=false test
  ;;

  *)
  echo "Unexpected TEST mode: $TEST"
  exit 1
  ;;
esac

