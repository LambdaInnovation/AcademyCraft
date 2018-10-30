package cn.academy.item;

import cn.academy.ACItems;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.*;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;

public class ACItemAdditionalRegistry {

    private static final ACItemAdditionalRegistry INSTANCE = new ACItemAdditionalRegistry();
    private static final ResourceLocation[] factorAppearance = { LootTableList.CHESTS_ABANDONED_MINESHAFT,
            LootTableList.CHESTS_DESERT_PYRAMID,
            LootTableList.CHESTS_JUNGLE_TEMPLE,
            LootTableList.CHESTS_STRONGHOLD_LIBRARY,
            LootTableList.CHESTS_SIMPLE_DUNGEON };

    //public static final ResourceLocation INDUCTION_FACTOR = LootTableList.register(new ResourceLocation("academy", "inject/induction_factor"));;
    private static final LootPool pool;
    static{
        LootCondition conds[] = new LootCondition[0];
        LootFunction func = new LootFunction(conds)
        {
            @Override
            public ItemStack apply(ItemStack stack, Random rand, LootContext context)
            {
                stack.setItemDamage(rand.nextInt(4));
                return stack;
            }
        };
        LootEntry entries[] = new LootEntry[]{
                new LootEntryItem(ACItems.induction_factor, 10, 1,
                        new LootFunction[]{func}, conds,"academy:induction_factor"),
                new LootEntryEmpty(90, 1, conds, "empty")
        };
        pool = new LootPool(entries, conds, new RandomValueRange(4, 4), new RandomValueRange(0,0), "academy:main");
    }

    private ACItemAdditionalRegistry()
    {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void lootLoad(LootTableLoadEvent evt) {
        for(ResourceLocation loc:factorAppearance)
        {
            if (evt.getName().equals(loc)) {
                evt.getTable().addPool(pool);
                break;
            }
        }
    }

}
