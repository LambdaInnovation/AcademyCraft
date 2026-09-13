package cn.li.mcbase.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;

public final class BlockPlacementHelper {
    private BlockPlacementHelper() {
    }

    @SuppressWarnings("unchecked")
    public static BlockState withHorizontalFacing(Block block, BlockState state, BlockPlaceContext context) {
        if (context == null) {
            return state;
        }
        Property<?> prop = block.getStateDefinition().getProperty("facing");
        if (prop instanceof EnumProperty<?> enumProperty
                && enumProperty.getValueClass() == Direction.class) {
            EnumProperty<Direction> directionProperty = (EnumProperty<Direction>) enumProperty;
            Direction placedFacing = context.getHorizontalDirection().getOpposite();
            if (directionProperty.getPossibleValues().contains(placedFacing)) {
                return state.setValue(directionProperty, placedFacing);
            }
        }
        return state;
    }
}
