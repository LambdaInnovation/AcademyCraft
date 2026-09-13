package cn.li.mc262.block;


import cn.li.mcbase.block.IScriptedBlock;
import cn.li.mcbase.block.logic.TileLogicBundle;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared base for scripted carrier blocks across platform loaders.
 *
 * <p>Provides common identity fields ({@code blockId}/{@code tileId}) and
 * server ticker wiring while leaving platform-specific block-entity creation
 * and cast logic to subclasses.</p>
 */
public abstract class ScriptedCarrierBlockBase extends BaseEntityBlock implements IScriptedBlock {

    protected final String blockId;
    protected final String tileId;
    private volatile TileLogicBundle tileLogic = TileLogicBundle.EMPTY;

    protected ScriptedCarrierBlockBase(String blockId, String tileId, Properties props) {
        super(props);
        this.blockId = blockId;
        this.tileId = tileId;
    }

    /**
     * Code-registered scripted blocks are not reconstructed from codecs;
     * {@link MapCodec#unit} satisfies the abstract {@code codec()} contract.
     */
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return MapCodec.unit(this);
    }

    // =========================================================================
    // IScriptedBlock
    // =========================================================================

    @Override
    public String getBlockId() {
        return blockId;
    }

    @Override
    public String getTileId() {
        return tileId;
    }

    @Override
    public TileLogicBundle getTileLogic() {
        return tileLogic;
    }

    @Override
    public void installTileLogic(TileLogicBundle bundle) {
        this.tileLogic = bundle != null ? bundle : TileLogicBundle.EMPTY;
    }

    protected abstract BlockEntity createScriptedBlockEntity(BlockPos pos, BlockState state);

    protected abstract void serverTickScripted(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity);

    @Override
    public final BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return createScriptedBlockEntity(pos, state);
    }

    @Override
    public final <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return (lvl, pos, st, be) -> serverTickScripted(lvl, pos, st, be);
    }
}
