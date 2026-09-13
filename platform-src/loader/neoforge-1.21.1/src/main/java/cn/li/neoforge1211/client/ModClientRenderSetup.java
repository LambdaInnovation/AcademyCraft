package cn.li.neoforge1211.client;

import cn.li.neoforge1211.AcademyCraft1211;
import cn.li.neoforge1211.client.render.ForgeClientRenderRegistry;
import cn.li.neoforge1211.shim.ForgeFlowingFluidType;
import cn.li.mcbase.clj.ClojureInterop;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * NeoForge discovers {@code @EventBusSubscriber} and invokes BER registration on the
 * mod bus. Clojure {@code IEventBus.addListener(Class, Consumer)} for inner event types
 * can fail to match generics, so the handler never ran (no scripted BER in-game).
 */
@EventBusSubscriber(modid = AcademyCraft1211.MODID, value = Dist.CLIENT)
public final class ModClientRenderSetup {

    private ModClientRenderSetup() {
    }

    @SubscribeEvent
    public static void onRegisterBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ForgeClientRenderRegistry.registerEntityAndBlockRenderers(event);
    }

    @SubscribeEvent
    public static void onRegisterItemDecorations(RegisterItemDecorationsEvent event) {
        ForgeClientRenderRegistry.registerItemDecorations(event);
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        ForgeClientRenderRegistry.registerParticleProviders(event);
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        ClojureInterop.requireNamespace("cn.li.neoforge1211.gui.screen-impl");
        ClojureInterop.invoke(
            "cn.li.neoforge1211.gui.screen-impl",
            "register-screens-on-event!",
            event);
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        ClojureInterop.requireNamespace("cn.li.neoforge1211.client.obj-model-registration");
        ClojureInterop.invoke(
            "cn.li.neoforge1211.client.obj-model-registration",
            "register-additional-obj-models!",
            event);
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        ClojureInterop.requireNamespace("cn.li.neoforge1211.client.obj-model-registration");
        ClojureInterop.invoke(
            "cn.li.neoforge1211.client.obj-model-registration",
            "replace-obj-composite-models!",
            event);
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        NeoForgeRegistries.FLUID_TYPES.forEach(fluidType -> {
            if (fluidType instanceof ForgeFlowingFluidType fluid) {
                event.registerFluidType(new IClientFluidTypeExtensions() {
                    @Override
                    public ResourceLocation getStillTexture() {
                        return fluid.getStillTexture();
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return fluid.getFlowingTexture();
                    }

                    @Override
                    public ResourceLocation getOverlayTexture() {
                        return fluid.getOverlayTexture();
                    }

                    @Override
                    public int getTintColor() {
                        return fluid.getTintColor();
                    }
                }, fluidType);
            }
        });
    }
}
