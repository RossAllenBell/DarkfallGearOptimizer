package com.rossallenbell.darkfallgearoptimizer;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.ARMOR_SLOT;
import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.ARMOR_TYPE;
import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.PROTECTION;

public class ArmorRankerTest {
    
    Armor scaleArms;
    Armor scaleGloves;
    Armor scaleLegs;
    Armor fullPlateArms;
    Armor fullPlateGloves;
    Armor infernalChest;
    
    @Before
    public void setUp() {
        scaleArms = new Armor(ARMOR_TYPE.Scale, ARMOR_SLOT.Arms, 1);
        scaleArms.addResistance(PROTECTION.Slashing, 0.25);
        scaleGloves = new Armor(ARMOR_TYPE.Scale, ARMOR_SLOT.Gauntlets, 1);
        scaleGloves.addResistance(PROTECTION.Slashing, 0.25);
        scaleLegs = new Armor(ARMOR_TYPE.Scale, ARMOR_SLOT.Legs, 0.9);
        scaleLegs.addResistance(PROTECTION.Slashing, 0.25);
        fullPlateArms = new Armor(ARMOR_TYPE.FullPlate, ARMOR_SLOT.Arms, 2);
        fullPlateArms.addResistance(PROTECTION.Slashing, 0.6);
        fullPlateGloves = new Armor(ARMOR_TYPE.FullPlate, ARMOR_SLOT.Gauntlets,
                2);
        fullPlateGloves.addResistance(PROTECTION.Slashing, 0.6);
        infernalChest = new Armor(ARMOR_TYPE.Infernal, ARMOR_SLOT.Chest, 3);
        infernalChest.addResistance(PROTECTION.Slashing, 1.33);
    }
    
    @Test
    public void test() {
        Set<Armor> armors = new HashSet<Armor>(Arrays.asList(new Armor[] {
                scaleArms, scaleGloves, scaleLegs, fullPlateArms,
                fullPlateGloves, infernalChest }));
        Set<ArmorSet> armorSets = new ArmorCombinator(armors).getArmorSets();
        ArmorRanker ranker = new ArmorRanker(armorSets);
        Collection<ArmorSet> winningSets = ranker.getWinningSets();
        assertEquals(3, winningSets.size());
        Iterator<ArmorSet> iterator = winningSets.iterator();
        assertEquals(5.90, iterator.next().encumbrance, 0);
        assertEquals(1.215, iterator.next().resistanceScore, 0);
        assertEquals(3, iterator.next().getSAArmorWithCounts().keySet().size());
    }
    
}
