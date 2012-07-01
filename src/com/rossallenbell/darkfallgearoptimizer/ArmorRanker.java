package com.rossallenbell.darkfallgearoptimizer;

import java.util.Collection;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class ArmorRanker {
    
    Set<ArmorSet> armorSets;

    public ArmorRanker(Set<ArmorSet> armorSets) {
        this.armorSets = armorSets;
    }

    public Collection<ArmorSet> getWinningSets() {
        SortedMap<Double, ArmorSet> winningSets = new TreeMap<Double, ArmorSet>();
        
        for(ArmorSet armorSet : armorSets){
            double encumbrance = armorSet.getEncumbrance();
            double resistanceScore = armorSet.getResistanceScore();
            boolean winner = false;
            
            if(winningSets.containsKey(encumbrance)){
                if(winningSets.get(encumbrance).getResistanceScore() < resistanceScore){
                    winner = true;
                }
            } else {
                SortedMap<Double, ArmorSet> lighterSets = winningSets.headMap(encumbrance);
                if(lighterSets.isEmpty() || lighterSets.get(lighterSets.lastKey()).getResistanceScore() < resistanceScore) {
                    winner = true;
                }
            }
            
            if(winner){
                SortedMap<Double, ArmorSet> heavierSets = winningSets.tailMap(encumbrance);
                while(!heavierSets.isEmpty()){
                    double nextLightestEncumbrance = heavierSets.firstKey();
                    if(heavierSets.get(nextLightestEncumbrance).resistanceScore <= resistanceScore){
                        winningSets.remove(nextLightestEncumbrance);
                    } else {
                        break;
                    }
                    heavierSets = winningSets.tailMap(encumbrance);
                }
                winningSets.put(encumbrance, armorSet);
            }
        }
        
        return winningSets.values();
    }
    
}
