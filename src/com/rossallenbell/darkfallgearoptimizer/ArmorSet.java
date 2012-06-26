
package com.rossallenbell.darkfallgearoptimizer;

import java.util.ArrayList;
import java.util.Collection;

import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.OPTIMIZATION;

public class ArmorSet {

    public Collection<Armor> armors;

    public double averagePhysicalResistance;

    public double averageMagicalResistance;

    public double averageResistance;

    public double encumbrance;

    public int ingots;

    public ArmorSet() {
        this.armors = new ArrayList<Armor>();

        this.averagePhysicalResistance = 0;
        this.averageMagicalResistance = 0;
        this.averageResistance = 0;
        this.encumbrance = 0;
        this.ingots = 0;
    }

    public void addArmor(Armor armor) {
        this.armors.add(armor);
        this.averagePhysicalResistance += armor.averagePhysicalResistance;
        this.averageMagicalResistance += armor.averageMagicalResistance;
        if(DarkfallGearOptimizer.RESULTS_OPTIMIZATION == OPTIMIZATION.Physical){
            this.averageResistance = this.averagePhysicalResistance;
        } else if(DarkfallGearOptimizer.RESULTS_OPTIMIZATION == OPTIMIZATION.Magical){
            this.averageResistance = this.averageMagicalResistance;
        } else {
            this.averageResistance = (this.averageMagicalResistance + this.averagePhysicalResistance) / 2;            
        }
        this.encumbrance += armor.encumbrance;
        this.ingots += armor.ingots;
    }

    @Override
    public String toString() {
        StringBuilder rVal = new StringBuilder();

        rVal.append("[ArmorSet:\n");
        if(DarkfallGearOptimizer.RESULTS_OPTIMIZATION == OPTIMIZATION.Both){
            rVal.append("averageResistance: " + this.averageResistance + "\n");
        }
        rVal.append("averagePhysicalResistance: " + this.averagePhysicalResistance + "\n");
        rVal.append("averageMagicalResistance: " + this.averageMagicalResistance + "\n");
        rVal.append("encumbrance: " + this.encumbrance + "\n");
        rVal.append("ingots: " + this.ingots + "\n");
        for (Armor armor : this.armors) {
            rVal.append("\t" + armor.toString() + "\n");
        }
        rVal.append("]");

        return rVal.toString();
    }

}
