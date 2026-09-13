package cn.li.mc1211.item;

import cn.li.mcver.ItemData;
import clojure.lang.IFn;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Objects;

public class NbtBarItem extends Item {

    private final String currentKey;
    private final String maxKey;
    private final double defaultMaxValue;
    private final int barColor;
    private final IFn onUseCallback;

    /** Callback is a per-item closure built in item_properties.clj
     *  that encapsulates the DSL :on-use and :on-right-click handlers. */
    public NbtBarItem(Properties properties,
                      String currentKey,
                      String maxKey,
                      double defaultMaxValue,
                      int barColor,
                      IFn onUseCallback) {
        super(properties);
        this.currentKey = (currentKey == null || currentKey.isEmpty()) ? "energy" : currentKey;
        this.maxKey = (maxKey == null || maxKey.isEmpty()) ? "maxEnergy" : maxKey;
        this.defaultMaxValue = Math.max(1.0D, defaultMaxValue);
        this.barColor = barColor;
        this.onUseCallback = Objects.requireNonNull(onUseCallback, "onUseCallback");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player,
                                                   InteractionHand hand) {
        Object result = this.onUseCallback.invoke(level, player, hand);
        if (result instanceof InteractionResultHolder<?> holder) {
            @SuppressWarnings("unchecked")
            InteractionResultHolder<ItemStack> typed =
                (InteractionResultHolder<ItemStack>) holder;
            return typed;
        }
        throw new IllegalStateException("NbtBarItem use callback must return InteractionResultHolder");
    }

    private double getCurrentValue(ItemStack stack) {
        if (!ItemData.hasCustomData(stack)) {
            return 0.0D;
        }
        CompoundTag tag = ItemData.getCustomDataCopy(stack);
        return Math.max(0.0D, tag.getDouble(currentKey));
    }

    private double getMaxValue(ItemStack stack) {
        if (!ItemData.hasCustomData(stack)) {
            return defaultMaxValue;
        }
        CompoundTag tag = ItemData.getCustomDataCopy(stack);
        double taggedMax = tag.getDouble(maxKey);
        return taggedMax > 0.0D ? taggedMax : defaultMaxValue;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getCurrentValue(stack) > 0.0D;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        double max = getMaxValue(stack);
        if (max <= 0.0D) {
            return 0;
        }
        double ratio = Math.min(1.0D, getCurrentValue(stack) / max);
        return (int) Math.round(13.0D * ratio);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return barColor;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        double current = getCurrentValue(stack);
        double max = getMaxValue(stack);
        long currentDisplay = Math.round(current);
        long maxDisplay = Math.round(max);
        tooltip.add(Component.translatable(
            "tooltip.academy.energy_info", currentDisplay, maxDisplay));
    }
}
