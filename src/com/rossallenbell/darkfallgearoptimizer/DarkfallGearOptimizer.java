
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

    public static final DecimalFormat formatter = new DecimalFormat("00.00");

    public static Map<PROTECTION, Double> protectionWeights = new HashMap<PROTECTION, Double>();

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
        System.out.println(String.format("Found %d pieces of armor at %s", armors.size(), filePath));
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
        writeOutResults(winningArmorSets, outputFormat, filePath, null);
    }

    private static void generateAllPresets(String filePath, String outputFormat, boolean useLegacy, int threads) throws FileNotFoundException, IOException {
        System.out.println("Generating all preset protection weight combinations...");
        System.out.println();

        // Generate single timestamp for entire batch
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String batchTimestamp = dateFormat.format(new Date());

        List<Map<PROTECTION, Double>> presets = new ArrayList<>();

        // Individual protection types (10 presets)
        for (PROTECTION protection : PROTECTION.values()) {
            Map<PROTECTION, Double> weights = new HashMap<>();
            weights.put(protection, 1.0);
            presets.add(weights);
        }

        // All protections
        Map<PROTECTION, Double> allProtections = new HashMap<>();
        for (PROTECTION protection : PROTECTION.values()) {
            allProtections.put(protection, 1.0);
        }
        presets.add(allProtections);

        // Physical protections (Bludgeoning, Piercing, Slashing)
        Map<PROTECTION, Double> physical = new HashMap<>();
        physical.put(PROTECTION.Bludgeoning, 1.0);
        physical.put(PROTECTION.Piercing, 1.0);
        physical.put(PROTECTION.Slashing, 1.0);
        presets.add(physical);

        // Magic protections (Acid, Cold, Fire, Holy, Lightning, Unholy, Impact)
        Map<PROTECTION, Double> magic = new HashMap<>();
        magic.put(PROTECTION.Acid, 1.0);
        magic.put(PROTECTION.Cold, 1.0);
        magic.put(PROTECTION.Fire, 1.0);
        magic.put(PROTECTION.Holy, 1.0);
        magic.put(PROTECTION.Lightning, 1.0);
        magic.put(PROTECTION.Unholy, 1.0);
        magic.put(PROTECTION.Impact, 1.0);
        presets.add(magic);

        // Slashing + Fire (equal)
        Map<PROTECTION, Double> slashingFire = new HashMap<>();
        slashingFire.put(PROTECTION.Slashing, 1.0);
        slashingFire.put(PROTECTION.Fire, 1.0);
        presets.add(slashingFire);

        // Slashing-heavy + Fire
        Map<PROTECTION, Double> slashingHeavy = new HashMap<>();
        slashingHeavy.put(PROTECTION.Slashing, 1.0);
        slashingHeavy.put(PROTECTION.Fire, 0.33);
        presets.add(slashingHeavy);

        // Fire-heavy + Slashing
        Map<PROTECTION, Double> fireHeavy = new HashMap<>();
        fireHeavy.put(PROTECTION.Slashing, 0.33);
        fireHeavy.put(PROTECTION.Fire, 1.0);
        presets.add(fireHeavy);

        int totalPresets = presets.size();
        int currentPreset = 0;

        for (Map<PROTECTION, Double> weights : presets) {
            currentPreset++;
            System.out.println(String.format("========== Preset %d/%d ==========", currentPreset, totalPresets));
            System.out.print("Protection weights: ");
            List<String> weightDescriptions = new ArrayList<>();
            for (Map.Entry<PROTECTION, Double> entry : weights.entrySet()) {
                weightDescriptions.add(entry.getKey().name() + "=" + entry.getValue());
            }
            System.out.println(String.join(", ", weightDescriptions));
            System.out.println();

            runWithWeights(filePath, outputFormat, useLegacy, threads, weights, batchTimestamp);
            System.out.println();
        }

        System.out.println(String.format("Successfully generated %d preset combinations!", totalPresets));
    }

    private static void runWithWeights(String filePath, String outputFormat, boolean useLegacy, int threads, Map<PROTECTION, Double> weights, String timestamp) throws FileNotFoundException, IOException {
        // Set the protection weights
        protectionWeights.clear();
        protectionWeights.putAll(weights);

        // Load armor data
        Set<Armor> armors = new CsvArmorProvider().readFilePath(filePath);
        System.out.println(String.format("Found %d pieces of armor at %s", armors.size(), filePath));

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
        writeOutResults(winningArmorSets, outputFormat, filePath, timestamp);
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
        System.out.println("  --threads <num>         Number of threads for parallel processing (default: 1)");
        System.out.println("  --generate-presets      Generate all 16 preset combinations");
        System.out.println("  --use-legacy            Use legacy algorithm (slower, for comparison)");
        System.out.println();
        System.out.println("PROTECTION TYPES:");
        System.out.println("  bludgeoning, piercing, slashing, acid, cold, fire, holy, lightning, unholy, impact");
        System.out.println();
        System.out.println("EXAMPLES:");
        System.out.println("  # Generate all presets (recommended for web apps)");
        System.out.println("  ./run.sh ./data/armor-data-complete.csv --format json --threads 4 --generate-presets");
        System.out.println();
        System.out.println("  # Single protection type");
        System.out.println("  ./run.sh ./data/armor-data-complete.csv --format json --weight slashing=1.0");
        System.out.println();
        System.out.println("  # Multiple protection types (50/50 slashing and fire)");
        System.out.println("  ./run.sh ./data/armor-data-complete.csv --format json --weight slashing=1.0 --weight fire=1.0");
        System.out.println();
        System.out.println("  # Weighted combination (75% slashing, 25% fire)");
        System.out.println("  ./run.sh ./data/armor-data-complete.csv --format json --weight slashing=1.0 --weight fire=0.33");
        System.out.println();
        System.out.println("NOTES:");
        System.out.println("  - Weights are relative to each other (slashing=1.0, fire=1.0 is same as slashing=2.0, fire=2.0)");
        System.out.println("  - Default weights if none specified: slashing=1.0, fire=1.0");
        System.out.println("  - Output files named: results-YYYY-MM-DD-HH-mm-ss-[dataset]-[weights].[json|txt]");
        System.out.println("  - With --generate-presets, all 16 files share the same timestamp");
        System.out.println();
        System.out.println("DATASETS:");
        System.out.println("  ./data/armor-data-minimal.csv   - Small dataset for testing");
        System.out.println("  ./data/armor-data-common.csv    - Medium dataset");
        System.out.println("  ./data/armor-data-complete.csv  - Full dataset (recommended for production)");
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

    private static void writeOutResults(Collection<ArmorSet> winningSets, String format, String inputFilePath, String timestamp) throws IOException {
        // If timestamp not provided, generate a new one
        if (timestamp == null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
            timestamp = dateFormat.format(new Date());
        }
        String datasetName = extractDatasetName(inputFilePath);
        String weightIndicator = generateWeightIndicator();
        String extension = format.equals("json") ? ".json" : ".txt";
        String filePath = "./results-" + timestamp + "-" + datasetName + "-" + weightIndicator + extension;

        System.out.println("Writing to path: " + filePath);
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(filePath)));

        if (format.equals("json")) {
            writeJsonOutput(bufferedWriter, winningSets, datasetName, inputFilePath);
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

    private static void writeJsonOutput(BufferedWriter writer, Collection<ArmorSet> winningSets, String datasetName, String inputFilePath) throws IOException {
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String timestamp = isoFormat.format(new Date());

        writer.write("{\n");
        writer.write("  \"metadata\": {\n");
        writer.write("    \"dataset\": \"" + datasetName + "\",\n");
        writer.write("    \"inputFile\": \"" + inputFilePath + "\",\n");
        writer.write("    \"timestamp\": \"" + timestamp + "\",\n");
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
