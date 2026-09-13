package cn.li.mcbase.block;

import cn.li.mcbase.block.IScriptedBlock;
import cn.li.mcbase.block.logic.TileLogicBundle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Supplier;
import cn.li.mcbase.block.ScriptedBlockEntityFactory;
import cn.li.mcbase.block.ScriptedServerTickDispatcher;

/**
 * A LiquidBlock that implements EntityBlock so the scripted TESR system
 * can attach a custom renderer. Used for fluid blocks that need animated
 * overlay rendering (e.g. ImagPhase Liquid).
 */
public class ScriptedLiquidBlock extends LiquidBlock implements EntityBlock, IScriptedBlock {

    private final String blockId;
    private final String tileId;
    private final ScriptedBlockEntityFactory blockEntityFactory;
    private final ScriptedServerTickDispatcher serverTickDispatcher;
    private volatile TileLogicBundle tileLogic = TileLogicBundle.EMPTY;

    public ScriptedLiquidBlock(Supplier<? extends FlowingFluid> fluidSupplier,
                               String blockId,
                               String tileId,
                               Properties props,
                               ScriptedBlockEntityFactory blockEntityFactory,
                               ScriptedServerTickDispatcher serverTickDispatcher) {
        super(Objects.requireNonNull(fluidSupplier.get(), "fluid"), props);
        this.blockId = blockId;
        this.tileId = tileId;
        this.blockEntityFactory = Objects.requireNonNull(blockEntityFactory);
        this.serverTickDispatcher = Objects.requireNonNull(serverTickDispatcher);
    }

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

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return blockEntityFactory.create(tileId, blockId, pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> actualType) {
        if (cn.li.mcver.McAccess.isClientSide(level)) {
            return null;
        }
        return (lvl, pos, st, be) -> serverTickDispatcher.tick(lvl, pos, st, be);
    }
}
