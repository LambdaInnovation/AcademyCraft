package cn.li.mc1201.client.render;

import cn.li.mc1201.client.render.effect.ScriptedEffectBillboardRenderer;
import cn.li.mc1201.client.render.effect.ScriptedMarkerBillboardRenderer;
import cn.li.mc1201.client.render.effect.ScriptedRayCompositeRenderer;
import cn.li.mc1201.entity.ScriptedEffectEntity;
import cn.li.mc1201.entity.ScriptedMarkerEntity;
import cn.li.mc1201.entity.ScriptedRayEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Shared renderer-id dispatch for scripted effect/marker/ray entities.
 */
public final class EffectRendererDispatcher {
    private EffectRendererDispatcher() {
    }

    public static EntityRendererProvider<ScriptedEffectEntity> pickEffectRenderer(String rendererId) {
        return ScriptedEffectBillboardRenderer::new;
    }

    public static EntityRendererProvider<ScriptedMarkerEntity> pickMarkerRenderer(String rendererId) {
        return ScriptedMarkerBillboardRenderer::new;
    }

    public static EntityRendererProvider<ScriptedRayEntity> pickRayRenderer(String rendererId) {
        return ScriptedRayCompositeRenderer::new;
    }
}
