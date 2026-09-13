package cn.li.neoforge1211.client.render;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import cn.li.neoforge1211.AcademyCraft1211;
import cn.li.neoforge1211.entity.ModEntities;
import cn.li.mcbase.clj.ClojureInterop;
import cn.li.mc1211.client.effects.particle.MdParticle;
import cn.li.mc1211.client.font.msdf.MsdfRenderTypes;
import cn.li.mc1211.client.render.EffectRendererDispatcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import cn.li.mc1211.client.render.ModRenderTypes;
import cn.li.mcbase.client.render.RenderProfileBootstrap;
import cn.li.mc1211.client.render.effect.BehaviorObjRenderer;
import cn.li.mc1211.client.render.effect.ScriptedBlockBodyRenderer;
import cn.li.mc1211.entity.ScriptedBlockBodyEntity;
import cn.li.mc1211.entity.ScriptedEffectEntity;
import cn.li.mc1211.entity.ScriptedMarkerEntity;
import cn.li.mc1211.entity.ScriptedProjectileEntity;
import cn.li.mc1211.entity.ScriptedRayEntity;
import cn.li.mcbase.entity.spec.ScriptedBlockBodySpec;
import cn.li.mcbase.entity.spec.ScriptedMarkerSpec;
import cn.li.mcbase.entity.spec.ScriptedRaySpec;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.io.IOException;

/**
 * Centralized client rendering registration entrypoint for Forge.
 */
public final class ForgeClientRenderRegistry {
    private static final Logger LOGGER = LogManager.getLogger();
    private static ShaderInstance plasmaBodyShader;
    private static ShaderInstance skillProgbarShader;
    private static ShaderInstance monoShader;
    private static ShaderInstance cpbarOverloadShader;
    private static ShaderInstance alphaDiscardShader;

    private ForgeClientRenderRegistry() {
    }

    public static void registerEntityAndBlockRenderers(EntityRenderersEvent.RegisterRenderers event) {
        RenderProfileBootstrap.runContentClientInitHooks();

        for (String registryName : ModEntities.getScriptedEffectRegistryNames()) {
            EntityType<ScriptedEffectEntity> effectType = ModEntities.getEntityType(registryName, ScriptedEffectEntity.class);
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
            // Upstream RendererMagHook draws maghook.obj, swapping to
            // maghook_open.obj once the hook bites (EntityMagHook.isHit).
            // ThrownItemRenderer drew a flat item sprite that never opened.
            event.registerEntityRenderer(magHook, BehaviorObjRenderer::new);
        }

        for (String registryName : ModEntities.getScriptedRayRegistryNames()) {
            EntityType<ScriptedRayEntity> rayType = ModEntities.getEntityType(registryName, ScriptedRayEntity.class);
            if (rayType == null) {
                continue;
            }
            ScriptedRaySpec raySpec = ModEntities.getScriptedRaySpec(registryName);
            String rendererId = raySpec == null ? "ray-composite" : raySpec.getRendererId();
            event.registerEntityRenderer(rayType, EffectRendererDispatcher.pickRayRenderer(rendererId));
        }

        for (String registryName : ModEntities.getScriptedMarkerRegistryNames()) {
            EntityType<ScriptedMarkerEntity> markerType = ModEntities.getEntityType(registryName, ScriptedMarkerEntity.class);
            if (markerType == null) {
                continue;
            }
            ScriptedMarkerSpec markerSpec = ModEntities.getScriptedMarkerSpec(registryName);
            String rendererId = markerSpec == null ? "marker-billboard" : markerSpec.getRendererId();
            event.registerEntityRenderer(markerType, EffectRendererDispatcher.pickMarkerRenderer(rendererId));
        }

        for (String registryName : ModEntities.getScriptedBlockBodyRegistryNames()) {
            EntityType<ScriptedBlockBodyEntity> blockBodyType = ModEntities.getEntityType(registryName, ScriptedBlockBodyEntity.class);
            if (blockBodyType == null) {
                continue;
            }
            ScriptedBlockBodySpec spec = ModEntities.getScriptedBlockBodySpec(registryName);
            String rendererId = spec == null ? "block-body" : spec.getRendererId();
            if ("block-body".equals(rendererId)) {
                event.registerEntityRenderer(blockBodyType, ScriptedBlockBodyRenderer::new);
            } else {
                // Behavior-driven block-body entities (e.g. the silbarn)
                // render their own model: BehaviorObjRenderer resolves the
                // content render namespace from the spec's hook id.
                event.registerEntityRenderer(blockBodyType, BehaviorObjRenderer::new);
            }
        }

        ClojureInterop.requireNamespace("cn.li.neoforge1211.client.init");
        IFn fn = Clojure.var("cn.li.neoforge1211.client.init", "register-scripted-block-entity-renderers!");
        fn.invoke(event);
    }

    public static void registerItemDecorations(RegisterItemDecorationsEvent event) {
        // Content-specific item decorators are registered by descriptor-driven client hooks.
    }

        public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        // Meltdowner md particles (md_particle / md_particle_luck sprites from
        // the mod's particles/<id>.json atlas definitions) — upstream MdParticleFactory.
        registerMdParticleProvider(event, "md_particle");
        registerMdParticleProvider(event, "md_particle_luck");
    }

    private static void registerMdParticleProvider(RegisterParticleProvidersEvent event, String id) {
        ResourceLocation key = ResourceLocation.fromNamespaceAndPath(AcademyCraft1211.MODID, id);
        ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(key);
        if (type instanceof SimpleParticleType simple) {
            event.registerSpriteSet(simple, MdParticle.Provider::new);
        } else {
            LOGGER.error("md particle type not registered: {}", key);
        }
    }

    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
            new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(AcademyCraft1211.MODID, "plasma_body"),
                ModRenderTypes.PLASMA_BODY_FORMAT),
            shader -> {
                plasmaBodyShader = shader;
                ModRenderTypes.setPlasmaBodyShader(shader);
            }
        );
        // Fog-free textured quad shader for MineDetect's ore highlights:
        // every vanilla shader applies fog, but the highlights must stay
        // visible through the blindness fog the skill itself applies
        // (upstream disables GL_FOG for the whole mineview pass).
        try {
            event.registerShader(
                new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(AcademyCraft1211.MODID, "rendertype_academy_no_fog"),
                    com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),
                ModRenderTypes::setNoFogShader
            );
        } catch (IOException e) {
            LOGGER.error("Failed to register no-fog quad shader", e);
        }
        try {
            event.registerShader(
                new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(AcademyCraft1211.MODID, "msdf_text"),
                    MsdfRenderTypes.MSDF_TEXT_FORMAT),
                shader -> {
                    MsdfRenderTypes.setMsdfShader(shader);
                    ClojureInterop.requireNamespace("cn.li.mc1211.gui.cgui.font");
                    ClojureInterop.invoke("cn.li.mc1211.gui.cgui.font",
                            "set-msdf-shader!", shader);
                }
            );
            ClojureInterop.requireNamespace("cn.li.mc1211.client.font.msdf-setup");
            ClojureInterop.invoke("cn.li.mc1211.client.font.msdf-setup", "on-shader-ready!");
        } catch (IOException e) {
            LOGGER.error("Failed to register MSDF text shader", e);
        }
        // Skill tree shaders
        try {
            // Skill progress ring shader (radial wipe effect)
            event.registerShader(
                new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(AcademyCraft1211.MODID, "skill_progbar"),
                    com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX),
                shader -> {
                    skillProgbarShader = shader;
                    ModRenderTypes.setSkillProgbarShader(shader);
                }
            );
            // Grayscale shader (for unlearned content icons)
            event.registerShader(
                new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(AcademyCraft1211.MODID, "mono"),
                    com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX),
                shader -> {
                    monoShader = shader;
                    ModRenderTypes.setMonoShader(shader);
                }
            );
            // CPBar overload shader (scroll + highlight pulse effect)
            event.registerShader(
                new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(AcademyCraft1211.MODID, "cpbar_overload"),
                    com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX),
                shader -> {
                    cpbarOverloadShader = shader;
                    ModRenderTypes.setCpbarOverloadShader(shader);
                }
            );
            // Alpha discard shader (for depth masking in content tree nodes)
            event.registerShader(
                new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(AcademyCraft1211.MODID, "alpha_discard"),
                    com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX),
                shader -> {
                    alphaDiscardShader = shader;
                    ModRenderTypes.setAlphaDiscardShader(shader);
                }
            );
        } catch (IOException e) {
            LOGGER.error("Failed to register content shaders", e);
        }
    }

    public static ShaderInstance getPlasmaBodyShader() {
        return plasmaBodyShader;
    }

    public static ShaderInstance getSkillProgbarShader() {
        return skillProgbarShader;
    }

    public static ShaderInstance getMonoShader() {
        return monoShader;
    }

    public static ShaderInstance getCpbarOverloadShader() {
        return cpbarOverloadShader;
    }

    public static ShaderInstance getAlphaDiscardShader() {
        return alphaDiscardShader;
    }
}
