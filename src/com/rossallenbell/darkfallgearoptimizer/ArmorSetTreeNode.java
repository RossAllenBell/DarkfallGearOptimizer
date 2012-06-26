
package com.rossallenbell.darkfallgearoptimizer;

import java.util.Collection;
import java.util.HashSet;

public class ArmorSetTreeNode implements Comparable<ArmorSetTreeNode> {

    public Collection<ArmorSet> armorSets;

    public double averageResistance;

    public double encumbrance;

    public int ingots;

    public ArmorSetTreeNode(ArmorSet armorSet) {
        this.armorSets = new HashSet<ArmorSet>();
        this.armorSets.add(armorSet);
        this.averageResistance = armorSet.averageResistance;
        this.encumbrance = armorSet.encumbrance;
        this.ingots = armorSet.ingots;
    }

    public void add(ArmorSet armorSet) {
        if (armorSet.encumbrance == this.encumbrance && armorSet.ingots == this.ingots) {
            this.armorSets.add(armorSet);
        } else if ((armorSet.encumbrance < this.encumbrance && armorSet.ingots <= this.ingots) || (armorSet.encumbrance <= this.encumbrance && armorSet.ingots < this.ingots)) {
            this.armorSets.clear();
            this.armorSets.add(armorSet);
            this.encumbrance = armorSet.encumbrance;
            this.ingots = armorSet.ingots;
        }
    }

    @Override
    public int compareTo(ArmorSetTreeNode o) {
        if (this.averageResistance > o.averageResistance) return 1;
        if (this.averageResistance < o.averageResistance) return -1;
        return o.ingots - this.ingots;
    }

}
