package cn.academy.achievement;

import cn.academy.ability.Category;
import cn.academy.ability.Skill;
import cn.academy.datapart.AbilityData;
import cn.academy.event.ability.LevelChangeEvent;
import cn.academy.event.ability.SkillLearnEvent;
import cn.academy.achievement.aches.*;
import cn.academy.event.MatterUnitHarvestEvent;
import cn.academy.misc.achievement.aches.*;
import net.minecraftforge.fml.common.FMLCommonHandler;
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
    
    private final HashMap<Item, HashSet<AchEvItemCrafted>> hcItemCrafted = new HashMap<Item, HashSet<AchEvItemCrafted>>();
    
    public void rgItemCrafted(Item item, AchEvItemCrafted ach) {
        if (hcItemCrafted.containsKey(item))
            hcItemCrafted.get(item).add(ach);
        else {
            HashSet<AchEvItemCrafted> set = new HashSet<AchEvItemCrafted>();
            set.add(ach);
            hcItemCrafted.put(item, set);
        }
        hcItemCrafted.get(item).add(ach);
    }
    
    public void urItemCrafted(Item item, AchEvItemCrafted ach) {
        HashSet<AchEvItemCrafted> set = hcItemCrafted.get(item);
        if (set != null)
            set.remove(ach);
    }
    
    @SubscribeEvent
    public void onItemCrafted(ItemCraftedEvent event) {
        HashSet<AchEvItemCrafted> set = hcItemCrafted.get(event.crafting.getItem());
        if (set != null)
            for (AchEvItemCrafted a : set)
                if (a.accept(event))
                    event.player.triggerAchievement(a);
    }
    
    
    //cn.academy.event.ability.LevelChangeEvent
    
    private final HashMap<Category, AchEvLevelChange[]> hcLevelChange = new HashMap<>();
    
    public void rgLevelChange(Category cat, int lv, AchEvLevelChange ach) {
        if (hcLevelChange.containsKey(cat))
            hcLevelChange.get(cat)[lv - 1] = ach;
        else {
            AchEvLevelChange[] arr = new AchEvLevelChange[5];
            arr[lv - 1] = ach;
            hcLevelChange.put(cat, arr);
        }
    }
    
    public void urLevelChange(Category cat, int lv) {
        AchEvLevelChange[] arr = hcLevelChange.get(cat);
        if (arr != null)
            arr[lv - 1] = null;
    }
    
    @SubscribeEvent
    public void onLevelChange(LevelChangeEvent event) {
        AbilityData data = AbilityData.get(event.player);
        if (data.hasCategory()) {
            int xlv = data.getLevel() - 1;
            AchEvLevelChange[] arr = hcLevelChange.get(data.getCategory());
            if (arr != null && xlv >= 0 && arr[xlv] != null && arr[xlv].accept(event))
                event.player.triggerAchievement(arr[xlv]);
        }
    }
    
    
    //cn.academy.event.MatterUnitHarvestEvent
    
    private final HashMap<Block, AchEvMatterUnitHarvest> hcMatterUnitHarvest = new HashMap<Block, AchEvMatterUnitHarvest>();
    
    public void rgMatterUnitHarvest(Block blo, AchEvMatterUnitHarvest ach) {
        hcMatterUnitHarvest.put(blo, ach);
    }
    
    public void urMatterUnitHarvest(Block blo) {
        hcMatterUnitHarvest.remove(blo);
    }
    
    @SubscribeEvent
    public void onMatterUnitHarvest(MatterUnitHarvestEvent event) {
        AchEvMatterUnitHarvest ach = hcMatterUnitHarvest.get(event.mat.block);
        if (ach != null && ach.accept(event))
            event.player.triggerAchievement(ach);
    }
    
    
    //cn.academy.event.ability.SkillLearnEvent
    
    private final HashMap<Skill, AchEvSkillLearn> hcSkillLearn = new HashMap<Skill, AchEvSkillLearn>();
    
    public void rgSkillLearn(Skill skill, AchEvSkillLearn ach) {
        hcSkillLearn.put(skill, ach);
    }
    
    public void urSkillLearn(Skill skill) {
        hcSkillLearn.remove(skill);
    }
    
    @SubscribeEvent
    public void onSkillLearn(SkillLearnEvent event) {
        AchEvSkillLearn ach = hcSkillLearn.get(event.skill);//CHANGED HERE
        if (ach != null && ach.accept(event))
            event.player.triggerAchievement(ach);
    }
    
    //net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemPickupEvent
    
    private final Map<Item, AchEvItemPickup> hcPlayerPickup = new HashMap();
    
    public void rgPlayerPickup(ItemStack stack, AchEvItemPickup ach) {
        hcPlayerPickup.put(stack.getItem(), ach);
    }
    
    @SubscribeEvent
    public void onPlayerPickup(PlayerEvent.ItemPickupEvent event) {
        ItemStack stack = event.pickedUp.getEntityItem();
        AchEvItemPickup ach = hcPlayerPickup.get(stack.getItem());
        if(ach != null && ach.accept(event)) {
            event.player.triggerAchievement(ach);
        }
    }
    
    //Init
    
    private DispatcherAch() {
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    //stub method for loading
    public static void init() {
    }
    
}