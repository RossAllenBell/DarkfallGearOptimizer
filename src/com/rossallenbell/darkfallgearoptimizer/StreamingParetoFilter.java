package com.rossallenbell.darkfallgearoptimizer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Phase 2 optimization: Hash-based deduplication instead of Map storage.
 * Stores integer hash codes instead of full Map objects, reducing memory usage by ~90%.
 *
 * Supports a two-phase API for hot loops: check() tests dedup + Pareto dominance
 * using only primitive values, and addWinner() inserts the ArmorSet only when needed.
 * This avoids allocating ArmorSet objects for the 99%+ of combinations that are
 * duplicates or dominated.
 */
public class StreamingParetoFilter {

    // For deduplication: store hash codes instead of full Map objects
    private final Set<Integer> seenHashes;

    // For Pareto filtering: maintain current frontier (encumbrance -> best ArmorSet)
    private final SortedMap<Double, ArmorSet> paretoFrontier;

    // Track hash collisions for monitoring
    private int hashCollisions;

    public StreamingParetoFilter() {
        this.seenHashes = new HashSet<>();
        this.paretoFrontier = new TreeMap<>();
        this.hashCollisions = 0;
    }

    /**
     * Phase 1 of two-phase API: checks dedup hash and Pareto dominance using only
     * primitive values. Returns true if the combination should be added (caller
     * should then build the ArmorSet and call addWinner).
     */
    public boolean check(int hash, double encumbrance, double resistanceScore) {
        // Dedup check
        if (seenHashes.contains(hash)) {
            if (paretoFrontier.containsKey(encumbrance)) {
                ArmorSet existing = paretoFrontier.get(encumbrance);
                if (Math.abs(existing.getResistanceScore() - resistanceScore) < 0.0001) {
                    return false; // Confirmed duplicate
                }
                hashCollisions++;
            } else {
                hashCollisions++;
            }
        }

        seenHashes.add(hash);

        // Pareto dominance check
        if (paretoFrontier.containsKey(encumbrance)) {
            return paretoFrontier.get(encumbrance).getResistanceScore() < resistanceScore;
        } else {
            SortedMap<Double, ArmorSet> lighterSets = paretoFrontier.headMap(encumbrance);
            return lighterSets.isEmpty() || lighterSets.get(lighterSets.lastKey()).getResistanceScore() < resistanceScore;
        }
    }

    /**
     * Phase 2 of two-phase API: adds a winning ArmorSet to the frontier and
     * removes dominated entries. Only call after check() returns true.
     */
    public void addWinner(double encumbrance, double resistanceScore, ArmorSet armorSet) {
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
    }

    /**
     * Convenience method combining check + addWinner. Used by merge step and
     * backward-compatible callers.
     */
    public boolean tryAdd(ArmorSet armorSet) {
        int hash = armorSet.getSAArmorWithCounts().hashCode();
        double encumbrance = armorSet.getEncumbrance();
        double resistanceScore = armorSet.getResistanceScore();
        if (check(hash, encumbrance, resistanceScore)) {
            addWinner(encumbrance, resistanceScore, armorSet);
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
     * Returns the number of unique hashes seen (approximate unique combinations).
     */
    public int getSeenCount() {
        return seenHashes.size();
    }

    /**
     * Returns the number of sets on the Pareto frontier.
     */
    public int getFrontierCount() {
        return paretoFrontier.size();
    }

    /**
     * Returns the number of hash collisions detected.
     */
    public int getHashCollisions() {
        return hashCollisions;
    }

    /**
     * Returns the hash collision rate as a percentage.
     */
    public double getCollisionRate() {
        if (seenHashes.isEmpty()) {
            return 0.0;
        }
        return (double) hashCollisions / seenHashes.size() * 100.0;
    }
}
