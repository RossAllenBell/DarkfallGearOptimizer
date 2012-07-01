
package com.rossallenbell.darkfallgearoptimizer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collection;
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
    
    @SuppressWarnings("serial")
    public static final Map<PROTECTION, Double> protectionWeights = new HashMap<PROTECTION, Double>(){
        {
            this.put(PROTECTION.Slashing, 1.0);
            this.put(PROTECTION.Fire, 1.0);
        }
    };

    public static void main(String[] args) throws FileNotFoundException, IOException {
        String filePath = "./data/default_set.csv";
        //String filePath = "./data/short_set.csv";
        //String filePath = "./data/all_armor.csv";
        
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
        System.out.println("Ordering and filtering out non-ideal sets...");
        ArmorRanker ranker = new ArmorRanker(armorSets);
        Collection<ArmorSet> winningArmorSets = ranker.getWinningSets();
        System.out.println(String.format("Ideal armor combinations found: %d", winningArmorSets.size()));
        writeOutResults(ranker.getWinningSets());
    }

    private static void writeOutResults(Collection<ArmorSet> winningSets) throws IOException {
        String filePath = "./results_" + System.currentTimeMillis() + ".txt";
        System.out.println("Writing to path: " + filePath);
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(filePath)));
        for(ArmorSet armorSet : winningSets){
            bufferedWriter.write(armorSet.toString());
        }
        bufferedWriter.close();
    }
    
}
