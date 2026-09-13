package cn.li.mc262.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.List;

/**
 * Version-local SharedDynamicStateBlock that supplies MapCodec required by MC mc262.
 */
public class SharedDynamicStateBlock extends cn.li.mcbase.block.SharedDynamicStateBlock {

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
    protected MapCodec<? extends Block> codec() {
        return MapCodec.unit(this);
    }
}
