package cn.li.fabric1211.client;

import cn.li.mcver.ResourceLocations;

import cn.li.fabric1211.entity.FabricEntities;
import cn.li.fabric1211.shim.FabricClientHelper;
import cn.li.mcbase.clj.ClojureInterop;
import cn.li.mc1211.client.font.msdf.MsdfRenderTypes;
import cn.li.mc1211.client.render.EffectRendererDispatcher;
import cn.li.mc1211.client.render.ModRenderTypes;
import cn.li.mcbase.client.render.RenderProfileBootstrap;
import cn.li.mc1211.client.render.effect.ScriptedBlockBodyRenderer;
import cn.li.mcbase.entity.ScriptedEntitySpecAccess;
import cn.li.mcbase.entity.spec.ScriptedBlockBodySpec;
import cn.li.mcbase.entity.spec.ScriptedEffectSpec;
import cn.li.mcbase.entity.spec.ScriptedMarkerSpec;
import cn.li.mcbase.entity.spec.ScriptedRaySpec;
import cn.li.mcmod.ModId;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
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
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            try {
                context.register(
                        ResourceLocations.of(ModId.ID, "plasma_body"),
                        ModRenderTypes.PLASMA_BODY_FORMAT,
                        ModRenderTypes::setPlasmaBodyShader);
            } catch (IOException e) {
                LOGGER.error("Failed to register plasma_body shader", e);
            }
            try {
                context.register(
                        ResourceLocations.of(ModId.ID, "msdf_text"),
                        MsdfRenderTypes.MSDF_TEXT_FORMAT,
                        shader -> {
                            MsdfRenderTypes.setMsdfShader(shader);
                            ClojureInterop.requireNamespace("cn.li.mc1211.gui.cgui.font");
                            ClojureInterop.invoke("cn.li.mc1211.gui.cgui.font",
                                    "set-msdf-shader!", shader);
                        });
                ClojureInterop.requireNamespace("cn.li.mc1211.client.font.msdf-setup");
                ClojureInterop.invoke("cn.li.mc1211.client.font.msdf-setup", "on-shader-ready!");
            } catch (IOException e) {
                LOGGER.error("Failed to register MSDF text shader", e);
            }
            try {
                context.register(
                        ResourceLocations.of(ModId.ID, "skill_progbar"),
                        DefaultVertexFormat.POSITION_TEX,
                        ModRenderTypes::setSkillProgbarShader);
                context.register(
                        ResourceLocations.of(ModId.ID, "mono"),
                        DefaultVertexFormat.POSITION_TEX,
                        ModRenderTypes::setMonoShader);
                context.register(
                        ResourceLocations.of(ModId.ID, "cpbar_overload"),
                        DefaultVertexFormat.POSITION_TEX,
                        ModRenderTypes::setCpbarOverloadShader);
                context.register(
                        ResourceLocations.of(ModId.ID, "alpha_discard"),
                        DefaultVertexFormat.POSITION_TEX,
                        ModRenderTypes::setAlphaDiscardShader);
            } catch (IOException e) {
                LOGGER.error("Failed to register content shaders", e);
            }
        });
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
