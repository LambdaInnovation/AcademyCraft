package cn.li.neoforge262.client;

import cn.li.mc262.client.GuiGraphicsHelper;
import cn.li.mc262.client.particle.MdParticle;
import cn.li.mcbase.clj.ClojureInterop;
import cn.li.mc262.client.render.GuiRenderPipelines;
import cn.li.mc262.client.render.ModRenderTypes;
import cn.li.mc262.client.render.PlasmaRenderTypes;
import cn.li.mc262.client.render.ReactivePreviewPipRenderer;
import cn.li.mc262.client.render.ReactivePreviewRenderState;
import cn.li.mc262.client.render.item.EnergyItemPropertyFunction;
import cn.li.mc262.client.render.item.FrameItemPropertyFunction;
import cn.li.mc262.client.render.item.MatterKindItemPropertyFunction;
import cn.li.mcver.ResourceLocations;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import org.joml.Matrix3x2f;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import cn.li.neoforge262.client.render.ForgeClientRenderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

/**
 * Client mod-bus render/GUI registration for NeoForge 26.2.
 *
 * EventBusSubscriber.Bus was removed — register explicitly on the mod bus
 * (same pattern as ForgeModBusListener).
 *
 * OBJ items: do <em>not</em> mirror 1.21.1 {@code ModelEvent} composite baking.
 * NeoForge 26.x ships a built-in {@code neoforge:obj} loader; GUI-vs-hand
 * selection is expressed in item definitions via {@code minecraft:select} +
 * {@code minecraft:display_context} (see item-model-provider datagen).
 */
public final class ModClientRenderSetup {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModClientRenderSetup.class);


    private ModClientRenderSetup() {
    }

    public static void register(IEventBus modBus) {
        if (modBus == null) {
            return;
        }
        installGuiExtractionHooks();
        modBus.addListener(ModClientRenderSetup::onRegisterMenuScreens);
        modBus.addListener(ModClientRenderSetup::onRegisterRenderers);
        modBus.addListener(ModClientRenderSetup::onRegisterFluidModels);
        modBus.addListener(ModClientRenderSetup::onRegisterRangeProperties);
        modBus.addListener(ModClientRenderSetup::onRegisterRenderPipelines);
        modBus.addListener(ModClientRenderSetup::onRegisterPictureInPictureRenderers);
        modBus.addListener(ModClientRenderSetup::onRegisterParticleProviders);
    }

    private static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        // Meltdowner md particles (md_particle / md_particle_luck sprites from
        // the mod's particles/<id>.json atlas definitions) — upstream MdParticleFactory. The
        // definition entries must exist for the sprite-set registration
        // (the event javadoc enforces it).
        registerMdParticleProvider(event, "md_particle");
        registerMdParticleProvider(event, "md_particle_luck");
    }

    private static void registerMdParticleProvider(RegisterParticleProvidersEvent event, String id) {
        ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE
                .get(ResourceLocations.of("academy", id))
                .map(Holder.Reference::value).orElse(null);
        if (type instanceof SimpleParticleType simple) {
            event.registerSpriteSet(simple, MdParticle.Provider::new);
        } else {
            LOGGER.error("md particle type not registered: academy:{}", id);
        }
    }

    private static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        ClojureInterop.requireNamespace("cn.li.neoforge262.gui.screen-impl");
        ClojureInterop.invoke(
            "cn.li.neoforge262.gui.screen-impl",
            "register-screens-on-event!",
            event);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ForgeClientRenderRegistry.registerEntityAndBlockRenderers(event);
    }

    private static void onRegisterFluidModels(RegisterFluidModelsEvent event) {
        ClojureInterop.requireNamespace("cn.li.neoforge262.client.init");
        ClojureInterop.invoke("cn.li.neoforge262.client.init", "register-fluid-render-layers!", event);
    }

    public static void registerFluidModel(
            RegisterFluidModelsEvent event,
            FluidModel.Unbaked model,
            Fluid source,
            Fluid flowing) {
        event.register(model, source, flowing);
    }

    private static void onRegisterRangeProperties(RegisterRangeSelectItemModelPropertyEvent event) {
        event.register(ResourceLocations.of("academy", "energy"), EnergyItemPropertyFunction.CODEC);
        // Matter-unit variant + frame animation (upstream ItemMatterUnit:
        // per-damage models + `frame` override for the flowing-liquid).
        event.register(ResourceLocations.of("academy", "matter_kind"), MatterKindItemPropertyFunction.CODEC);
        event.register(ResourceLocations.of("academy", "frame"), FrameItemPropertyFunction.CODEC);
    }

    private static void onRegisterRenderPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(PlasmaRenderTypes.plasmaBodyPipeline());
        event.registerPipeline(ModRenderTypes.academyQuadsTranslucentPipeline());
        GuiRenderPipelines.all().forEach(event::registerPipeline);
    }

    /**
     * Wire the NeoForge GUI-extraction patches the shared mc-26.2 module
     * cannot reference (they must compile against the vanilla jar for Fabric).
     */
    private static void installGuiExtractionHooks() {
        // Two-sampler GUI pipelines (skill_progbar / cpbar_overload) sample a
        // mask from Sampler1; the vanilla extractor blits single textures, so
        // submit a BlitRenderState carrying the double TextureSetup through
        // NeoForge's submitGuiElementRenderState.
        GuiGraphicsHelper.installTwoTextureBlitter((graphics, pipeline, textures,
                x0, y0, x1, y1, u0, u1, v0, v1, argb) -> {
            ScreenRectangle scissor = graphics.peekScissorStack();
            graphics.submitGuiElementRenderState(new BlitRenderState(
                    pipeline, textures, new Matrix3x2f(graphics.pose()),
                    x0, y0, x1, y1, u0, u1, v0, v1, argb, scissor));
            return true;
        });
        // Rotated GUI elements (diagonal connection lines): submit a
        // BlitRenderState through the same patched API, attaching the current
        // scissor (null would sort the element first and let backgrounds cover
        // it).
        GuiGraphicsHelper.installGuiElementSubmitter((graphics, pipeline, textures,
                pose, x0, y0, x1, y1, u0, u1, v0, v1, color) -> {
            graphics.submitGuiElementRenderState(new BlitRenderState(
                    pipeline, textures, pose, x0, y0, x1, y1,
                    u0, u1, v0, v1, color, graphics.peekScissorStack()));
            return true;
        });
    }

    private static void onRegisterPictureInPictureRenderers(
            RegisterPictureInPictureRenderersEvent event) {
        event.register(ReactivePreviewRenderState.class, ReactivePreviewPipRenderer::new);
        // The submission API (submitPictureInPictureRenderState / peekScissorStack)
        // is a NeoForge patch on GuiGraphicsExtractor, so it is installed here
        // instead of in the shared mc-26.2 module (which must compile against
        // the vanilla jar for Fabric).
        ReactivePreviewRenderState.installSubmitter((graphics, itemRenderState,
                blockRenderState, x0, y0, x1, y1, modelScale, yawDegrees, yOffset) -> {
            ScreenRectangle scissorArea = graphics.peekScissorStack();
            ScreenRectangle bounds = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0);
            graphics.submitPictureInPictureRenderState(new ReactivePreviewRenderState(
                    itemRenderState, blockRenderState, x0, y0, x1, y1,
                    modelScale, yawDegrees, yOffset, scissorArea, bounds));
            return true;
        });
    }

}
