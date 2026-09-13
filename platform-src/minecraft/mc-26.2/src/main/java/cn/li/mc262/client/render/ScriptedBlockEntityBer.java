package cn.li.mc262.client.render;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import cn.li.mcbase.clj.ClojureInterop;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ScriptedBlockEntityBer<T extends BlockEntity>
        implements BlockEntityRenderer<T, ScriptedBlockEntityBer.State<T>> {
    private static volatile IFn renderTileEntityFn;

    public ScriptedBlockEntityBer(BlockEntityRendererProvider.Context context) {
    }

    private static IFn renderFn() {
        IFn fn = renderTileEntityFn;
        if (fn == null) {
            synchronized (ScriptedBlockEntityBer.class) {
                fn = renderTileEntityFn;
                if (fn == null) {
                    ClojureInterop.requireNamespace("cn.li.mcmod.client.render.tesr-api");
                    fn = Clojure.var("cn.li.mcmod.client.render.tesr-api", "render-tile-entity");
                    renderTileEntityFn = fn;
                }
            }
        }
        return fn;
    }

    @Override
    public State<T> createRenderState() {
        return new State<>();
    }

    @Override
    public void extractRenderState(T blockEntity, State<T> state, float partialTick,
                                   Vec3 cameraPosition,
                                   ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(blockEntity, state, breakProgress);
        state.blockEntity = blockEntity;
        state.partialTick = partialTick;
    }

    @Override
    public void submit(State<T> state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        SubmitNodeRenderBufferAdapter bufferAdapter =
                new SubmitNodeRenderBufferAdapter(collector, poseStack);
        try {
            renderFn().invoke(
                    state.blockEntity,
                    state.partialTick,
                    poseStack,
                    bufferAdapter,
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY);
        } catch (Throwable error) {
            throw new IllegalStateException(
                    "Minecraft 26.2 scripted block entity renderer failed for "
                            + state.blockEntity.getBlockPos(),
                    error);
        } finally {
            bufferAdapter.finish();
        }
    }

    public static final class State<T extends BlockEntity> extends BlockEntityRenderState {
        public T blockEntity;
        public float partialTick;
    }
}
