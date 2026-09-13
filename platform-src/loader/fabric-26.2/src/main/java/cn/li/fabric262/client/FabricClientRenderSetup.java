package cn.li.fabric262.client;

import cn.li.fabric262.entity.FabricEntities;
import cn.li.fabric262.shim.FabricClientHelper;
import cn.li.mcbase.clj.ClojureInterop;
import cn.li.mc262.client.render.EffectRendererDispatcher;
import cn.li.mcbase.client.render.RenderProfileBootstrap;
import cn.li.mc262.client.render.effect.ScriptedBlockBodyRenderer;
import cn.li.mcbase.entity.ScriptedEntitySpecAccess;
import cn.li.mcbase.entity.spec.ScriptedBlockBodySpec;
import cn.li.mcbase.entity.spec.ScriptedEffectSpec;
import cn.li.mcbase.entity.spec.ScriptedMarkerSpec;
import cn.li.mcbase.entity.spec.ScriptedRaySpec;
import cn.li.mcmod.ModId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.entity.EntityType;

import java.io.IOException;

/**
 * Fabric entity renderer / shader registration for scripted runtime entities.
 */
public final class FabricClientRenderSetup {
    private static final Logger LOGGER = LogManager.getLogger();

    private FabricClientRenderSetup() {
    }

    public static void registerClientHooks() {
        // Fabric 26.2 removed CoreShaderRegistrationCallback. Shader
        // pipelines are declared through the vanilla render-pipeline JSON.
    }

    public static void registerParticleProviders() {
        // Content modules register their own particle providers during client init.
    }

    public static void registerEntityRenderers() {
        RenderProfileBootstrap.runContentClientInitHooks();
        registerEffectRenderer();
        registerProjectileRenderer();
        registerRayRenderer();
        registerMarkerRenderer();
        registerBlockBodyRenderer();
    }

    private static void registerEffectRenderer() {
        EntityType<?> effectType = FabricEntities.getEntityType("scripted-effect");
        if (effectType == null) {
            return;
        }
        ScriptedEffectSpec effectSpec = ScriptedEntitySpecAccess.getScriptedEffectSpec(effectType);
        String rendererId = effectSpec == null || effectSpec.getRendererId() == null || effectSpec.getRendererId().isBlank()
                ? "effect-billboard"
                : effectSpec.getRendererId();

        FabricClientHelper.registerEntityRenderer(effectType, EffectRendererDispatcher.pickEffectRenderer(rendererId));
    }

    private static void registerProjectileRenderer() {
        EntityType<?> projectileType = FabricEntities.getEntityType("scripted-projectile");
        if (projectileType == null) {
            return;
        }
        @SuppressWarnings({"rawtypes", "unchecked"})
        EntityRendererProvider<?> provider = (EntityRendererProvider) (context -> new ThrownItemRenderer(context));
        FabricClientHelper.registerEntityRenderer(projectileType, provider);
    }

    private static void registerRayRenderer() {
        EntityType<?> rayType = FabricEntities.getEntityType("scripted-ray");
        if (rayType == null) {
            return;
        }
        ScriptedRaySpec raySpec = ScriptedEntitySpecAccess.getScriptedRaySpec(rayType);
        String rendererId = raySpec == null || raySpec.getRendererId() == null || raySpec.getRendererId().isBlank()
                ? "ray-composite"
                : raySpec.getRendererId();
        FabricClientHelper.registerEntityRenderer(rayType, EffectRendererDispatcher.pickRayRenderer(rendererId));
    }

    private static void registerMarkerRenderer() {
        EntityType<?> markerType = FabricEntities.getEntityType("scripted-marker");
        if (markerType == null) {
            return;
        }
        ScriptedMarkerSpec markerSpec = ScriptedEntitySpecAccess.getScriptedMarkerSpec(markerType);
        String rendererId = markerSpec == null || markerSpec.getRendererId() == null || markerSpec.getRendererId().isBlank()
                ? "marker-billboard"
                : markerSpec.getRendererId();

        FabricClientHelper.registerEntityRenderer(markerType, EffectRendererDispatcher.pickMarkerRenderer(rendererId));
    }

    private static void registerBlockBodyRenderer() {
        EntityType<?> blockBodyType = FabricEntities.getEntityType("scripted-block-body");
        if (blockBodyType == null) {
            return;
        }
        ScriptedBlockBodySpec blockBodySpec = ScriptedEntitySpecAccess.getScriptedBlockBodySpec(blockBodyType);
        String rendererId = blockBodySpec == null || blockBodySpec.getRendererId() == null || blockBodySpec.getRendererId().isBlank()
                ? "block-body"
                : blockBodySpec.getRendererId();

        if ("block-body".equals(rendererId)) {
            FabricClientHelper.registerEntityRenderer(blockBodyType, ScriptedBlockBodyRenderer::new);
        }
    }
}
