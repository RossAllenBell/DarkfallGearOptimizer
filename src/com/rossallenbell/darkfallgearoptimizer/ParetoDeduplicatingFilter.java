package com.rossallenbell.darkfallgearoptimizer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.rossallenbell.darkfallgearoptimizer.Armor.SlotAgnosticArmor;

/**
 * Combines deduplication and Pareto filtering into a single incremental filter.
 * Maintains both a HashSet for seen combinations and a TreeMap for the Pareto frontier.
 */
public class ParetoDeduplicatingFilter {

    // For deduplication: store seen slot-agnostic armor combinations
    private final Set<Map<SlotAgnosticArmor, Integer>> seenCombinations;

    // For Pareto filtering: maintain current frontier (encumbrance -> best ArmorSet)
    private final SortedMap<Double, ArmorSet> paretoFrontier;

    public ParetoDeduplicatingFilter() {
        this.seenCombinations = new HashSet<>();
        this.paretoFrontier = new TreeMap<>();
    }

    /**
     * Tries to add an ArmorSet to the filter.
     * Returns true if the set was added to the Pareto frontier, false otherwise.
     * A set is not added if:
     * - It's a duplicate (same slot-agnostic composition)
     * - It's dominated by an existing set in the frontier
     */
    public boolean tryAdd(ArmorSet armorSet) {
        Map<SlotAgnosticArmor, Integer> saArmors = armorSet.getSAArmorWithCounts();

        // Check for duplicate
        if (seenCombinations.contains(saArmors)) {
            return false;
        }

        // Mark as seen
        seenCombinations.add(saArmors);

        // Check if this set belongs on the Pareto frontier
        // This logic is identical to ArmorRanker.getWinningSets()
        double encumbrance = armorSet.getEncumbrance();
        double resistanceScore = armorSet.getResistanceScore();
        boolean winner = false;

        if (paretoFrontier.containsKey(encumbrance)) {
            // Same encumbrance - keep only if higher resistance
            if (paretoFrontier.get(encumbrance).getResistanceScore() < resistanceScore) {
                winner = true;
            }
        } else {
            // Different encumbrance - check if dominated by lighter sets
            SortedMap<Double, ArmorSet> lighterSets = paretoFrontier.headMap(encumbrance);
            if (lighterSets.isEmpty() || lighterSets.get(lighterSets.lastKey()).getResistanceScore() < resistanceScore) {
                winner = true;
            }
        }

        if (winner) {
            // Remove heavier sets that this set dominates
            SortedMap<Double, ArmorSet> heavierSets = paretoFrontier.tailMap(encumbrance);
            while (!heavierSets.isEmpty()) {
                double nextLightestEncumbrance = heavierSets.firstKey();
                if (heavierSets.get(nextLightestEncumbrance).getResistanceScore() <= resistanceScore) {
                    paretoFrontier.remove(nextLightestEncumbrance);
                } else {
                    break;
                }
                heavierSets = paretoFrontier.tailMap(encumbrance);
            }
            paretoFrontier.put(encumbrance, armorSet);
            return true;
        }

        return false;
    }

    /**
     * Returns the current Pareto frontier.
     */
    public Collection<ArmorSet> getWinningSets() {
        return paretoFrontier.values();
    }

    /**
     * Returns the number of unique combinations seen (for progress reporting).
     */
    public int getSeenCount() {
        return seenCombinations.size();
    }

    /**
     * Returns the number of sets on the Pareto frontier (for progress reporting).
     */
    public int getFrontierCount() {
        return paretoFrontier.size();
    }
}
