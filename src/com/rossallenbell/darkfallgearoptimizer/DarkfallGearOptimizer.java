
package com.rossallenbell.darkfallgearoptimizer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.rossallenbell.darkfallgearoptimizer.data.CsvArmorProvider;

public class DarkfallGearOptimizer {

    public static enum PROTECTION {
        Bludgeoning, Piercing, Slashing, Acid, Cold, Fire, Holy, Lightning, Unholy, Impact
    }

    public static enum ARMOR_TYPE {
        Cloth, Padded, Leather, Studded, Bone, Chain, Banded, Scale, Plate, FullPlate, Infernal, Dragon, NoArmor
    }

    public static enum ARMOR_SLOT {
        Chest, Head, Legs, Boots, Gauntlets, Arms, Elbows, Shoulders, Greaves, Girdle
    }

    public static enum AVAILABILITY_TIER {
        Common,               // Exclude FullPlate, Infernal, Dragon
        CommonFullPlate,      // Exclude Infernal, Dragon
        CommonFullPlateInfernal, // Exclude Dragon
        All                   // Everything
    }

    public static final DecimalFormat formatter = new DecimalFormat("00.00");

    public static Map<PROTECTION, Double> protectionWeights = new HashMap<PROTECTION, Double>();
    public static Map<PROTECTION, Double> protectionMaxValues = new HashMap<PROTECTION, Double>();

    public static void main(String[] args) throws FileNotFoundException, IOException {
        // Check for help flag first
        for (String arg : args) {
            if (arg.equals("-h") || arg.equals("--help")) {
                printHelp();
                return;
            }
        }

        // Parse arguments
        String filePath = "./data/default_set.csv";
        boolean useLegacy = false;
        int threads = 1;
        String outputFormat = "text";
        boolean generatePresets = false;
        AVAILABILITY_TIER tier = AVAILABILITY_TIER.All;

        // Default protection weights (Slashing + Fire)
        protectionWeights.put(PROTECTION.Slashing, 1.0);
        protectionWeights.put(PROTECTION.Fire, 1.0);

        boolean hasCustomWeights = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--use-legacy")) {
                useLegacy = true;
            } else if (arg.equals("--threads")) {
                if (i + 1 < args.length) {
                    try {
                        threads = Integer.parseInt(args[i + 1]);
                        i++; // Skip next arg
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid thread count: " + args[i + 1]);
                        System.exit(1);
                    }
                } else {
                    System.err.println("--threads requires a number argument");
                    System.exit(1);
                }
            } else if (arg.equals("--format")) {
                if (i + 1 < args.length) {
                    outputFormat = args[i + 1];
                    if (!outputFormat.equals("text") && !outputFormat.equals("json")) {
                        System.err.println("Invalid format: " + outputFormat + ". Must be 'text' or 'json'");
                        System.exit(1);
                    }
                    i++; // Skip next arg
                } else {
                    System.err.println("--format requires an argument (text or json)");
                    System.exit(1);
                }
            } else if (arg.equals("--weight")) {
                if (i + 1 < args.length) {
                    String weightSpec = args[i + 1];
                    if (!hasCustomWeights) {
                        // First weight argument - clear defaults
                        protectionWeights.clear();
                        hasCustomWeights = true;
                    }
                    parseWeightSpec(weightSpec);
                    i++; // Skip next arg
                } else {
                    System.err.println("--weight requires an argument (e.g., slashing=1.0)");
                    System.exit(1);
                }
            } else if (arg.equals("--tier")) {
                if (i + 1 < args.length) {
                    String tierArg = args[i + 1].toLowerCase();
                    switch (tierArg) {
                        case "common": tier = AVAILABILITY_TIER.Common; break;
                        case "fullplate": tier = AVAILABILITY_TIER.CommonFullPlate; break;
                        case "infernal": tier = AVAILABILITY_TIER.CommonFullPlateInfernal; break;
                        case "all": tier = AVAILABILITY_TIER.All; break;
                        default:
                            System.err.println("Invalid tier: " + args[i + 1] + ". Must be 'common', 'fullplate', 'infernal', or 'all'");
                            System.exit(1);
                    }
                    i++; // Skip next arg
                } else {
                    System.err.println("--tier requires an argument (common, fullplate, infernal, all)");
                    System.exit(1);
                }
            } else if (arg.equals("--generate-presets")) {
                generatePresets = true;
            } else if (!arg.startsWith("--")) {
                filePath = arg;
            }
        }

        if (generatePresets) {
            generateAllPresets(filePath, outputFormat, useLegacy, threads);
            return;
        }

        if (protectionWeights.isEmpty()) {
            System.err.println("Error: At least one protection weight must be specified");
            System.exit(1);
        }

        Set<Armor> armors = new CsvArmorProvider().readFilePath(filePath);
        armors = filterArmorsByTier(armors, tier);
        computeProtectionMaxValues(armors);
        System.out.println(String.format("Found %d pieces of armor at %s (tier: %s)", armors.size(), filePath, tierDisplayName(tier)));
        ArmorCombinator combinator = new ArmorCombinator(armors);
        Map<ARMOR_SLOT, List<Armor>> slotBuckets = combinator.getSlotBuckets();
        long totalPossibleNonUniqueArmorSets = 1;
        for(ARMOR_SLOT slot : ARMOR_SLOT.values()){
            int slotCount = slotBuckets.containsKey(slot)? slotBuckets.get(slot).size() : 0;
            System.out.println(String.format("%s: %d", slot, slotCount));
            if(slotCount != 0){
                totalPossibleNonUniqueArmorSets *= slotCount;
            }
        }
        System.out.println(String.format("Total possible armor set combinations: %d", totalPossibleNonUniqueArmorSets));

        Collection<ArmorSet> winningArmorSets;

        if (useLegacy) {
            // Legacy approach: generate all, then filter
            System.out.println("Using legacy mode (--use-legacy)");
            System.out.println("Producing all armor combinations...");
            Set<ArmorSet> armorSets = combinator.getArmorSets();
            System.out.println(String.format("Unique armor combinations found: %d", armorSets.size()));
            System.out.println("Ordering and filtering out non-ideal sets...");
            ArmorRanker ranker = new ArmorRanker(armorSets);
            winningArmorSets = ranker.getWinningSets();
        } else if (threads > 1) {
            // Optimized approach with parallelization (Phase 1-3)
            System.out.println(String.format("Using optimized mode with %d threads (Phase 1-3: incremental Pareto filtering + hash deduplication + parallelization)", threads));
            System.out.println("Producing Pareto-optimal armor combinations...");
            winningArmorSets = new ParallelArmorCombinator(armors, threads).getOptimalArmorSets();
        } else {
            // Optimized approach: filter during generation (Phase 1-2)
            System.out.println("Using optimized mode (Phase 1-2: incremental Pareto filtering + hash deduplication)");
            System.out.println("Producing Pareto-optimal armor combinations...");
            winningArmorSets = combinator.getOptimalArmorSets();
        }

        System.out.println(String.format("Ideal armor combinations found: %d", winningArmorSets.size()));
        writeOutResults(winningArmorSets, outputFormat, filePath, null, tier);
    }

    private static void generateAllPresets(String filePath, String outputFormat, boolean useLegacy, int threads) throws FileNotFoundException, IOException {
        System.out.println("Generating all preset protection weight combinations...");
        System.out.println();

        List<String> presetNames = new ArrayList<>();
        List<String> presetFileKeys = new ArrayList<>();
        List<Map<PROTECTION, Double>> presets = new ArrayList<>();

        // Physical
        presetNames.add("Physical");
        presetFileKeys.add("physical");
        Map<PROTECTION, Double> physical = new HashMap<>();
        physical.put(PROTECTION.Bludgeoning, 1.0);
        physical.put(PROTECTION.Piercing, 1.0);
        physical.put(PROTECTION.Slashing, 1.0);
        presets.add(physical);

        // Magic
        presetNames.add("Magic");
        presetFileKeys.add("magic");
        Map<PROTECTION, Double> magic = new HashMap<>();
        magic.put(PROTECTION.Acid, 1.0);
        magic.put(PROTECTION.Cold, 1.0);
        magic.put(PROTECTION.Fire, 1.0);
        magic.put(PROTECTION.Holy, 1.0);
        magic.put(PROTECTION.Lightning, 1.0);
        magic.put(PROTECTION.Unholy, 1.0);
        presets.add(magic);

        // Piercing
        presetNames.add("Piercing");
        presetFileKeys.add("piercing");
        Map<PROTECTION, Double> piercing = new HashMap<>();
        piercing.put(PROTECTION.Piercing, 1.0);
        presets.add(piercing);

        // Phys+Magic 50/50
        presetNames.add("Phys+Magic 50/50");
        presetFileKeys.add("physical50-magic50");
        Map<PROTECTION, Double> physMagic5050 = new HashMap<>();
        physMagic5050.put(PROTECTION.Bludgeoning, 1.0);
        physMagic5050.put(PROTECTION.Piercing, 1.0);
        physMagic5050.put(PROTECTION.Slashing, 1.0);
        physMagic5050.put(PROTECTION.Acid, 0.5);
        physMagic5050.put(PROTECTION.Cold, 0.5);
        physMagic5050.put(PROTECTION.Fire, 0.5);
        physMagic5050.put(PROTECTION.Holy, 0.5);
        physMagic5050.put(PROTECTION.Lightning, 0.5);
        physMagic5050.put(PROTECTION.Unholy, 0.5);
        presets.add(physMagic5050);

        // Phys+Magic 33/66
        presetNames.add("Phys+Magic 33/66");
        presetFileKeys.add("physical33-magic66");
        Map<PROTECTION, Double> physMagic3366 = new HashMap<>();
        physMagic3366.put(PROTECTION.Bludgeoning, 1.0);
        physMagic3366.put(PROTECTION.Piercing, 1.0);
        physMagic3366.put(PROTECTION.Slashing, 1.0);
        physMagic3366.put(PROTECTION.Acid, 1.0);
        physMagic3366.put(PROTECTION.Cold, 1.0);
        physMagic3366.put(PROTECTION.Fire, 1.0);
        physMagic3366.put(PROTECTION.Holy, 1.0);
        physMagic3366.put(PROTECTION.Lightning, 1.0);
        physMagic3366.put(PROTECTION.Unholy, 1.0);
        presets.add(physMagic3366);

        // Phys+Magic 66/33
        presetNames.add("Phys+Magic 66/33");
        presetFileKeys.add("physical66-magic33");
        Map<PROTECTION, Double> physMagic6633 = new HashMap<>();
        physMagic6633.put(PROTECTION.Bludgeoning, 1.0);
        physMagic6633.put(PROTECTION.Piercing, 1.0);
        physMagic6633.put(PROTECTION.Slashing, 1.0);
        physMagic6633.put(PROTECTION.Acid, 0.25);
        physMagic6633.put(PROTECTION.Cold, 0.25);
        physMagic6633.put(PROTECTION.Fire, 0.25);
        physMagic6633.put(PROTECTION.Holy, 0.25);
        physMagic6633.put(PROTECTION.Lightning, 0.25);
        physMagic6633.put(PROTECTION.Unholy, 0.25);
        presets.add(physMagic6633);

        AVAILABILITY_TIER[] tiers = AVAILABILITY_TIER.values();
        int totalRuns = tiers.length * presets.size();
        int currentRun = 0;

        for (AVAILABILITY_TIER tierValue : tiers) {
            for (int p = 0; p < presets.size(); p++) {
                currentRun++;
                String presetName = presetNames.get(p);
                String presetFileKey = presetFileKeys.get(p);
                Map<PROTECTION, Double> weights = presets.get(p);

                System.out.println(String.format("========== [%d/%d] Tier: %s | Preset: %s ==========", currentRun, totalRuns, tierDisplayName(tierValue), presetName));
                System.out.print("Protection weights: ");
                List<String> weightDescriptions = new ArrayList<>();
                for (Map.Entry<PROTECTION, Double> entry : weights.entrySet()) {
                    weightDescriptions.add(entry.getKey().name() + "=" + entry.getValue());
                }
                System.out.println(String.join(", ", weightDescriptions));
                System.out.println();

                runWithWeights(filePath, outputFormat, useLegacy, threads, weights, presetFileKey, tierValue);
                System.out.println();
            }
        }

        System.out.println(String.format("Successfully generated %d preset combinations!", totalRuns));
    }

    private static void runWithWeights(String filePath, String outputFormat, boolean useLegacy, int threads, Map<PROTECTION, Double> weights, String presetFileKey, AVAILABILITY_TIER tier) throws FileNotFoundException, IOException {
        // Set the protection weights
        protectionWeights.clear();
        protectionWeights.putAll(weights);

        // Load armor data
        Set<Armor> armors = new CsvArmorProvider().readFilePath(filePath);
        armors = filterArmorsByTier(armors, tier);
        computeProtectionMaxValues(armors);
        System.out.println(String.format("Found %d pieces of armor at %s (tier: %s)", armors.size(), filePath, tierDisplayName(tier)));

        ArmorCombinator combinator = new ArmorCombinator(armors);
        Map<ARMOR_SLOT, List<Armor>> slotBuckets = combinator.getSlotBuckets();
        long totalPossibleNonUniqueArmorSets = 1;
        for(ARMOR_SLOT slot : ARMOR_SLOT.values()){
            int slotCount = slotBuckets.containsKey(slot)? slotBuckets.get(slot).size() : 0;
            if(slotCount != 0){
                totalPossibleNonUniqueArmorSets *= slotCount;
            }
        }
        System.out.println(String.format("Total possible armor set combinations: %d", totalPossibleNonUniqueArmorSets));

        Collection<ArmorSet> winningArmorSets;

        if (useLegacy) {
            System.out.println("Using legacy mode (--use-legacy)");
            System.out.println("Producing all armor combinations...");
            Set<ArmorSet> armorSets = combinator.getArmorSets();
            System.out.println(String.format("Unique armor combinations found: %d", armorSets.size()));
            System.out.println("Ordering and filtering out non-ideal sets...");
            ArmorRanker ranker = new ArmorRanker(armorSets);
            winningArmorSets = ranker.getWinningSets();
        } else if (threads > 1) {
            System.out.println(String.format("Using optimized mode with %d threads (Phase 1-3: incremental Pareto filtering + hash deduplication + parallelization)", threads));
            System.out.println("Producing Pareto-optimal armor combinations...");
            winningArmorSets = new ParallelArmorCombinator(armors, threads).getOptimalArmorSets();
        } else {
            System.out.println("Using optimized mode (Phase 1-2: incremental Pareto filtering + hash deduplication)");
            System.out.println("Producing Pareto-optimal armor combinations...");
            winningArmorSets = combinator.getOptimalArmorSets();
        }

        System.out.println(String.format("Ideal armor combinations found: %d", winningArmorSets.size()));
        writeOutResults(winningArmorSets, outputFormat, filePath, presetFileKey, tier);
    }

    private static void printHelp() {
        System.out.println("Darkfall Gear Optimizer");
        System.out.println();
        System.out.println("USAGE:");
        System.out.println("  java -cp \"bin:lib/*\" com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer [dataset] [options]");
        System.out.println("  ./run.sh [dataset] [options]");
        System.out.println();
        System.out.println("OPTIONS:");
        System.out.println("  -h, --help              Show this help message");
        System.out.println("  --format <text|json>    Output format (default: text)");
        System.out.println("  --weight <type>=<val>   Set protection weight (e.g., slashing=1.0)");
        System.out.println("                          Can be specified multiple times");
        System.out.println("  --tier <tier>           Armor availability tier (default: all)");
        System.out.println("                          Values: common, fullplate, infernal, all");
        System.out.println("  --threads <num>         Number of threads for parallel processing (default: 1)");
        System.out.println("  --generate-presets      Generate all 24 preset combinations (6 presets x 4 tiers)");
        System.out.println("  --use-legacy            Use legacy algorithm (slower, for comparison)");
        System.out.println();
        System.out.println("AVAILABILITY TIERS:");
        System.out.println("  common     - Excludes FullPlate, Infernal, and Dragon armor");
        System.out.println("  fullplate  - Excludes Infernal and Dragon armor");
        System.out.println("  infernal   - Excludes Dragon armor");
        System.out.println("  all        - All armor types included");
        System.out.println();
        System.out.println("PROTECTION TYPES:");
        System.out.println("  bludgeoning, piercing, slashing, acid, cold, fire, holy, lightning, unholy, impact");
        System.out.println();
        System.out.println("EXAMPLES:");
        System.out.println("  # Generate all 24 presets (6 weight presets x 4 tiers)");
        System.out.println("  ./run.sh ./data/armor-data-complete.csv --format json --threads 4 --generate-presets");
        System.out.println();
        System.out.println("  # Single run with tier filter");
        System.out.println("  ./run.sh ./data/armor-data-complete.csv --format json --weight slashing=1.0 --tier common");
        System.out.println();
        System.out.println("  # Multiple protection types");
        System.out.println("  ./run.sh ./data/armor-data-complete.csv --format json --weight slashing=1.0 --weight fire=1.0");
        System.out.println();
        System.out.println("NOTES:");
        System.out.println("  - Weights are relative to each other (slashing=1.0, fire=1.0 is same as slashing=2.0, fire=2.0)");
        System.out.println("  - Default weights if none specified: slashing=1.0, fire=1.0");
        System.out.println("  - Output files named: results-[dataset]-[tier]-[weights].[json|txt]");
        System.out.println();
        System.out.println("DATASETS:");
        System.out.println("  ./data/armor-data-minimal.csv   - Small dataset for testing");
        System.out.println("  ./data/armor-data-common.csv    - Medium dataset");
        System.out.println("  ./data/armor-data-complete.csv  - Full dataset (recommended for production)");
    }

    private static Set<Armor> filterArmorsByTier(Set<Armor> armors, AVAILABILITY_TIER tier) {
        Set<ARMOR_TYPE> excludedTypes = new java.util.HashSet<>();
        switch (tier) {
            case Common:
                excludedTypes.add(ARMOR_TYPE.FullPlate);
                excludedTypes.add(ARMOR_TYPE.Infernal);
                excludedTypes.add(ARMOR_TYPE.Dragon);
                break;
            case CommonFullPlate:
                excludedTypes.add(ARMOR_TYPE.Infernal);
                excludedTypes.add(ARMOR_TYPE.Dragon);
                break;
            case CommonFullPlateInfernal:
                excludedTypes.add(ARMOR_TYPE.Dragon);
                break;
            case All:
                break;
        }
        Set<Armor> filtered = new java.util.HashSet<>();
        for (Armor armor : armors) {
            if (armor.type == ARMOR_TYPE.NoArmor || !excludedTypes.contains(armor.type)) {
                filtered.add(armor);
            }
        }
        return filtered;
    }

    private static String tierDisplayName(AVAILABILITY_TIER tier) {
        switch (tier) {
            case Common: return "common";
            case CommonFullPlate: return "common+fp";
            case CommonFullPlateInfernal: return "common+fp+inf";
            case All: return "all";
            default: return tier.name().toLowerCase();
        }
    }

    private static void computeProtectionMaxValues(Set<Armor> armors) {
        protectionMaxValues.clear();
        for (Armor armor : armors) {
            if (armor.type == ARMOR_TYPE.NoArmor) continue;
            for (PROTECTION protection : PROTECTION.values()) {
                double value = armor.getResistance(protection);
                Double currentMax = protectionMaxValues.get(protection);
                if (currentMax == null || value > currentMax) {
                    protectionMaxValues.put(protection, value);
                }
            }
        }
    }

    private static void parseWeightSpec(String weightSpec) {
        String[] parts = weightSpec.split("=");
        if (parts.length != 2) {
            System.err.println("Invalid weight specification: " + weightSpec);
            System.err.println("Format should be: protectionType=value (e.g., slashing=1.0)");
            System.exit(1);
        }

        String protectionName = parts[0].trim();
        PROTECTION protection;
        try {
            // Capitalize first letter to match enum
            String capitalizedName = protectionName.substring(0, 1).toUpperCase() + protectionName.substring(1).toLowerCase();
            protection = PROTECTION.valueOf(capitalizedName);
        } catch (IllegalArgumentException e) {
            System.err.println("Unknown protection type: " + protectionName);
            System.err.println("Valid types: " + java.util.Arrays.toString(PROTECTION.values()));
            System.exit(1);
            return;
        }

        double value;
        try {
            value = Double.parseDouble(parts[1].trim());
        } catch (NumberFormatException e) {
            System.err.println("Invalid weight value: " + parts[1]);
            System.exit(1);
            return;
        }

        if (value < 0) {
            System.err.println("Weight values must be non-negative: " + value);
            System.exit(1);
        }

        protectionWeights.put(protection, value);
    }

    private static String extractDatasetName(String filePath) {
        String fileName = new File(filePath).getName().toLowerCase();
        if (fileName.contains("minimal")) {
            return "minimal";
        } else if (fileName.contains("common")) {
            return "common";
        } else if (fileName.contains("complete")) {
            return "complete";
        }
        return "unknown";
    }

    private static String generateWeightIndicator() {
        List<String> parts = new ArrayList<String>();
        for (Map.Entry<PROTECTION, Double> entry : protectionWeights.entrySet()) {
            if (entry.getValue() > 0) {
                String protectionName = entry.getKey().name().toLowerCase();
                int weightValue = (int) Math.round(entry.getValue() * 100);
                parts.add(protectionName + weightValue);
            }
        }
        return String.join("-", parts);
    }

    private static void writeOutResults(Collection<ArmorSet> winningSets, String format, String inputFilePath, String presetFileKey, AVAILABILITY_TIER tier) throws IOException {
        String datasetName = extractDatasetName(inputFilePath);
        String tierName = tierDisplayName(tier);
        String weightPart = presetFileKey != null ? presetFileKey : generateWeightIndicator();
        String extension = format.equals("json") ? ".json" : ".txt";
        String filePath = "./results-" + datasetName + "-" + tierName + "-" + weightPart + extension;

        System.out.println("Writing to path: " + filePath);
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(filePath)));

        if (format.equals("json")) {
            writeJsonOutput(bufferedWriter, winningSets, datasetName, inputFilePath, tierName);
        } else {
            writeTextOutput(bufferedWriter, winningSets);
        }

        bufferedWriter.close();
    }

    private static void writeTextOutput(BufferedWriter writer, Collection<ArmorSet> winningSets) throws IOException {
        for (ArmorSet armorSet : winningSets) {
            writer.write(armorSet.toString());
        }
    }

    private static void writeJsonOutput(BufferedWriter writer, Collection<ArmorSet> winningSets, String datasetName, String inputFilePath, String tierName) throws IOException {
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String timestamp = isoFormat.format(new Date());

        writer.write("{\n");
        writer.write("  \"metadata\": {\n");
        writer.write("    \"dataset\": \"" + datasetName + "\",\n");
        writer.write("    \"inputFile\": \"" + inputFilePath + "\",\n");
        writer.write("    \"timestamp\": \"" + timestamp + "\",\n");
        writer.write("    \"availabilityTier\": \"" + tierName + "\",\n");
        writer.write("    \"protectionWeights\": {\n");

        int weightCount = 0;
        int totalWeights = protectionWeights.size();
        for (Map.Entry<PROTECTION, Double> entry : protectionWeights.entrySet()) {
            weightCount++;
            writer.write("      \"" + entry.getKey().name() + "\": " + entry.getValue());
            if (weightCount < totalWeights) {
                writer.write(",");
            }
            writer.write("\n");
        }

        writer.write("    }\n");
        writer.write("  },\n");
        writer.write("  \"results\": [\n");

        int rank = 0;
        int totalSets = winningSets.size();
        for (ArmorSet armorSet : winningSets) {
            rank++;
            writer.write("    {\n");
            writer.write("      \"rank\": " + rank + ",\n");
            writer.write("      \"totalProtection\": " + armorSet.getResistanceScore() + ",\n");
            writer.write("      \"encumbrance\": " + armorSet.getEncumbrance() + ",\n");
            writer.write("      \"gear\": {\n");

            Map<Armor.SlotAgnosticArmor, Integer> armorPieces = armorSet.getSAArmorWithCounts();
            int pieceCount = 0;
            for (Map.Entry<Armor.SlotAgnosticArmor, Integer> entry : armorPieces.entrySet()) {
                pieceCount++;
                Armor.SlotAgnosticArmor saArmor = entry.getKey();
                int count = entry.getValue();
                writer.write("        \"piece" + pieceCount + "\": {\n");
                writer.write("          \"description\": \"" + saArmor.toString() + "\",\n");
                writer.write("          \"count\": " + count + ",\n");
                writer.write("          \"encumbrance\": " + saArmor.getEncumbrance() + ",\n");
                writer.write("          \"protection\": " + saArmor.getResistanceScore() + "\n");
                writer.write("        }");
                if (pieceCount < armorPieces.size()) {
                    writer.write(",");
                }
                writer.write("\n");
            }

            writer.write("      }\n");
            writer.write("    }");
            if (rank < totalSets) {
                writer.write(",");
            }
            writer.write("\n");
        }

        writer.write("  ]\n");
        writer.write("}\n");
    }

}
