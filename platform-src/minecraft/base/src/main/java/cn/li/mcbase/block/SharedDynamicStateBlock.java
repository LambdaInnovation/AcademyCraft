package cn.li.mcbase.block;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import java.util.List;

/**
 * Loader-agnostic dynamic block (no BlockEntity).
 */
public class SharedDynamicStateBlock extends AbstractDynamicStateBlock {

    public static SharedDynamicStateBlock create(String blockId,
                                                 List<Property<?>> properties,
                                                 BlockBehaviour.Properties behaviourProperties) {
        INIT_CONTEXT.set(new InitContext(blockId, properties));
        try {
            return new SharedDynamicStateBlock(behaviourProperties);
        } finally {
            INIT_CONTEXT.remove();
        }
    }

    public SharedDynamicStateBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return BlockPlacementHelper.withHorizontalFacing(this, this.defaultBlockState(), context);
    }
}
