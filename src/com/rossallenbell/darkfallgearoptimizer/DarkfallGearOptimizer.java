
package com.rossallenbell.darkfallgearoptimizer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class DarkfallGearOptimizer {

    public static final String[] DEFAULT_TARGETED_STATS = { "Fire", "Slashing" };

    public static enum PROTECTION {
        Bludgeoning, Piercing, Slashing, Acid, Cold, Fire, Holy, Lightning, Unholy, Arcane, Impact
    }

    public static final PROTECTION[] PHYSICAL_PROTECTIONS = { PROTECTION.Bludgeoning, PROTECTION.Piercing, PROTECTION.Slashing };

    public static final PROTECTION[] MAGICAL_PROTECTIONS = { PROTECTION.Acid, PROTECTION.Cold, PROTECTION.Fire, PROTECTION.Holy, PROTECTION.Lightning, PROTECTION.Unholy };//, PROTECTION.Arcane };

    public static enum ARMOR_TYPE {
        Cloth, Padded, Leather, Studded, Bone, Chain, Banded, Scale, Plate, FullPlate, NoArmor
    }

    public static enum SLOT {
        Chest, Head, Leg, Foot, Hand, Arm, Elbow, Shoulder, Shin, Waist
    }

    public static String[] dataColumns;
    
    public static enum OPTIMIZATION { Physical, Magical, Both }
    
    public static final OPTIMIZATION RESULTS_OPTIMIZATION = OPTIMIZATION.Magical;

    public static void main(String[] args) {
        File file = new File("./data/df_gear.csv");
//        File file = new File("./data/df_gear_short_test.csv");
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }

        Map<SLOT, List<Armor>> armors = new HashMap<SLOT, List<Armor>>();
        if (bufferedReader != null) {
            String line;
            try {
                dataColumns = bufferedReader.readLine().split(",");
                while ((line = bufferedReader.readLine()) != null) {
                    line = line.replaceAll("\"", "");
                    Armor thisArmor = new Armor(line);
                    if (!armors.containsKey(thisArmor.slot)) {
                        armors.put(thisArmor.slot, new ArrayList<Armor>());
                    }
                    armors.get(thisArmor.slot).add(thisArmor);
                }
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        } else {
            System.exit(1);
        }

        SLOT[] slots = SLOT.values();
        int[] slotSizes = new int[slots.length];
        int[] slotIndices = new int[slots.length];
        for (int i = 0; i < slots.length; i++) {
            slotSizes[i] = armors.get(slots[i]).size();
        }

        int combinations = 1;
        for (int count : slotSizes) {
            combinations *= count;
        }
        System.out.println("Total armor combinations: " + combinations);
        System.out.println("Calculating all combinations...");

        TreeSet<ArmorSetTreeNode> ideals = new TreeSet<ArmorSetTreeNode>();

        int count = 0;
        int lastOutputPercent = 0;
        while (true) {
            ArmorSet armorSet = new ArmorSet();
            for (int i = 0; i < slots.length; i++) {
                armorSet.addArmor(armors.get(slots[i]).get(slotIndices[i]));
            }

            boolean incrementNext = true;
            boolean breakLoop = true;
            for (int i = 0; i < slotIndices.length; i++) {
                if (incrementNext) {
                    slotIndices[i]++;
                    if (slotIndices[i] >= slotSizes[i]) {
                        slotIndices[i] = 0;
                        incrementNext = true;
                        continue;
                    }
                }
                breakLoop = false;
                incrementNext = false;
            }

            count++;
            int thisPercent = (int) (((double) count) / combinations * 100);
            if (thisPercent != lastOutputPercent) {
                lastOutputPercent = thisPercent;
                System.out.print(lastOutputPercent + "% ");
                if(thisPercent % 25 == 0){
                    System.out.println();
                }
            }

            ArmorSetTreeNode armorSetTN = new ArmorSetTreeNode(armorSet);
            ArmorSetTreeNode ceiling = ideals.ceiling(armorSetTN);

            if (ceiling == null || ceiling.encumbrance >= armorSetTN.encumbrance) {
                if (ceiling == null) {
                    ideals.add(armorSetTN);
                } else if (ceiling.encumbrance >= armorSetTN.encumbrance) {
                    if (ceiling.averageResistance == armorSet.averageResistance) {
                        ceiling.add(armorSet);
                    } else {
                        ideals.add(armorSetTN);
                    }
                }

                ArmorSetTreeNode lower = ideals.lower(armorSetTN);
                while (lower != null && lower.encumbrance >= armorSetTN.encumbrance) {
                    ideals.remove(lower);
                    lower = ideals.lower(armorSetTN);
                }
            }

            if (breakLoop) {
                break;
            }
        }

        int idealCount = 0;
        for (ArmorSetTreeNode armorSetTN : ideals){
            idealCount += armorSetTN.armorSets.size();
        }

        System.out.println("Writing ideal sets");
        BufferedWriter bufferedWriter = null;
        File resultsfile = new File("./data/" + System.currentTimeMillis() + "_idealSets.txt");
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(resultsfile));
            
            bufferedWriter.write("Finding ideal armor combinations for resists: " + RESULTS_OPTIMIZATION + "\n");
            
            bufferedWriter.write("Total armor combinations: " + combinations + "\n");
            bufferedWriter.write("Ideal sets found: " + idealCount + "\n");
            bufferedWriter.write("Non-ideal armor sets rooted out: " + (combinations - idealCount) + "\n");
            bufferedWriter.write("\n");

            System.out.println("Ideal sets found: " + idealCount);
            System.out.println("Non-ideal armor sets rooted out: " + (combinations - idealCount));
            
            for (ArmorSetTreeNode armorSetTN : ideals.descendingSet()) {
                for (ArmorSet armorSet : armorSetTN.armorSets) {
                    bufferedWriter.write(armorSet.toString() + "\n");
                }
            }
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

}
