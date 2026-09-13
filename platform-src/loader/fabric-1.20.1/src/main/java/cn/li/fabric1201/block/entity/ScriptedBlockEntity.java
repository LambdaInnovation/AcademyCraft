package cn.li.fabric1201.block.entity;

import cn.li.mc1201.block.entity.AbstractScriptedBlockEntity;
import cn.li.mc1201.block.entity.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ScriptedBlockEntity extends AbstractScriptedBlockEntity {

    /**
     * Register this entity type via the shared registry.
     */
    public static void registerType(String tileId, BlockEntityType<ScriptedBlockEntity> type) {
        BlockEntityRegistry.registerType(tileId, type);
    }

    /**
     * Retrieve a registered entity type via the shared registry.
     */
    public static BlockEntityType<ScriptedBlockEntity> getType(String tileId) {
        return (BlockEntityType<ScriptedBlockEntity>) BlockEntityRegistry.getType(tileId);
    }

    public ScriptedBlockEntity(BlockEntityType<ScriptedBlockEntity> type,
                               BlockPos pos,
                               BlockState state,
                               String tileId,
                               String blockId) {
        super(type, pos, state, tileId, blockId);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ScriptedBlockEntity blockEntity) {
        invokeServerTick(level, pos, state, blockEntity);
    }
}
