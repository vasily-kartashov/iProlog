#!/usr/bin/env sh

for f in src/main/resources/prolog/*.pl ; do
  key="${f%.*}"
  echo "Processing $key"
  swipl -f scripts/pl2nl.pl -g "pl('$key'),halt"
done
