#!/bin/bash
n=$(grep -vc '^#' '/var/tmp/forge/encounter-fx/e2-recurrences.txt'); echo "impact-oracle-one-hop-short recurrences=$n"; [ "$n" -eq 0 ]
