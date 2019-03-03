package cn.academy.item;

import cn.academy.ACItems;
import cn.academy.misc.media.MediaManager;
import cn.lambdalib2.registry.StateEventCallback;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.*;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ACItemAdditionalRegistry {
    public static class ACLootItem
    {
        public LootFunction func = null;
        public Item item = null;
        public int weight = 10;
        public int quality = 1;
        public String name;
        public LootCondition[] conds = DEFAULT_CONDS;

        public ACLootItem(){}
        public ACLootItem(String _name, Item _item, LootFunction _func, int _weight, int _quality)
        {
            name = "academy:"+_name;
            item = _item;
            func = _func;
            weight = _weight;
            quality = _quality;
        }
        public static ACLootItem newEmpty(int _weight, int _quality)
        {
            return new ACLootItem("empty", null, null, _weight, _quality);
        }
    }
    private static final ACItemAdditionalRegistry INSTANCE = new ACItemAdditionalRegistry();
    private static final ResourceLocation[] DEFAULT_APPEARANCE = { LootTableList.CHESTS_ABANDONED_MINESHAFT,
            LootTableList.CHESTS_DESERT_PYRAMID,
            LootTableList.CHESTS_JUNGLE_TEMPLE,
            LootTableList.CHESTS_STRONGHOLD_LIBRARY,
            LootTableList.CHESTS_SIMPLE_DUNGEON };

    public static final LootCondition DEFAULT_CONDS[] = new LootCondition[0];

    private static LootPool pool;

    @StateEventCallback
    public static void preInit(FMLPreInitializationEvent evt)
    {
        MinecraftForge.EVENT_BUS.register(INSTANCE);

        LootFunction func = new LootFunction(DEFAULT_CONDS){
        @Override
        public ItemStack apply(ItemStack stack, Random rand, LootContext context)
        {
            stack.setItemDamage(rand.nextInt(4));
            return stack;
        }
    };
        ACLootItem itemFactor = new ACLootItem("induction_factor", ACItems.induction_factor, func, 10, 1);
        ACLootItem itemEmpty = ACLootItem.newEmpty(90,1);
        addLoots("name", 4, 4, itemFactor, itemEmpty);

        LootFunction funcMedia = new LootFunction(DEFAULT_CONDS) {
            @Override
            public ItemStack apply(ItemStack stack, Random rand, LootContext context )
            {
                stack.setItemDamage(rand.nextInt(MediaManager.internalMedias().length()));
                return stack;
            }
        };
        ACLootItem itemMedia = new ACLootItem("music", ACItems.media_item, funcMedia,
                10,1);
        addLoots("music", 1,1, itemMedia, itemEmpty);
    }
    public static void addLoots(String poolName, int minValue, int maxValue, ACLootItem... entries)
    {
        List<LootEntry> _entries = new ArrayList<>();
        for(ACLootItem item : entries)
        {
            LootEntry entry;
            if(item.item!=null)
            {
                entry = new LootEntryItem(item.item, item.weight, item.quality, new LootFunction[]{item.func},
                        item.conds,item.name);
            }
            else
            {
                entry = new LootEntryEmpty(item.weight, item.quality, item.conds, item.name);
            }
            _entries.add(entry);
        }
        LootEntry[] entries1 = new LootEntry[_entries.size()];
        pool = new LootPool( _entries.toArray(entries1), DEFAULT_CONDS, new RandomValueRange(minValue, maxValue), new RandomValueRange(0,0), "academy:"+poolName);
    }

    @SubscribeEvent
    public void lootLoad(LootTableLoadEvent evt) {
        for(ResourceLocation loc:DEFAULT_APPEARANCE)
        {
            if (evt.getName().equals(loc)) {
                evt.getTable().addPool(pool);
                break;
            }
        }
    }

}
