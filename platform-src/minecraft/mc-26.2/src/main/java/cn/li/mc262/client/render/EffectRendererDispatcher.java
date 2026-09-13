package cn.li.mc262.client.render;

import cn.li.mc262.client.render.effect.BehaviorObjRenderer;
import cn.li.mc262.client.render.effect.ScriptedBlockBodyRenderer;
import cn.li.mc262.client.render.effect.ScriptedEffectBillboardRenderer;
import cn.li.mc262.client.render.effect.ScriptedMarkerBillboardRenderer;
import cn.li.mc262.client.render.effect.ScriptedRayCompositeRenderer;
import cn.li.mc262.entity.ScriptedBlockBodyEntity;
import cn.li.mc262.entity.ScriptedEffectEntity;
import cn.li.mc262.entity.ScriptedMarkerEntity;
import cn.li.mc262.entity.ScriptedRayEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Shared renderer-id dispatch for scripted effect/marker/ray entities.
 */
public final class EffectRendererDispatcher {
    private EffectRendererDispatcher() {
    }

    public static EntityRendererProvider<ScriptedEffectEntity> pickEffectRenderer(String rendererId) {
        String id = normalize(rendererId, "effect-billboard");
        return context -> new ScriptedEffectBillboardRenderer<>(context, id);
    }

    public static EntityRendererProvider<ScriptedMarkerEntity> pickMarkerRenderer(String rendererId) {
        String id = normalize(rendererId, "marker-billboard");
        return context -> new ScriptedMarkerBillboardRenderer<>(context, id);
    }

    public static EntityRendererProvider<ScriptedRayEntity> pickRayRenderer(String rendererId) {
        String id = normalize(rendererId, "ray-composite");
        return context -> new ScriptedRayCompositeRenderer<>(context, id);
    }

    /**
     * Block bodies either render their synced block model, or — for
     * behavior-driven profiles such as the silbarn — a content-owned OBJ model.
     */
    public static EntityRendererProvider<ScriptedBlockBodyEntity> pickBlockBodyRenderer(String rendererId) {
        String id = normalize(rendererId, "block-body");
        if ("block-body".equals(id)) {
            return ScriptedBlockBodyRenderer::new;
        }
        return context -> new BehaviorObjRenderer<>(context, id);
    }

    private static String normalize(String rendererId, String fallback) {
        return rendererId == null || rendererId.isBlank() ? fallback : rendererId;
    }
}
