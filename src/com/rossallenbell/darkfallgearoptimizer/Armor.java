
package com.rossallenbell.darkfallgearoptimizer;

import java.util.HashMap;

import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.ARMOR_TYPE;
import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.PROTECTION;
import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.SLOT;

public class Armor {

    public ARMOR_TYPE type;

    public SLOT slot;

    public double encumbrance;
    
    public int ingots;

    public double averagePhysicalResistance;

    public double averageMagicalResistance;

    public Armor(String fileLine) {
        String[] cells = fileLine.split(",");

        this.type = ARMOR_TYPE.valueOf(cells[0]);
        this.slot = SLOT.valueOf(cells[1]);
        this.encumbrance = Double.parseDouble(cells[2]);
        this.ingots = Integer.parseInt(cells[3]);

        HashMap<PROTECTION, Double> protections = new HashMap<PROTECTION, Double>();
        for (int i = 0; i < 10; i++) {
            protections.put(PROTECTION.valueOf(DarkfallGearOptimizer.dataColumns[i + 4]), Double.valueOf(cells[i + 4]));
        }

        setAveragePhysicalResistance(protections);
        setAverageMagicalResistance(protections);
    }

    public void setAveragePhysicalResistance(HashMap<PROTECTION, Double> protections) {
        double physicalResistanceSum = 0;
        for (PROTECTION protection : DarkfallGearOptimizer.PHYSICAL_PROTECTIONS) {
            if (protections.containsKey(protection)) {
                physicalResistanceSum += protections.get(protection).doubleValue();
            }
        }
        this.averagePhysicalResistance = physicalResistanceSum / DarkfallGearOptimizer.PHYSICAL_PROTECTIONS.length;
    }

    public void setAverageMagicalResistance(HashMap<PROTECTION, Double> protections) {
        double magicalResistanceSum = 0;
        for (PROTECTION protection : DarkfallGearOptimizer.MAGICAL_PROTECTIONS) {
            if (protections.containsKey(protection)) {
                magicalResistanceSum += protections.get(protection).doubleValue();
            }
        }
        this.averageMagicalResistance = magicalResistanceSum / DarkfallGearOptimizer.MAGICAL_PROTECTIONS.length;
    }
    
    @Override
    public String toString(){
        return this.slot + "-" + this.type;
    }
}
