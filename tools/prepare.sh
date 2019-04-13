#!/bin/bash

find . -depth 4 | sed 's/\(.*\)\/\(.*\)/\1\/\2 \1-\2/' | xargs -n2 mv
find . -depth 3 -type d | xargs -n1 rm -r
