package cn.li.mc262.item;

import cn.li.mc262.bridge.NbtAccess;
import cn.li.mcver.ItemData;
import clojure.lang.IFn;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.function.Consumer;

public class NbtBarItem extends Item {

    private final String currentKey;
    private final String maxKey;
    private final double defaultMaxValue;
    private final int barColor;
    private final IFn onUseCallback;

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
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        Object result = this.onUseCallback.invoke(level, player, hand);
        if (result instanceof InteractionResult interactionResult) {
            return interactionResult;
        }
        throw new IllegalStateException("NbtBarItem use callback must return InteractionResult");
    }

    private double getCurrentValue(ItemStack stack) {
        if (!ItemData.hasCustomData(stack)) {
            return 0.0D;
        }
        CompoundTag tag = ItemData.getCustomDataCopy(stack);
        return Math.max(0.0D, NbtAccess.getDouble(tag, currentKey));
    }

    private double getMaxValue(ItemStack stack) {
        if (!ItemData.hasCustomData(stack)) {
            return defaultMaxValue;
        }
        CompoundTag tag = ItemData.getCustomDataCopy(stack);
        double taggedMax = NbtAccess.getDouble(tag, maxKey);
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

    public String getCurrentKey() {
        return currentKey;
    }

    public String getMaxKey() {
        return maxKey;
    }

    public double getDefaultMaxValue() {
        return defaultMaxValue;
    }

    public IFn getOnUseCallback() {
        return onUseCallback;
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                Item.TooltipContext context,
                                TooltipDisplay display,
                                Consumer<Component> tooltipAdder,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltipAdder, flag);
        double current = getCurrentValue(stack);
        double max = getMaxValue(stack);
        long currentDisplay = Math.round(current);
        long maxDisplay = Math.round(max);
        tooltipAdder.accept(Component.translatable(
            "tooltip.academy.energy_info", currentDisplay, maxDisplay));
    }
}
