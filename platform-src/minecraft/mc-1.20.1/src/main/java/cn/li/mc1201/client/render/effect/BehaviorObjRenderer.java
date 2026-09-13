package cn.li.mc1201.client.render.effect;

import cn.li.mcbase.clj.ClojureInterop;
import cn.li.mcbase.entity.ScriptedEntitySpecAccess;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Generic OBJ-model renderer for a behavior-driven block-body entity.
 *
 * <p>The render namespace is resolved at render time from the neutral
 * {@code cn.li.mcmod.spi.entity-render-registry} (registered by content
 * modules during client init). This avoids hardcoding an AC namespace
 * in the shared Minecraft layer.</p>
 */
public final class BehaviorObjRenderer<T extends Entity> extends EntityRenderer<T> {
    private static final String REGISTRY_NS = "cn.li.mcmod.spi.entity-render-registry";
    /**
     * Cached per hook id. A single static slot pinned whichever entity
     * resolved first, so a second model-rendering entity would have been drawn
     * with the first one's namespace.
     */
    private static final java.util.Map<String, String> RENDER_NAMESPACES =
            new java.util.concurrent.ConcurrentHashMap<>();

    static {
        try {
            ClojureInterop.requireNamespace(REGISTRY_NS);
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    public BehaviorObjRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    private String hookIdOf(T entity) {
        var spec = ScriptedEntitySpecAccess.getScriptedBlockBodySpec(entity.getType());
        if (spec != null && spec.getHookId() != null && !spec.getHookId().isBlank()) {
            return spec.getHookId();
        }
        // Entities without a block-body spec (the mag hook is a scripted
        // projectile) key off their registry name instead.
        var key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key == null ? "" : key.getPath();
    }

    private String resolveRenderNamespace(T entity) {
        String hookId = hookIdOf(entity);
        if (hookId.isEmpty()) {
            return null;
        }
        String cached = RENDER_NAMESPACES.get(hookId);
        if (cached != null) {
            return cached;
        }
        try {
            Object result = ClojureInterop.invoke(REGISTRY_NS, "get-entity-render-ns", hookId);
            if (result instanceof String) {
                String ns = (String) result;
                ClojureInterop.requireNamespace(ns);
                RENDER_NAMESPACES.put(hookId, ns);
                return ns;
            }
        } catch (Throwable ignored) {
            // Content module may not have registered yet — skip rendering.
        }
        return null;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        String ns = resolveRenderNamespace(entity);
        if (ns == null) {
            return; // render namespace not yet registered — skip rendering
        }
        boolean hit = ScriptedRenderAccess.isBehaviorHit(entity);
        ClojureInterop.invoke(
            ns,
            "render!",
            entity.getId(),
            hit,
            (double) entity.tickCount,
            (double) entityYaw,
            (double) entity.getXRot(),
            (double) partialTick,
            poseStack,
            bufferSource,
            packedLight,
            OverlayTexture.NO_OVERLAY
        );
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return null;
    }
}
