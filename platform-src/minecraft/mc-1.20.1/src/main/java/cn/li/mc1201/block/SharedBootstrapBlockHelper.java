package cn.li.mc1201.block;

import cn.li.mcbase.block.SharedDynamicStateBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import cn.li.mcbase.block.ScriptedBlockEntityFactory;
import cn.li.mcbase.block.ScriptedServerTickDispatcher;

/**
 * Shared loader-agnostic block/bootstrap helpers used by Forge and Fabric shims.
 */
public final class SharedBootstrapBlockHelper {

    private SharedBootstrapBlockHelper() {
    }

    @FunctionalInterface
    public interface ScriptedBlockEntityTypeFactory<T extends BlockEntity> {
        T create(BlockEntityType<T> type, BlockPos pos, BlockState state, String tileId, String blockId);
    }

    @FunctionalInterface
    public interface ScriptedBlockEntityTypeRegistrar<T extends BlockEntity> {
        void register(String tileId, BlockEntityType<T> type);
    }

    public static BlockBehaviour.Properties createStoneProperties() {
        return BlockBehaviour.Properties.copy(Blocks.STONE);
    }

    public static BlockBehaviour.Properties createBlockProperties(String material,
                                                                   float hardness,
                                                                   float resistance,
                                                                   boolean requiresCorrectTool) {
        BlockBehaviour.Properties props = "metal".equals(material)
            ? BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
            : BlockBehaviour.Properties.copy(Blocks.STONE);
        props = props.strength(hardness, resistance);
        return requiresCorrectTool ? props.requiresCorrectToolForDrops() : props;
    }

    /**
     * Apply a fluid's luminosity as block light emission. A FluidType's light
     * level drives fog and entity lighting only, so a glowing fluid still needs
     * its LiquidBlock to emit — vanilla lava does the same via its own
     * Properties. A level of 0 leaves the properties untouched.
     */
    public static BlockBehaviour.Properties withLightLevel(BlockBehaviour.Properties props, int lightLevel) {
        return lightLevel > 0 ? props.lightLevel(state -> lightLevel) : props;
    }

    public static BlockBehaviour.Properties carrierBlockProperties(BlockBehaviour.Properties base) {
        BlockBehaviour.StatePredicate alwaysFalse = new BlockBehaviour.StatePredicate() {
            @Override
            public boolean test(BlockState state, BlockGetter level, BlockPos pos) {
                return false;
            }
        };
        return base.noOcclusion()
            .forceSolidOff()
            .isViewBlocking(alwaysFalse)
            .isSuffocating(alwaysFalse)
            .isRedstoneConductor(alwaysFalse);
    }

    public static Block createCarrierScriptedDynamicBlock(String blockId,
                                                          String tileId,
                                                          List<Property<?>> properties,
                                                          BlockBehaviour.Properties blockProperties,
                                                          ScriptedBlockEntityFactory blockEntityFactory,
                                                          ScriptedServerTickDispatcher serverTickDispatcher) {
        return SharedScriptedBlock.create(
            blockId,
            tileId,
            new ArrayList<>(properties),
            blockProperties,
            blockEntityFactory,
            serverTickDispatcher,
            (resolvedBlockId, state) -> ScriptedRenderShapes.resolveDefault(resolvedBlockId)
        );
    }

    public static Block createDynamicStateBlock(String blockId,
                                                List<Property<?>> properties,
                                                BlockBehaviour.Properties blockProperties) {
        return SharedDynamicStateBlock.create(blockId, new ArrayList<>(properties), blockProperties);
    }

    public static Block createCarrierScriptedBlock(String blockId,
                                                   String tileId,
                                                   BlockBehaviour.Properties blockProperties,
                                                   ScriptedBlockEntityFactory blockEntityFactory,
                                                   ScriptedServerTickDispatcher serverTickDispatcher) {
        return new SharedScriptedBlock(
            blockId,
            tileId,
            blockProperties,
            blockEntityFactory,
            serverTickDispatcher,
            (resolvedBlockId, state) -> ScriptedRenderShapes.resolveDefault(resolvedBlockId)
        );
    }

    public static Block createPlainBlock(BlockBehaviour.Properties blockProperties) {
        return new Block(blockProperties);
    }

    public static Item createBlockItem(Block block) {
        return new BlockItem(block, new Item.Properties());
    }

    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity> BlockEntityType<T> createScriptedBlockEntityType(
        String tileId,
        List<Block> blocks,
        Function<Block, String> blockIdResolver,
        ScriptedBlockEntityTypeFactory<T> factory,
        ScriptedBlockEntityTypeRegistrar<T> registrar
    ) {
        Block[] blockArray = blocks.toArray(new Block[0]);
        BlockEntityType<T>[] typeHolder = (BlockEntityType<T>[]) new BlockEntityType<?>[1];
        BlockEntityType<T> beType = BlockEntityType.Builder.of(
            (pos, state) -> {
                Block blockInst = state.getBlock();
                String blockId = blockIdResolver.apply(blockInst);
                if (blockId == null) {
                    blockId = tileId;
                }
                return factory.create(typeHolder[0], pos, state, tileId, blockId);
            },
            blockArray
        ).build(null);
        typeHolder[0] = beType;
        registrar.register(tileId, beType);
        return beType;
    }
}
