package cn.li.mc262.item;

import clojure.lang.IFn;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class ScriptedItem extends Item {
    private final int enchantability;
    private final List<String> tooltipLines;
    private final IFn onUseCallback;

    /** Callback is a per-item closure that encapsulates DSL :on-use handlers. */
    public ScriptedItem(Properties properties, int enchantability,
                        List<String> tooltipLines, IFn onUseCallback) {
        super(enchantability > 0 ? properties.enchantable(enchantability) : properties);
        this.enchantability = Math.max(0, enchantability);
        this.tooltipLines = tooltipLines == null ? List.of() : List.copyOf(tooltipLines);
        this.onUseCallback = Objects.requireNonNull(onUseCallback, "onUseCallback");
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        Object result = this.onUseCallback.invoke(level, player, hand);
        if (result instanceof InteractionResult interactionResult) {
            return interactionResult;
        }
        throw new IllegalStateException("ScriptedItem use callback must return InteractionResult");
    }

    public int getScriptedEnchantability() {
        return enchantability;
    }

    public List<String> getTooltipLines() {
        return tooltipLines;
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
        for (String line : tooltipLines) {
            if (line != null && !line.isEmpty()) {
                tooltipAdder.accept(Component.literal(line));
            }
        }
    }
}
