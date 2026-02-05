package com.rossallenbell.darkfallgearoptimizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.ARMOR_SLOT;
import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.ARMOR_TYPE;
import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.PROTECTION;

public class StreamingParetoFilterTest {

    Armor scaleArms;
    Armor scaleGloves;
    Armor fullPlateArms;
    Armor fullPlateGloves;

    @Before
    public void setUp() {
        scaleArms = new Armor(ARMOR_TYPE.Scale, ARMOR_SLOT.Arms, 1);
        scaleArms.addResistance(PROTECTION.Slashing, 0.25);
        scaleGloves = new Armor(ARMOR_TYPE.Scale, ARMOR_SLOT.Gauntlets, 1);
        scaleGloves.addResistance(PROTECTION.Slashing, 0.25);
        fullPlateArms = new Armor(ARMOR_TYPE.FullPlate, ARMOR_SLOT.Arms, 2);
        fullPlateArms.addResistance(PROTECTION.Slashing, 0.6);
        fullPlateGloves = new Armor(ARMOR_TYPE.FullPlate, ARMOR_SLOT.Gauntlets, 2);
        fullPlateGloves.addResistance(PROTECTION.Slashing, 0.6);
    }

    @Test
    public void testHashBasedDeduplication() {
        // Two identical sets should only add first one
        StreamingParetoFilter filter = new StreamingParetoFilter();

        ArmorSet set1 = new ArmorSet();
        set1.addArmor(scaleArms);
        set1.addArmor(fullPlateGloves);

        ArmorSet set2 = new ArmorSet();
        set2.addArmor(scaleArms);
        set2.addArmor(fullPlateGloves);

        assertTrue("First set should be added", filter.tryAdd(set1));
        assertFalse("Duplicate set should not be added", filter.tryAdd(set2));
        assertEquals(1, filter.getWinningSets().size());
    }

    @Test
    public void testSlotAgnosticDuplicateWithHash() {
        // Verify hash-based deduplication works for slot-agnostic duplicates
        StreamingParetoFilter filter = new StreamingParetoFilter();

        ArmorSet set1 = new ArmorSet();
        set1.addArmor(scaleArms);
        set1.addArmor(fullPlateGloves);

        ArmorSet set2 = new ArmorSet();
        set2.addArmor(fullPlateArms);
        set2.addArmor(scaleGloves);

        assertTrue("First set should be added", filter.tryAdd(set1));
        assertFalse("Slot-agnostic duplicate should not be added", filter.tryAdd(set2));
        assertEquals(1, filter.getWinningSets().size());
    }

    @Test
    public void testHashCollisionTracking() {
        // Verify hash collision counting works
        StreamingParetoFilter filter = new StreamingParetoFilter();

        // Add several sets
        for (int i = 0; i < 100; i++) {
            ArmorSet set = createArmorSet(i + 1.0, i * 0.5);
            filter.tryAdd(set);
        }

        // With good hash function, collision rate should be very low
        double collisionRate = filter.getHashCollisions() / (double) filter.getSeenCount();
        assertTrue("Collision rate should be very low: " + collisionRate, collisionRate < 0.01);
    }

    @Test
    public void testTryAdd_NewUniqueDominating() {
        StreamingParetoFilter filter = new StreamingParetoFilter();

        ArmorSet set1 = new ArmorSet();
        set1.addArmor(scaleArms);

        assertTrue("First set should be added", filter.tryAdd(set1));
        assertEquals(1, filter.getWinningSets().size());
    }

    @Test
    public void testTryAdd_Dominated() {
        StreamingParetoFilter filter = new StreamingParetoFilter();

        ArmorSet light = createArmorSet(2.0, 1.0);
        assertTrue("Light set should be added", filter.tryAdd(light));

        ArmorSet dominated = createArmorSet(3.0, 0.8);
        assertFalse("Dominated set should not be added", filter.tryAdd(dominated));
        assertEquals(1, filter.getWinningSets().size());
    }

    @Test
    public void testTryAdd_DominatesExisting() {
        StreamingParetoFilter filter = new StreamingParetoFilter();

        ArmorSet heavy = createArmorSet(3.0, 0.8);
        assertTrue("Heavy set should be added", filter.tryAdd(heavy));

        ArmorSet dominating = createArmorSet(2.0, 1.0);
        assertTrue("Dominating set should be added", filter.tryAdd(dominating));

        Collection<ArmorSet> winners = filter.getWinningSets();
        assertEquals(1, winners.size());
        assertTrue(winners.contains(dominating));
        assertFalse(winners.contains(heavy));
    }

    @Test
    public void testTryAdd_BothOnFrontier() {
        StreamingParetoFilter filter = new StreamingParetoFilter();

        ArmorSet light = createArmorSet(2.0, 0.5);
        ArmorSet heavy = createArmorSet(3.0, 1.0);

        assertTrue("Light set should be added", filter.tryAdd(light));
        assertTrue("Heavy set should be added", filter.tryAdd(heavy));
        assertEquals(2, filter.getWinningSets().size());
    }

    @Test
    public void testTryAdd_ComplexScenario() {
        StreamingParetoFilter filter = new StreamingParetoFilter();

        ArmorSet s1 = createArmorSet(1.0, 0.3);
        ArmorSet s2 = createArmorSet(2.0, 0.7);
        ArmorSet s3 = createArmorSet(3.0, 1.0);
        ArmorSet dominated = createArmorSet(2.5, 0.6);

        assertTrue(filter.tryAdd(s1));
        assertTrue(filter.tryAdd(s2));
        assertTrue(filter.tryAdd(s3));
        assertFalse("Dominated set should not be added", filter.tryAdd(dominated));

        assertEquals(3, filter.getWinningSets().size());
    }

    @Test
    public void testMemoryEfficiency() {
        // This is more of a documentation test
        // Hash-based storage should use ~4 bytes per seen combination
        // vs ~48+ bytes for Map<SlotAgnosticArmor, Integer> objects
        StreamingParetoFilter filter = new StreamingParetoFilter();

        // Add sets with some dominated ones
        // Create 10 frontier points with clear progression
        for (int i = 0; i < 10; i++) {
            ArmorSet set = createArmorSet(i + 1.0, i * 2.0);
            filter.tryAdd(set);
        }

        // Add 90 dominated sets (heavier and weaker than frontier points)
        for (int i = 0; i < 90; i++) {
            // Create sets that are clearly dominated (heavier with less resistance)
            // These will be heavier than (5, 8) but weaker, so dominated
            ArmorSet set = createArmorSet(6.0 + (i * 0.01), 7.0 - (i * 0.01));
            filter.tryAdd(set);
        }

        assertEquals(100, filter.getSeenCount());
        // Only ~10 should be on frontier
        assertTrue("Frontier should be much smaller than total seen: " +
                   filter.getFrontierCount() + " vs " + filter.getSeenCount(),
                filter.getFrontierCount() < 20);
    }

    @Test
    public void testGetWinningSets_Empty() {
        StreamingParetoFilter filter = new StreamingParetoFilter();
        assertEquals(0, filter.getWinningSets().size());
    }

    /**
     * Helper to create unique armor sets with specific encumbrance and resistance
     */
    private static int uniqueId = 0;
    private ArmorSet createArmorSet(double encumbrance, double resistance) {
        ArmorSet set = new ArmorSet();

        ARMOR_TYPE[] types = ARMOR_TYPE.values();
        ARMOR_TYPE type = types[uniqueId % types.length];
        ARMOR_SLOT slot = ARMOR_SLOT.values()[uniqueId % ARMOR_SLOT.values().length];

        Armor armor = new Armor(type, slot, encumbrance);
        armor.addResistance(PROTECTION.Slashing, resistance);
        set.addArmor(armor);
        uniqueId++;

        return set;
    }
}
