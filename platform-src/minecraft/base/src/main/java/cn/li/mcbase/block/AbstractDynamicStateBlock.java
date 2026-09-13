package cn.li.mcbase.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared base for blocks with dynamic BlockState properties.
 * Handles ThreadLocal InitContext pattern and property registration.
 */
public abstract class AbstractDynamicStateBlock extends Block {

    protected static final Map<String, List<Property<?>>> BLOCK_PROPERTIES = new HashMap<>();
    protected static final ThreadLocal<InitContext> INIT_CONTEXT = new ThreadLocal<>();

    protected static final class InitContext {
        public final String blockId;
        public final List<Property<?>> properties;

        public InitContext(String blockId, List<Property<?>> properties) {
            this.blockId = blockId;
            this.properties = properties;
        }
    }

    protected String blockId;

    public AbstractDynamicStateBlock(BlockBehaviour.Properties properties) {
        super(properties);
        InitContext ctx = INIT_CONTEXT.get();
        if (ctx != null) {
            this.blockId = ctx.blockId;
            BLOCK_PROPERTIES.put(ctx.blockId, ctx.properties);
        }
    }

    public String getBlockId() {
        return blockId;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        InitContext ctx = INIT_CONTEXT.get();
        List<Property<?>> properties;

        if (ctx != null) {
            properties = ctx.properties;
            if (ctx.blockId != null) {
                this.blockId = ctx.blockId;
                BLOCK_PROPERTIES.put(ctx.blockId, ctx.properties);
            }
        } else {
            properties = BLOCK_PROPERTIES.getOrDefault(this.blockId, Collections.emptyList());
        }

        if (properties != null && !properties.isEmpty()) {
            builder.add(properties.toArray(new Property<?>[0]));
        }
    }

    /**
     * Clear all registered properties (primarily for testing).
     */
    public static void clearBlockProperties() {
        BLOCK_PROPERTIES.clear();
    }
}
