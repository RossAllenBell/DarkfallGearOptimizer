
package com.rossallenbell.darkfallgearoptimizer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.rossallenbell.darkfallgearoptimizer.data.CsvArmorProvider;

public class DarkfallGearOptimizer {

    public static enum PROTECTION {
        Bludgeoning, Piercing, Slashing, Acid, Cold, Fire, Holy, Lightning, Unholy, Impact
    }

    public static final PROTECTION[] PHYSICAL_PROTECTIONS = { PROTECTION.Bludgeoning, PROTECTION.Piercing, PROTECTION.Slashing };

    public static final PROTECTION[] MAGICAL_PROTECTIONS = { PROTECTION.Acid, PROTECTION.Cold, PROTECTION.Fire, PROTECTION.Holy, PROTECTION.Lightning, PROTECTION.Unholy };

    public static enum ARMOR_TYPE {
        Cloth, Padded, Leather, Studded, Bone, Chain, Banded, Scale, Plate, FullPlate, Infernal, Dragon, NoArmor
    }

    public static enum ARMOR_SLOT {
        Chest, Head, Legs, Boots, Gauntlets, Arms, Elbows, Shoulders, Greaves, Girdle
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        String filePath = "./data/default_set.csv";
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
        System.out.println("Producing all armor combinations...");
        Set<ArmorSet> armorSets = combinator.getArmorSets();
        System.out.println(String.format("Unique armor combinations found: %d", armorSets.size()));
    }

}
