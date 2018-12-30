#!/bin/bash
find . -name '*.md' -type f -exec perl -pi -e '
  s/```tut:book/```scala mdoc/g;
  s/```tut/```scala mdoc/g;
' {} +
