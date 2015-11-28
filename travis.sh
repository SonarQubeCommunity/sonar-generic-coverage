#!/bin/bash

set -euo pipefail

function installTravisTools {
  mkdir ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v21 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}

function strongEcho {
  echo ""
  echo "================ $1 ================="
}

case "$TARGET" in

CI)
  if [ "$TRAVIS_BRANCH" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    strongEcho 'Build and analyze commit in master'
    # this commit is master must be built and analyzed (with upload of report)
    mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent verify -Pcoverage-per-test -B -e -V

    # Switch to java 8 as the Dory HTTPS certificate is not supported by Java 7
    export JAVA_HOME=/usr/lib/jvm/java-8-oracle
    export PATH=$JAVA_HOME/bin:$PATH

    mvn sonar:sonar -B -e -V \
       -Dsonar.host.url=$SONAR_HOST_URL \
       -Dsonar.login=$SONAR_LOGIN \
       -Dsonar.password=$SONAR_PASSWORD


  elif [ "$TRAVIS_PULL_REQUEST" != "false" ] && [ -n "$SONAR_GITHUB_OAUTH" ]; then
    # For security reasons environment variables are not available on the pull requests
    # coming from outside repositories
    # http://docs.travis-ci.com/user/pull-requests/#Security-Restrictions-when-testing-Pull-Requests
    # That's why the analysis does not need to be executed if the variable SONAR_GITHUB_OAUTH is not defined.

    strongEcho 'Build and analyze pull request'
    # this pull request must be built and analyzed (without upload of report)
    mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent verify -Pcoverage-per-test -B -e -V

    # Switch to java 8 as the Dory HTTPS certificate is not supported by Java 7
    export JAVA_HOME=/usr/lib/jvm/java-8-oracle
    export PATH=$JAVA_HOME/bin:$PATH

    # integration of jacoco report is quite memory-consuming
    export MAVEN_OPTS="-Xmx1G -Xms128m"
    mvn sonar:sonar -B -e -V \
        -Dsonar.analysis.mode=issues \
        -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST \
        -Dsonar.github.repository=$TRAVIS_REPO_SLUG \
        -Dsonar.github.oauth=$SONAR_GITHUB_OAUTH \
        -Dsonar.host.url=$SONAR_HOST_URL \
        -Dsonar.login=$SONAR_LOGIN \
        -Dsonar.password=$SONAR_PASSWORD


  else
    strongEcho 'Build, no analysis'
    # Build branch, without any analysis

    # No need for Maven goal "install" as the generated JAR file does not need to be installed
    # in Maven local repository
    mvn verify -B -e -V
  fi
  ;;

IT)
  installTravisTools
  start_xvfb

  mvn package -Dsource.skip=true -Denforcer.skip=true -Danimal.sniffer.skip=true -Dmaven.test.skip=true

  if [ "${SQ_VERSION}" == "DEV" ];
  then
    build_snapshot "SonarSource/sonarqube"
  fi

  cd its/plugin
  mvn -DjavascriptVersion="LATEST_RELEASE" -DjavaVersion="LATEST_RELEASE" -Dsonar.runtimeVersion="$SQ_VERSION" -Dmaven.test.redirectTestOutputToFile=false test
  ;;

*)
  echo "Unexpected TARGET value: $TARGET"
  exit 1
  ;;

esac
