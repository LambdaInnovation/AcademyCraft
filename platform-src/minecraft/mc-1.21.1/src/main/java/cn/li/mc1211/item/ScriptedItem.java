package cn.li.mc1211.item;

import clojure.lang.IFn;
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

public class ScriptedItem extends Item {
    private final int enchantability;
    private final List<String> tooltipLines;
    private final IFn onUseCallback;

    /** Callback is a per-item closure built in item_properties.clj
     *  that encapsulates the DSL :on-use and :on-right-click handlers. */
    public ScriptedItem(Properties properties, int enchantability,
                        List<String> tooltipLines, IFn onUseCallback) {
        super(properties);
        this.enchantability = Math.max(0, enchantability);
        this.tooltipLines = tooltipLines == null ? List.of() : List.copyOf(tooltipLines);
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
        throw new IllegalStateException("ScriptedItem use callback must return InteractionResultHolder");
    }

    @Override
    public int getEnchantmentValue() {
        return enchantability;
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                Item.TooltipContext context,
                                List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        for (String line : tooltipLines) {
            if (line != null && !line.isEmpty()) {
                tooltip.add(Component.literal(line));
            }
        }
    }
}
