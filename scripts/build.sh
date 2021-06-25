#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

for f in "$DIR/programs/"*.pl ; do
  key="${f%.*}"
  echo "Processing $key"
  swipl -f "$DIR/pl2nl.pl" -g "pl('$key'),halt"
  mv "$f.nl" "$DIR/../src/main/resources/prolog/"
done
