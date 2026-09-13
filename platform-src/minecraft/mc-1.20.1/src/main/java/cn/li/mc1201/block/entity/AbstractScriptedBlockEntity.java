package cn.li.mc1201.block.entity;

import cn.li.mcbase.block.entity.IScriptedBlockEntity;

import cn.li.mcbase.block.IScriptedBlock;
import cn.li.mcbase.block.logic.ITileNbtLogic;
import cn.li.mcbase.block.logic.ITileTickLogic;
import cn.li.mcbase.block.logic.TileLogicBundle;
import cn.li.mcver.BlockEntityIo;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared scripted block-entity core for 1.20.1 loaders.
 *
 * <p>Server tick / NBT hooks dispatch through {@link IScriptedBlock#getTileLogic()}
 * bundles installed at registration time (no Clojure registry lookup on hot paths).</p>
 */
public abstract class AbstractScriptedBlockEntity extends BlockEntity implements IScriptedBlockEntity {

    private final String tileId;
    private final String blockId;

    /** Primary state: Clojure persistent map. Null until first NBT load or tick.
     *  Volatile so render-thread reads see server-thread writes on integrated server. */
    private volatile Object customState = null;

    protected AbstractScriptedBlockEntity(BlockEntityType<?> type,
                                          BlockPos pos,
                                          BlockState state,
                                          String tileId,
                                          String blockId) {
        super(type, pos, state);
        this.tileId = tileId;
        this.blockId = blockId;
    }

    public String getTileId() {
        return tileId;
    }

    public String getBlockId() {
        return blockId;
    }

    public Object getCustomState() {
        return customState;
    }

    /**
     * Pure store: no NBT-dirty marking, no client sync. Dirty flag and client sync
     * are controlled by the commit boundary (ac.block.machine.runtime/commit-state!)
     * via {@code :mark-changed?} / {@code :sync-client?} — calling those here as a
     * side effect would make every state write (including per-tick bookkeeping
     * fields that never persist) unconditionally dirty + broadcast.
     */
    public void setCustomState(Object state) {
        this.customState = state;
    }

    /** Explicit client sync entry point, invoked only via the :sync-client? commit gate. */
    public void syncCustomStateToClient() {
        if (level != null && !level.isClientSide) {
            BlockState blockState = getBlockState();
            // Re-render + neighbor updates.
            level.sendBlockUpdated(worldPosition, blockState, blockState, 3);
            // sendBlockUpdated does NOT deliver the BE data packet (no
            // ClientboundBlockEntityDataPacket in its 1.20.1 path), so the
            // client's custom state stays at its chunk-load snapshot — TESRs
            // that read it (wind generator fan, phase-gen liquid, cat engine
            // rotor) never see updates. Push the update tag to the players
            // tracking this chunk only (not every player in the dimension).
            Packet<?> pkt = getUpdatePacket();
            if (pkt != null && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.getChunkSource().chunkMap
                        .getPlayers(new ChunkPos(worldPosition), false)
                        .forEach(p -> p.connection.send(pkt));
            }
        }
    }

    private TileLogicBundle bundle() {
        Block block = getBlockState().getBlock();
        return (block instanceof IScriptedBlock scripted) ? scripted.getTileLogic() : TileLogicBundle.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ITileNbtLogic nbt = bundle().nbt;
        if (nbt != null) {
            BlockEntityIo.Io io = BlockEntityIo.ofValueInput(tag);
            nbt.readNbt(this, BlockEntityIo.asTag(io));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ITileNbtLogic nbt = bundle().nbt;
        if (nbt != null) {
            BlockEntityIo.Io io = BlockEntityIo.ofValueOutput(tag);
            nbt.writeNbt(this, BlockEntityIo.asTag(io));
        }
    }

    protected static void invokeServerTick(Level level, BlockPos pos, BlockState state, AbstractScriptedBlockEntity be) {
        if (level == null || level.isClientSide || be == null) {
            return;
        }
        ITileTickLogic tick = be.bundle().tick;
        if (tick != null) {
            tick.serverTick(level, pos, state, be);
        }
    }
}
