package cn.li.mc1211.client.render;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Java skeleton for {@link BlockEntityRendererProvider} — returns {@link ScriptedBlockEntityBer}.
 */
public final class ScriptedBlockEntityBerProvider implements BlockEntityRendererProvider<BlockEntity> {
    @Override
    public BlockEntityRenderer<BlockEntity> create(BlockEntityRendererProvider.Context ctx) {
        return new ScriptedBlockEntityBer();
    }
}
