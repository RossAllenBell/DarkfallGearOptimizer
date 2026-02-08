# Generate Presets Guide

This guide explains how to use the `--generate-presets` option to generate all common protection weight combinations in a single command.

## Basic Usage

```bash
# Simple way (recommended)
./run.sh ./data/armor-data-minimal.csv --format json --generate-presets

# Or the full Java command
java -cp "bin:lib/*" com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer \
  ./data/armor-data-minimal.csv \
  --format json \
  --generate-presets
```

This single command will generate 16 different result files, each optimized for a different protection weight combination.

## Options

- **Dataset**: Specify the armor data file (minimal, common, or complete)
- **--format**: Choose output format (`text` or `json`)
- **--threads**: Number of threads for parallel processing (optional)
- **--generate-presets**: Triggers preset generation mode

## Generated Presets

The `--generate-presets` option generates 16 preset combinations:

### Individual Protection Types (10 files)
One file for each protection type, optimized 100% for that type:
- `results-{timestamp}-{dataset}-bludgeoning100.json`
- `results-{timestamp}-{dataset}-piercing100.json`
- `results-{timestamp}-{dataset}-slashing100.json`
- `results-{timestamp}-{dataset}-acid100.json`
- `results-{timestamp}-{dataset}-cold100.json`
- `results-{timestamp}-{dataset}-fire100.json`
- `results-{timestamp}-{dataset}-holy100.json`
- `results-{timestamp}-{dataset}-lightning100.json`
- `results-{timestamp}-{dataset}-unholy100.json`
- `results-{timestamp}-{dataset}-impact100.json`

### Combination Presets (6 files)

1. **All Protections** - Equal weight for all 10 protection types
   - File: `results-{timestamp}-{dataset}-bludgeoning100-piercing100-slashing100-acid100-cold100-fire100-holy100-lightning100-unholy100-impact100.json`

2. **Physical Protections** - Equal weight for Bludgeoning, Piercing, and Slashing
   - File: `results-{timestamp}-{dataset}-bludgeoning100-piercing100-slashing100.json`

3. **Magic Protections** - Equal weight for Acid, Cold, Fire, Holy, Lightning, Unholy, and Impact
   - File: `results-{timestamp}-{dataset}-acid100-cold100-fire100-holy100-lightning100-unholy100-impact100.json`

4. **Slashing + Fire (Equal)** - 50/50 split between Slashing and Fire
   - File: `results-{timestamp}-{dataset}-fire100-slashing100.json`

5. **Slashing-Heavy + Fire** - 75% Slashing, 25% Fire
   - File: `results-{timestamp}-{dataset}-fire33-slashing100.json`

6. **Fire-Heavy + Slashing** - 75% Fire, 25% Slashing
   - File: `results-{timestamp}-{dataset}-fire100-slashing33.json`

## Example: Generate All Presets with Parallel Processing

```bash
# Generate all presets for the complete dataset using 4 threads
./run.sh ./data/armor-data-complete.csv --format json --threads 4 --generate-presets
```

## Output

Each generated file contains:
- **Metadata**: Dataset name, timestamp, and protection weights used
- **Results**: Ranked list of Pareto-optimal armor sets with detailed gear breakdowns

## For Your Web App

Since you're building a static web app, you can:

1. Generate all presets for your desired dataset once:
   ```bash
   ./run.sh ./data/armor-data-complete.csv --format json --threads 4 --generate-presets
   ```

2. Host the JSON files as static assets

3. Let users select from a dropdown of preset combinations

4. Load the corresponding JSON file and display the results instantly

This approach eliminates the need for server-side computation and keeps your hosting costs minimal!

## Notes

- Protection weights are **relative** to each other. `[slashing=1.0, fire=1.0]` produces the same results as `[slashing=0.5, fire=0.5]` (both are 50/50)
- The file naming convention automatically includes the dataset name and weight indicators for easy identification
- Each preset is optimized independently, so you get the best armor sets for that specific protection priority
