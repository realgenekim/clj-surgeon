#!/bin/bash
# recurred iff any recorded instance exists after registration
n=$(grep -vc '^#' '/var/tmp/forge/encounter-fx/e1-recurrences.txt'); echo "envelope-writer-class recurrences=$n"; [ "$n" -eq 0 ]
