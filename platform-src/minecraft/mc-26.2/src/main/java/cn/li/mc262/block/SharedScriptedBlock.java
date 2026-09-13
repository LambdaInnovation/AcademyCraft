package cn.li.mc262.block;

import cn.li.mcbase.block.BlockPlacementHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.List;
import cn.li.mcbase.block.DynamicBlockPropertyRegistry;
import cn.li.mcbase.block.ScriptedBlockEntityFactory;
import cn.li.mcbase.block.ScriptedRenderShapeResolver;
import cn.li.mcbase.block.ScriptedServerTickDispatcher;

/**
 * Loader-agnostic scripted carrier block with injected platform behavior.
 */
public class SharedScriptedBlock extends ScriptedCarrierBlockBase {

    private final ScriptedBlockEntityFactory blockEntityFactory;
    private final ScriptedServerTickDispatcher serverTickDispatcher;
    private final ScriptedRenderShapeResolver renderShapeResolver;

    public static SharedScriptedBlock create(String blockId,
                                             String tileId,
                                             List<Property<?>> properties,
                                             BlockBehaviour.Properties behaviourProperties,
                                             ScriptedBlockEntityFactory blockEntityFactory,
                                             ScriptedServerTickDispatcher serverTickDispatcher,
                                             ScriptedRenderShapeResolver renderShapeResolver) {
        return DynamicBlockPropertyRegistry.withInitContext(
            SharedScriptedBlock.class,
            blockId,
            properties,
            () -> new SharedScriptedBlock(
                blockId,
                tileId,
                behaviourProperties,
                blockEntityFactory,
                serverTickDispatcher,
                renderShapeResolver
            )
        );
    }

    public static SharedScriptedBlock create(String blockId,
                                             List<Property<?>> properties,
                                             BlockBehaviour.Properties behaviourProperties,
                                             ScriptedBlockEntityFactory blockEntityFactory,
                                             ScriptedServerTickDispatcher serverTickDispatcher,
                                             ScriptedRenderShapeResolver renderShapeResolver) {
        return create(
            blockId,
            blockId,
            properties,
            behaviourProperties,
            blockEntityFactory,
            serverTickDispatcher,
            renderShapeResolver
        );
    }

    public SharedScriptedBlock(String blockId,
                               String tileId,
                               Properties props,
                               ScriptedBlockEntityFactory blockEntityFactory,
                               ScriptedServerTickDispatcher serverTickDispatcher,
                               ScriptedRenderShapeResolver renderShapeResolver) {
        super(blockId, tileId, props);
        this.blockEntityFactory = blockEntityFactory;
        this.serverTickDispatcher = serverTickDispatcher;
        this.renderShapeResolver = renderShapeResolver;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        List<Property<?>> properties = DynamicBlockPropertyRegistry.resolveForDefinition(SharedScriptedBlock.class, blockId);
        if (properties != null && !properties.isEmpty()) {
            builder.add(properties.toArray(new Property<?>[0]));
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return BlockPlacementHelper.withHorizontalFacing(this, this.defaultBlockState(), context);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        if (renderShapeResolver == null) {
            return RenderShape.MODEL;
        }
        RenderShape resolved = renderShapeResolver.resolve(blockId, state);
        return resolved != null ? resolved : RenderShape.MODEL;
    }

    @Override
    protected BlockEntity createScriptedBlockEntity(BlockPos pos, BlockState state) {
        if (blockEntityFactory == null) {
            return null;
        }
        return blockEntityFactory.create(tileId, blockId, pos, state);
    }

    @Override
    protected void serverTickScripted(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (serverTickDispatcher != null) {
            serverTickDispatcher.tick(level, pos, state, blockEntity);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof Container container) {
            Containers.dropContents(level, pos, container);
        }
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return super.propagatesSkylightDown(state);
    }

    @Override
    protected int getLightDampening(BlockState state) {
        return super.getLightDampening(state);
    }

    /**
     * Dynamic light emission for working machines (NeoForge {@code IBlockExtension}).
     */
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        for (Property<?> prop : state.getProperties()) {
            if ("frame".equals(prop.getName()) && prop instanceof IntegerProperty integerProperty) {
                int frame = state.getValue(integerProperty);
                if (frame > 0) {
                    return 6;
                }
                break;
            }
        }
        return state.getLightEmission();
    }

    /** Alias used by some bootstrap call sites. */
    public String getScriptedBlockId() {
        return blockId;
    }
}
