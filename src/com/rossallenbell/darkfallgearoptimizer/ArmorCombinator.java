package com.rossallenbell.darkfallgearoptimizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.ARMOR_SLOT;

public class ArmorCombinator {

    private final Collection<Armor> armors;

    public ArmorCombinator(Collection<Armor> armors){
        this.armors = armors;
    }

    public Map<ARMOR_SLOT, List<Armor>> getSlotBuckets(){
        Map<ARMOR_SLOT, List<Armor>> slotBuckets = new HashMap<DarkfallGearOptimizer.ARMOR_SLOT, List<Armor>>();
        for (Armor armor : armors) {
            if(!slotBuckets.containsKey(armor.slot)){
                slotBuckets.put(armor.slot, new ArrayList<Armor>());
            }
            slotBuckets.get(armor.slot).add(armor);
        }
        return slotBuckets;
    }

    public Set<ArmorSet> getArmorSets(){
        Set<ArmorSet> armorSets = new HashSet<ArmorSet>();

        Map<ARMOR_SLOT, List<Armor>> slotBuckets = getSlotBuckets();
        long totalPossibleNonUniqueArmorSets = 1;
        for(ARMOR_SLOT slot : ARMOR_SLOT.values()){
            int slotCount = slotBuckets.containsKey(slot)? slotBuckets.get(slot).size() : 0;
            if(slotCount != 0){
                totalPossibleNonUniqueArmorSets *= slotCount;
            }
        }
        List<ARMOR_SLOT> slots = new ArrayList<ARMOR_SLOT>(slotBuckets.keySet());
        Map<ARMOR_SLOT, Integer> slotPointers = new HashMap<ARMOR_SLOT, Integer>();
        for(ARMOR_SLOT slot : slots){
            slotPointers.put(slot, 0);
        }
        ARMOR_SLOT mostSignificantSlot = slots.get(slots.size()-1);
        long count = 0;
        int lastReportedPercentile = 0;
        while(slotPointers.get(mostSignificantSlot) < slotBuckets.get(mostSignificantSlot).size()){
            ArmorSet armorSet = new ArmorSet();
            for(ARMOR_SLOT slot : slots){
                armorSet.addArmor(slotBuckets.get(slot).get(slotPointers.get(slot)));
            }
            armorSets.add(armorSet);
            slotPointers.put(slots.get(0), slotPointers.get(slots.get(0)) + 1);
            for(int i=0; i<slots.size() - 1; i++){
                ARMOR_SLOT slot = slots.get(i);
                if(slotPointers.get(slot) >= slotBuckets.get(slot).size()){
                    slotPointers.put(slot, 0);
                    ARMOR_SLOT nextSlot = slots.get(i + 1);
                    slotPointers.put(nextSlot, slotPointers.get(nextSlot) + 1);
                } else {
                    break;
                }
            }
            count++;

            // Report progress at 10% intervals
            int currentPercentile = (int) (((double) count / totalPossibleNonUniqueArmorSets) * 10);
            if (currentPercentile > lastReportedPercentile && currentPercentile <= 10) {
                lastReportedPercentile = currentPercentile;
                System.out.println(String.format("Progress: %d0%%", currentPercentile));
            }
        }

        return armorSets;
    }

    /**
     * Phase 1-2 optimization: Generates armor combinations with incremental Pareto filtering.
     * Returns only the Pareto-optimal sets, eliminating the need for a separate ranking step.
     *
     * Uses pre-computed arrays and deferred ArmorSet construction to minimize
     * per-iteration overhead. Only allocates ArmorSet objects for the rare
     * Pareto-winning combinations.
     */
    public Collection<ArmorSet> getOptimalArmorSets(){
        StreamingParetoFilter filter = new StreamingParetoFilter();

        Map<ARMOR_SLOT, List<Armor>> slotBuckets = getSlotBuckets();
        List<ARMOR_SLOT> slots = new ArrayList<ARMOR_SLOT>(slotBuckets.keySet());
        int numSlots = slots.size();

        // Pre-compute flat arrays for fast inner loop access
        int[] slotSizes = new int[numSlots];
        Armor[][] slotArmors = new Armor[numSlots][];
        int[][] saHashes = new int[numSlots][];
        double[][] saEncumbrances = new double[numSlots][];
        double[][] saResistances = new double[numSlots][];

        long totalCombinations = 1;
        for (int s = 0; s < numSlots; s++) {
            List<Armor> armorsInSlot = slotBuckets.get(slots.get(s));
            slotSizes[s] = armorsInSlot.size();
            totalCombinations *= slotSizes[s];
            slotArmors[s] = armorsInSlot.toArray(new Armor[0]);
            saHashes[s] = new int[slotSizes[s]];
            saEncumbrances[s] = new double[slotSizes[s]];
            saResistances[s] = new double[slotSizes[s]];
            for (int a = 0; a < slotSizes[s]; a++) {
                Armor.SlotAgnosticArmor sa = slotArmors[s][a].getSlotAgnosticArmor();
                saHashes[s][a] = sa.hashCode();
                saEncumbrances[s][a] = sa.getEncumbrance();
                saResistances[s][a] = sa.getResistanceScore();
            }
        }

        int[] ptrs = new int[numSlots];
        long count = 0;
        int lastReportedPercentile = 0;

        while (count < totalCombinations) {
            // Compute dedup hash, encumbrance, and resistance from pre-computed arrays
            int hash = 0;
            double enc = 0;
            double res = 0;
            for (int s = 0; s < numSlots; s++) {
                int idx = ptrs[s];
                hash += saHashes[s][idx];
                enc += saEncumbrances[s][idx];
                res += saResistances[s][idx];
            }

            // Two-phase filter: only build ArmorSet if it passes dedup + Pareto check
            if (filter.check(hash, enc, res)) {
                ArmorSet armorSet = new ArmorSet();
                for (int s = 0; s < numSlots; s++) {
                    armorSet.addArmor(slotArmors[s][ptrs[s]]);
                }
                filter.addWinner(enc, res, armorSet);
            }

            // Increment pointers (odometer style)
            ptrs[0]++;
            for (int i = 0; i < numSlots - 1; i++) {
                if (ptrs[i] >= slotSizes[i]) {
                    ptrs[i] = 0;
                    ptrs[i + 1]++;
                } else {
                    break;
                }
            }
            count++;

            // Report progress at 10% intervals
            int currentPercentile = (int) (((double) count / totalCombinations) * 10);
            if (currentPercentile > lastReportedPercentile && currentPercentile <= 10) {
                lastReportedPercentile = currentPercentile;
                System.out.println(String.format("Progress: %d0%%", currentPercentile));
            }
        }

        System.out.println(String.format("Final: Processed %d combinations, %d unique, %d Pareto-optimal, collisions: %d (%.4f%%)",
                count, filter.getSeenCount(), filter.getFrontierCount(), filter.getHashCollisions(), filter.getCollisionRate()));

        return filter.getWinningSets();
    }

}
