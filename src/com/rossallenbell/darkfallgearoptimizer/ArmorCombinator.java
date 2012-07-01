package com.rossallenbell.darkfallgearoptimizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.rossallenbell.darkfallgearoptimizer.DarkfallGearOptimizer.ARMOR_SLOT;

public class ArmorCombinator {
    
    private final Collection<Armor> armors;
    
    public ArmorCombinator(Collection<Armor> armors){
        this.armors = armors;
    }
    
    public Map<ARMOR_SLOT, List<Armor>> getSlotBuckets(){
        Map<ARMOR_SLOT, List<Armor>> slotBuckets = new HashMap<DarkfallGearOptimizer.ARMOR_SLOT, List<Armor>>();
        for (Armor armor : armors) {
            if(!slotBuckets.containsKey(armor.slot)){
                slotBuckets.put(armor.slot, new ArrayList<Armor>());
            }
            slotBuckets.get(armor.slot).add(armor);
        }
        return slotBuckets;
    }
    
    public Set<ArmorSet> getArmorSets(){
        Set<ArmorSet> armorSets = new HashSet<ArmorSet>();
        
        Map<ARMOR_SLOT, List<Armor>> slotBuckets = getSlotBuckets();
        List<ARMOR_SLOT> slots = new ArrayList<ARMOR_SLOT>(slotBuckets.keySet());
        Map<ARMOR_SLOT, Integer> slotPointers = new HashMap<ARMOR_SLOT, Integer>();
        for(ARMOR_SLOT slot : slots){
            slotPointers.put(slot, 0);
        }
        ARMOR_SLOT mostSignificantSlot = slots.get(slots.size()-1);
        int count = 0;
        while(slotPointers.get(mostSignificantSlot) < slotBuckets.get(mostSignificantSlot).size()){
            ArmorSet armorSet = new ArmorSet();
            for(ARMOR_SLOT slot : slots){
                armorSet.addArmor(slotBuckets.get(slot).get(slotPointers.get(slot)));
            }
            armorSets.add(armorSet);
            slotPointers.put(slots.get(0), slotPointers.get(slots.get(0)) + 1);
            for(int i=0; i<slots.size() - 1; i++){
                ARMOR_SLOT slot = slots.get(i);
                if(slotPointers.get(slot) >= slotBuckets.get(slot).size()){
                    slotPointers.put(slot, 0);
                    ARMOR_SLOT nextSlot = slots.get(i + 1);
                    slotPointers.put(nextSlot, slotPointers.get(nextSlot) + 1);
                } else {
                    break;
                }
            }
            count++;
            if(count % 100000 == 0){
                System.out.println(String.format("Processed %d combinations, retaining %d", count, armorSets.size() ));
            }
        }
        
        return armorSets;
    }
    
}
