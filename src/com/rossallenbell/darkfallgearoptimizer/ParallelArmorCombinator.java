package com.rossallenbell.darkfallgearoptimizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.ARMOR_SLOT;

/**
 * Phase 3 optimization: Parallel processing across CPU cores.
 * Partitions combination generation across threads, each with its own StreamingParetoFilter.
 * Merges Pareto frontiers after threads complete.
 *
 * Memory impact: Same as Phase 2 (~7.6MB per thread)
 * Performance impact: 4-8x faster on 4-8 cores
 */
public class ParallelArmorCombinator {

    private final Collection<Armor> armors;
    private final int threadCount;

    public ParallelArmorCombinator(Collection<Armor> armors, int threadCount) {
        this.armors = armors;
        this.threadCount = Math.max(1, threadCount);
    }

    /**
     * Generates optimal armor sets using parallel processing.
     */
    public Collection<ArmorSet> getOptimalArmorSets() {
        if (threadCount == 1) {
            // Single-threaded: use regular implementation
            return new ArmorCombinator(armors).getOptimalArmorSets();
        }

        Map<ARMOR_SLOT, List<Armor>> slotBuckets = new ArmorCombinator(armors).getSlotBuckets();

        if (slotBuckets.isEmpty()) {
            return new ArrayList<ArmorSet>();
        }

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

        // Cap effective threads to total combinations
        int effectiveThreads = (int) Math.min(threadCount, totalCombinations);
        long partitionSize = (long) Math.ceil((double) totalCombinations / effectiveThreads);

        // Shared progress tracking
        AtomicLong globalCount = new AtomicLong(0);
        AtomicInteger lastReportedPercentile = new AtomicInteger(0);

        // Create thread pool
        ExecutorService executor = Executors.newFixedThreadPool(effectiveThreads);
        List<Future<StreamingParetoFilter>> futures = new ArrayList<>();

        // Launch worker threads
        for (int t = 0; t < effectiveThreads; t++) {
            final long startIdx = (long) t * partitionSize;
            final long endIdx = Math.min(startIdx + partitionSize, totalCombinations);

            if (startIdx >= totalCombinations) {
                break; // No more work
            }

            Callable<StreamingParetoFilter> worker = new Worker(
                    slotArmors, slotSizes, saHashes, saEncumbrances, saResistances,
                    numSlots, startIdx, endIdx,
                    totalCombinations, globalCount, lastReportedPercentile);
            futures.add(executor.submit(worker));
        }

        // Collect results from all threads
        List<StreamingParetoFilter> filters = new ArrayList<>();
        try {
            for (Future<StreamingParetoFilter> future : futures) {
                filters.add(future.get());
            }
        } catch (Exception e) {
            throw new RuntimeException("Parallel processing failed", e);
        } finally {
            executor.shutdown();
        }

        // Merge Pareto frontiers
        return mergeParetoFrontiers(filters);
    }

    /**
     * Merges multiple Pareto frontiers into a single frontier.
     */
    private Collection<ArmorSet> mergeParetoFrontiers(List<StreamingParetoFilter> filters) {
        StreamingParetoFilter mergedFilter = new StreamingParetoFilter();

        // Feed all winning sets from all threads into merged filter
        for (StreamingParetoFilter filter : filters) {
            for (ArmorSet set : filter.getWinningSets()) {
                mergedFilter.tryAdd(set);
            }
        }

        return mergedFilter.getWinningSets();
    }

    /**
     * Converts a flat combination index to slot pointer values.
     * Treats slots as digits in a mixed-radix number (slot 0 is least significant).
     */
    private static int[] flatIndexToPointers(long flatIndex, int[] slotSizes) {
        int[] pointers = new int[slotSizes.length];
        long remaining = flatIndex;
        for (int i = 0; i < slotSizes.length; i++) {
            pointers[i] = (int) (remaining % slotSizes[i]);
            remaining /= slotSizes[i];
        }
        return pointers;
    }

    /**
     * Worker thread that processes a range of the flat combination index space.
     * Uses pre-computed arrays and deferred ArmorSet construction to minimize
     * per-iteration overhead.
     */
    private static class Worker implements Callable<StreamingParetoFilter> {
        private final Armor[][] slotArmors;
        private final int[] slotSizes;
        private final int[][] saHashes;
        private final double[][] saEncumbrances;
        private final double[][] saResistances;
        private final int numSlots;
        private final long startIdx;
        private final long endIdx;
        private final long totalCombinations;
        private final AtomicLong globalCount;
        private final AtomicInteger lastReportedPercentile;

        public Worker(Armor[][] slotArmors, int[] slotSizes,
                      int[][] saHashes, double[][] saEncumbrances, double[][] saResistances,
                      int numSlots, long startIdx, long endIdx, long totalCombinations,
                      AtomicLong globalCount, AtomicInteger lastReportedPercentile) {
            this.slotArmors = slotArmors;
            this.slotSizes = slotSizes;
            this.saHashes = saHashes;
            this.saEncumbrances = saEncumbrances;
            this.saResistances = saResistances;
            this.numSlots = numSlots;
            this.startIdx = startIdx;
            this.endIdx = endIdx;
            this.totalCombinations = totalCombinations;
            this.globalCount = globalCount;
            this.lastReportedPercentile = lastReportedPercentile;
        }

        @Override
        public StreamingParetoFilter call() {
            StreamingParetoFilter filter = new StreamingParetoFilter();

            // Initialize slot pointers from flat index
            int[] ptrs = flatIndexToPointers(startIdx, slotSizes);

            long totalForThread = endIdx - startIdx;
            long count = 0;
            long unreportedCount = 0;

            // Generate combinations for this thread's range
            while (count < totalForThread) {
                // Compute dedup hash, encumbrance, and resistance from pre-computed arrays
                // without allocating any objects
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

                // Increment pointers (odometer style) using int arrays
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
                unreportedCount++;

                // Batch-update shared progress counter
                if (unreportedCount >= 10000 || count == totalForThread) {
                    long current = globalCount.addAndGet(unreportedCount);
                    unreportedCount = 0;

                    int percentile = (int) (((double) current / totalCombinations) * 10);
                    int lastReported = lastReportedPercentile.get();
                    while (percentile > lastReported && lastReported < 10) {
                        if (lastReportedPercentile.compareAndSet(lastReported, percentile)) {
                            System.out.println(String.format("Progress: %d0%%", percentile));
                            break;
                        }
                        lastReported = lastReportedPercentile.get();
                    }
                }
            }

            return filter;
        }
    }
}
