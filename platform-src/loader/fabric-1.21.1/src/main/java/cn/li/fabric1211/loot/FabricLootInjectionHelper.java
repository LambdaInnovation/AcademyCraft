package cn.li.fabric1211.loot;

import cn.li.mcver.ResourceLocations;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public final class FabricLootInjectionHelper {
    private FabricLootInjectionHelper() {
    }

    public static void addItemInjection(
        LootTable.Builder tableBuilder,
        String itemId,
        int weight,
        int quality,
        float minCount,
        float maxCount
    ) {
        if (tableBuilder == null || itemId == null || itemId.isEmpty()) {
            return;
        }

        Item item = BuiltInRegistries.ITEM.get(ResourceLocations.parse(itemId));
        if (item == null) {
            return;
        }

        LootPool.Builder pool = LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1.0F))
            .add(LootItem.lootTableItem(item)
                .setWeight(Math.max(weight, 1))
                .setQuality(quality)
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(minCount, maxCount))));

        tableBuilder.withPool(pool);
    }
}
