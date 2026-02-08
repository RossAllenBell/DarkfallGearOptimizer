#!/bin/bash

# Darkfall Gear Optimizer Runner
# Usage: ./run.sh [dataset] [options]
#
# Examples:
#   ./run.sh ./data/armor-data-minimal.csv --format json --weight slashing=1.0
#   ./run.sh ./data/armor-data-complete.csv --format json --threads 4 --generate-presets

java -cp "bin:lib/*" com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer "$@"
