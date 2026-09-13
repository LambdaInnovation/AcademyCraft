package cn.li.mc262.client.render;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ScriptedBlockEntityBerProvider {
    private ScriptedBlockEntityBerProvider() {}

    public static <T extends BlockEntity>
    BlockEntityRendererProvider<T, ScriptedBlockEntityBer.State<T>> provider() {
        return context -> new ScriptedBlockEntityBer<>(context);
    }
}
