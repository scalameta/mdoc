#!/usr/bin/env bash
set -eu

if [[ "$TRAVIS_SECURE_ENV_VARS" == true ]]; then
  echo "Publishing..."
  git log | head -n 20
  echo "$PGP_SECRET" | base64 --decode | gpg --import
  if [ -n "$TRAVIS_TAG" ]; then
    echo "Tag push, publishing stable release to Sonatype."
    sbt ci-release sonatypeReleaseAll
  else
    echo "Merge, publishing snapshot to Sonatype."
    sbt -Dvork.snapshot=true ci-release
  fi
else
  echo "Skipping publish, branch=$TRAVIS_BRANCH"
fi
