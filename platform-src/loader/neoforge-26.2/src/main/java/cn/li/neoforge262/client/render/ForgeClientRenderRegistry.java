package cn.li.neoforge262.client.render;

import cn.li.mc262.client.render.EffectRendererDispatcher;
import cn.li.mcbase.client.render.RenderProfileBootstrap;
import cn.li.mc262.entity.ScriptedBlockBodyEntity;
import cn.li.mc262.entity.ScriptedEffectEntity;
import cn.li.mc262.entity.ScriptedMarkerEntity;
import cn.li.mc262.entity.ScriptedProjectileEntity;
import cn.li.mc262.entity.ScriptedRayEntity;
import cn.li.mcbase.clj.ClojureInterop;
import cn.li.mcbase.entity.spec.ScriptedBlockBodySpec;
import cn.li.mcbase.entity.spec.ScriptedMarkerSpec;
import cn.li.mcbase.entity.spec.ScriptedRaySpec;
import cn.li.neoforge262.entity.ModEntities;
import clojure.java.api.Clojure;
import clojure.lang.IFn;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Client entity and block-entity renderer registration for NeoForge 26.2. */
public final class ForgeClientRenderRegistry {
    private static final Logger LOGGER = LogManager.getLogger();

    private ForgeClientRenderRegistry() {
    }

    public static void registerEntityAndBlockRenderers(EntityRenderersEvent.RegisterRenderers event) {
        RenderProfileBootstrap.runContentClientInitHooks();

        for (String registryName : ModEntities.getScriptedEffectRegistryNames()) {
            EntityType<ScriptedEffectEntity> effectType =
                    ModEntities.getEntityType(registryName, ScriptedEffectEntity.class);
            if (effectType == null) {
                continue;
            }
            String rendererId = "effect-billboard";
            if (ModEntities.getScriptedEffectSpec(registryName) != null) {
                rendererId = ModEntities.getScriptedEffectSpec(registryName).getRendererId();
            }
            event.registerEntityRenderer(effectType, EffectRendererDispatcher.pickEffectRenderer(rendererId));
        }

        EntityType<ScriptedProjectileEntity> magHook =
                ModEntities.getEntityType("entity_mag_hook", ScriptedProjectileEntity.class);
        if (magHook != null) {
            event.registerEntityRenderer(magHook, ThrownItemRenderer::new);
        }

        for (String registryName : ModEntities.getScriptedRayRegistryNames()) {
            EntityType<ScriptedRayEntity> rayType =
                    ModEntities.getEntityType(registryName, ScriptedRayEntity.class);
            if (rayType == null) {
                continue;
            }
            ScriptedRaySpec raySpec = ModEntities.getScriptedRaySpec(registryName);
            String rendererId = raySpec == null ? "ray-composite" : raySpec.getRendererId();
            event.registerEntityRenderer(rayType, EffectRendererDispatcher.pickRayRenderer(rendererId));
        }

        for (String registryName : ModEntities.getScriptedMarkerRegistryNames()) {
            EntityType<ScriptedMarkerEntity> markerType =
                    ModEntities.getEntityType(registryName, ScriptedMarkerEntity.class);
            if (markerType == null) {
                continue;
            }
            ScriptedMarkerSpec markerSpec = ModEntities.getScriptedMarkerSpec(registryName);
            String rendererId = markerSpec == null ? "marker-billboard" : markerSpec.getRendererId();
            event.registerEntityRenderer(markerType, EffectRendererDispatcher.pickMarkerRenderer(rendererId));
        }

        for (String registryName : ModEntities.getScriptedBlockBodyRegistryNames()) {
            EntityType<ScriptedBlockBodyEntity> blockBodyType =
                    ModEntities.getEntityType(registryName, ScriptedBlockBodyEntity.class);
            if (blockBodyType == null) {
                continue;
            }
            ScriptedBlockBodySpec spec = ModEntities.getScriptedBlockBodySpec(registryName);
            String rendererId = spec == null ? "block-body" : spec.getRendererId();
            event.registerEntityRenderer(blockBodyType,
                    EffectRendererDispatcher.pickBlockBodyRenderer(rendererId));
        }

        try {
            ClojureInterop.requireNamespace("cn.li.neoforge262.client.init");
            IFn fn = Clojure.var("cn.li.neoforge262.client.init", "register-scripted-block-entity-renderers!");
            fn.invoke(event);
        } catch (Throwable t) {
            LOGGER.warn("Scripted BER registration skipped: {}", t.toString());
        }
    }

}
