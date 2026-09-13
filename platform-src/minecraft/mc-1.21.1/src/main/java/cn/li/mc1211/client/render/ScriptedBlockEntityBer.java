package cn.li.mc1211.client.render;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import cn.li.mcbase.clj.ClojureInterop;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Explicit Java {@link BlockEntityRenderer} for scripted tiles.
 * <p>
 * Clojure {@code reify} on {@code BlockEntityRenderer} can fail to match the
 * generic interface method at runtime (no crash, {@code render} simply never
 * runs). This class forwards to {@code cn.li.mcmod.client.render.tesr-api/render-tile-entity}.
 */
public final class ScriptedBlockEntityBer implements BlockEntityRenderer<BlockEntity> {

    private static volatile IFn renderTileEntityFn;

    private static IFn renderFn() {
        IFn f = renderTileEntityFn;
        if (f == null) {
            synchronized (ScriptedBlockEntityBer.class) {
                f = renderTileEntityFn;
                if (f == null) {
                    ClojureInterop.requireNamespace("cn.li.mcmod.client.render.tesr-api");
                    f = Clojure.var("cn.li.mcmod.client.render.tesr-api", "render-tile-entity");
                    renderTileEntityFn = f;
                }
            }
        }
        return f;
    }

    @Override
    public void render(BlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        try {
            renderFn().invoke(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        } catch (Throwable t) {
            System.err.println("[academy] ScriptedBlockEntityBer.render failed:");
            t.printStackTrace();
        }
    }
}
