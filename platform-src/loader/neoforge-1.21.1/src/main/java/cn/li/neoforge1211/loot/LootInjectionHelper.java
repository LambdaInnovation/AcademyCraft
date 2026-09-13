package cn.li.neoforge1211.loot;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.event.LootTableLoadEvent;

public final class LootInjectionHelper {
    private LootInjectionHelper() {
    }

    public static void addItemInjection(
        LootTableLoadEvent event,
        String itemId,
        int weight,
        int quality,
        float minCount,
        float maxCount
    ) {
        if (event == null || itemId == null || itemId.isEmpty()) {
            return;
        }

        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
        if (item == null) {
            return;
        }

        LootPool.Builder pool = LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1.0F))
            .add(LootItem.lootTableItem(item)
                .setWeight(Math.max(weight, 1))
                .setQuality(quality)
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(minCount, maxCount))));

        event.getTable().addPool(pool.build());
    }
}
