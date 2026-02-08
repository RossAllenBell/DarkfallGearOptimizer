# Usage Guide

Quick reference for common usage patterns.

## Running the Optimizer

Use the `run.sh` script to run the optimizer:

```bash
./run.sh [dataset] [options]
```

## Common Examples

### Generate all preset combinations (recommended for web app)
```bash
./run.sh ./data/armor-data-complete.csv --format json --threads 4 --generate-presets
```
This generates 16 JSON files with all common protection weight combinations, all with the same timestamp.

### Single protection type
```bash
# 100% Slashing protection
./run.sh ./data/armor-data-complete.csv --format json --weight slashing=1.0

# 100% Fire protection
./run.sh ./data/armor-data-complete.csv --format json --weight fire=1.0
```

### Multiple protection types
```bash
# 50/50 Slashing and Fire
./run.sh ./data/armor-data-complete.csv --format json --weight slashing=1.0 --weight fire=1.0

# 75% Slashing, 25% Fire
./run.sh ./data/armor-data-complete.csv --format json --weight slashing=1.0 --weight fire=0.33

# Equal weight for all physical protections
./run.sh ./data/armor-data-complete.csv --format json \
  --weight bludgeoning=1.0 \
  --weight piercing=1.0 \
  --weight slashing=1.0
```

### Output formats
```bash
# JSON format (for web apps)
./run.sh ./data/armor-data-complete.csv --format json --weight slashing=1.0

# Text format (human readable)
./run.sh ./data/armor-data-complete.csv --format text --weight slashing=1.0
```

### Using different datasets
```bash
# Minimal dataset (fastest, for testing)
./run.sh ./data/armor-data-minimal.csv --format json --weight slashing=1.0

# Common dataset (medium)
./run.sh ./data/armor-data-common.csv --format json --weight slashing=1.0

# Complete dataset (full data, slower)
./run.sh ./data/armor-data-complete.csv --format json --weight slashing=1.0
```

### Parallel processing
```bash
# Use 4 threads for faster processing
./run.sh ./data/armor-data-complete.csv --format json --threads 4 --weight slashing=1.0
```

## Available Protection Types

- `bludgeoning`
- `piercing`
- `slashing`
- `acid`
- `cold`
- `fire`
- `holy`
- `lightning`
- `unholy`
- `impact`

## Output Files

Generated files are named with the pattern:
```
results-YYYY-MM-DD-HH-mm-ss-[dataset]-[weights].json
```

Examples:
- `results-2026-02-06-09-10-39-complete-slashing100.json`
- `results-2026-02-06-09-10-39-complete-fire50-slashing100.json`

When using `--generate-presets`, all 16 files will have the same timestamp, making it easy to identify a batch.

## Protection Weight Notes

- Weights are **relative** to each other
- `[slashing=1.0, fire=1.0]` = 50/50 split
- `[slashing=1.0, fire=0.33]` = 75% slashing, 25% fire
- `[slashing=2.0, fire=2.0]` = same as `[slashing=1.0, fire=1.0]` (both 50/50)

## For More Details

See `PRESETS_GUIDE.md` for detailed information about the `--generate-presets` option and what preset combinations are generated.
