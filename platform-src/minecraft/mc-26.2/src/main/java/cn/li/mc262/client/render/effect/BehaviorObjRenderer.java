package cn.li.mc262.client.render.effect;

import cn.li.mc262.client.render.SubmitNodeRenderBufferAdapter;
import cn.li.mcbase.entity.ScriptedEntitySpecAccess;
import cn.li.mcbase.clj.ClojureInterop;
import cn.li.mcbase.entity.spec.ScriptedBlockBodySpec;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * Generic OBJ-model renderer for behavior-driven block-body entities.
 *
 * <p>The content render namespace is resolved at render time from the neutral
 * {@code cn.li.mcmod.spi.entity-render-registry}, keyed by the entity spec's
 * hook id (falling back to the configured renderer id/profile). Geometry is
 * produced by that Clojure renderer against a {@link
 * SubmitNodeRenderBufferAdapter}, which records the vertices and replays them
 * from the 26.2 submit-node pipeline.</p>
 */
public final class BehaviorObjRenderer<T extends Entity>
        extends AbstractScriptedGeometryRenderer<T> {
    private static final String REGISTRY_NS = "cn.li.mcmod.spi.entity-render-registry";

    static {
        ClojureInterop.requireNamespace(REGISTRY_NS);
    }

    /** Cached after first successful resolution (one renderer per entity type). */
    private volatile String renderNamespace;

    public BehaviorObjRenderer(EntityRendererProvider.Context context, String rendererId) {
        super(context, rendererId);
    }

    @Override
    public void extractRenderState(T entity, ScriptedEntityRenderState<T> state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.behaviorHit = ScriptedRenderAccess.isBehaviorHit(entity);
    }

    @Override
    public void submit(ScriptedEntityRenderState<T> state, PoseStack stack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.behaviorHit) {
            String namespace = resolveRenderNamespace(state.entityType);
            if (namespace != null) {
                submitContentGeometry(namespace, state, stack, collector);
            }
        }
        super.submit(state, stack, collector, camera);
    }

    private static void submitContentGeometry(String namespace, ScriptedEntityRenderState<?> state,
                                              PoseStack stack, SubmitNodeCollector collector) {
        SubmitNodeRenderBufferAdapter bufferAdapter =
                new SubmitNodeRenderBufferAdapter(collector, stack);
        try {
            ClojureInterop.invoke(
                    namespace,
                    "render!",
                    state.entityId,
                    Boolean.FALSE,
                    (double) state.ageTicks,
                    (double) state.yRot,
                    (double) state.partialTick,
                    stack,
                    bufferAdapter,
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY);
        } finally {
            bufferAdapter.finish();
        }
    }

    private String resolveRenderNamespace(EntityType<?> entityType) {
        String cached = renderNamespace;
        if (cached != null) {
            return cached;
        }
        try {
            Object result = ClojureInterop.invoke(REGISTRY_NS, "get-entity-render-ns",
                    renderProfile(entityType));
            if (result instanceof String namespace && !namespace.isBlank()) {
                ClojureInterop.requireNamespace(namespace);
                renderNamespace = namespace;
                return namespace;
            }
        } catch (Throwable ignored) {
            // Content module may not have registered its renderer yet — retry next frame.
        }
        return null;
    }

    /**
     * Hook id of the entity spec, or the configured renderer id when the spec is
     * unavailable (both are registry keys in the hook -> render-ns registry).
     */
    private String renderProfile(EntityType<?> entityType) {
        try {
            ScriptedBlockBodySpec spec = ScriptedEntitySpecAccess.getScriptedBlockBodySpec(entityType);
            if (spec != null && !spec.getHookId().isBlank()) {
                return spec.getHookId();
            }
        } catch (RuntimeException ignored) {
            // Spec accessor not installed — fall back to the configured profile.
        }
        return configuredRendererId;
    }
}
