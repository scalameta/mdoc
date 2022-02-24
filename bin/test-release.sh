#!/usr/bin/env bash
set -eux

version=$1
argumentsRest=${@:2}
suffix=${argumentsRest:-}

coursier fetch \
  org.scalameta:mdoc_2.12:$version \
  org.scalameta:mdoc_2.13:$version \
  org.scalameta:mdoc_3:$version \
  org.scalameta:mdoc-js_2.12:$version \
  org.scalameta:mdoc-js_2.13:$version \
  org.scalameta:mdoc-js_3:$version \
  org.scalameta:mdoc-js-interfaces:$version \
  org.scalameta:mdoc-js-worker_2.12:$version \
  org.scalameta:mdoc-js-worker_2.13:$version $suffix

coursier fetch \
    "org.scalameta:sbt-mdoc;sbtVersion=1.0;scalaVersion=2.12:$version" \
    --sbt-plugin-hack $suffix
