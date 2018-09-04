package cn.academy.advancements;

import cn.academy.ability.Category;
import cn.academy.ability.Skill;
import cn.academy.advancements.triggers.ACLevelTrigger;
import cn.academy.advancements.triggers.ACTrigger;
import cn.academy.datapart.AbilityData;
import cn.academy.event.ability.LevelChangeEvent;
import cn.academy.event.ability.SkillLearnEvent;
import cn.academy.event.MatterUnitHarvestEvent;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @author EAirPeter
 */
public final class DispatcherAch {

    public static final DispatcherAch INSTANCE = new DispatcherAch();
    
    
    //net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemCraftedEvent
    
    private final HashMap<Item, HashSet<ACTrigger>> hcItemCrafted = new HashMap<>();
    
    public void rgItemCrafted(Item item, ACTrigger ach) {
        if (hcItemCrafted.containsKey(item))
            hcItemCrafted.get(item).add(ach);
        else {
            HashSet<ACTrigger> set = new HashSet<>();
            set.add(ach);
            hcItemCrafted.put(item, set);
        }
        hcItemCrafted.get(item).add(ach);
    }
    
    public void urItemCrafted(Item item, ACTrigger ach) {
        HashSet<ACTrigger> set = hcItemCrafted.get(item);
        if (set != null)
            set.remove(ach);
    }
    
    @SubscribeEvent
    public void onItemCrafted(ItemCraftedEvent event) {
        HashSet<ACTrigger> set = hcItemCrafted.get(event.crafting.getItem());
        if (set != null)
            if (event.player instanceof EntityPlayerMP)
                for (ACTrigger a : set)
                    a.trigger((EntityPlayerMP) event.player);
    }
    
    
    //cn.academy.event.ability.LevelChangeEvent
    
    private final HashMap<Category, ACLevelTrigger[]> hcLevelChange = new HashMap<>();
    
    public void rgLevelChange(Category cat, int lv, ACLevelTrigger ach) {
        if (hcLevelChange.containsKey(cat))
            hcLevelChange.get(cat)[lv - 1] = ach;
        else {
            ACLevelTrigger[] arr = new ACLevelTrigger[5];
            arr[lv - 1] = ach;
            hcLevelChange.put(cat, arr);
        }
    }
    
    public void urLevelChange(Category cat, int lv) {
        ACLevelTrigger[] arr = hcLevelChange.get(cat);
        if (arr != null)
            arr[lv - 1] = null;
    }
    
    @SubscribeEvent
    public void onLevelChange(LevelChangeEvent event) {
        AbilityData data = AbilityData.get(event.player);
        if (data.hasCategory()) {
            int xlv = data.getLevel() - 1;
            ACLevelTrigger[] arr = hcLevelChange.get(data.getCategory());
            if(event.player instanceof EntityPlayerMP){
                if (arr != null && xlv >= 0 && arr[xlv] != null)
                    arr[xlv].trigger((EntityPlayerMP) event.player);
            }
        }
    }
    
    @SubscribeEvent
    public void onMatterUnitHarvest(MatterUnitHarvestEvent event) {
        if(event.player instanceof EntityPlayerMP)
            ACAchievements.aPhaseLiquid.trigger((EntityPlayerMP) event.player);
    }
    
    
    //cn.academy.event.ability.SkillLearnEvent
    
    private final HashMap<Skill, ACTrigger> hcSkillLearn = new HashMap<>();
    
    public void rgSkillLearn(Skill skill, ACTrigger ach) {
        hcSkillLearn.put(skill, ach);
    }
    
    public void urSkillLearn(Skill skill) {
        hcSkillLearn.remove(skill);
    }
    
    @SubscribeEvent
    public void onSkillLearn(SkillLearnEvent event) {
        ACTrigger ach = hcSkillLearn.get(event.skill);//CHANGED HERE
        if (ach != null)
            if(event.player instanceof EntityPlayerMP)
                ach.trigger((EntityPlayerMP) event.player);
    }
    
    //net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemPickupEvent
    
    private final Map<Item, ACTrigger> hcPlayerPickup = new HashMap();
    
    public void rgPlayerPickup(ItemStack stack, ACTrigger ach) {
        hcPlayerPickup.put(stack.getItem(), ach);
    }
    
    @SubscribeEvent
    public void onPlayerPickup(PlayerEvent.ItemPickupEvent event) {
        ItemStack stack = event.getStack();
        ACTrigger ach = hcPlayerPickup.get(stack.getItem());
        if(ach != null) {
            if(event.player instanceof EntityPlayerMP)
                ach.trigger((EntityPlayerMP) event.player);
        }
    }
    
    //Init
    
    private DispatcherAch() {
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    //stub method for loading
    public static void init() {
    }
    
}