package cn.li.forge1201.client;

import cn.li.forge1201.AcademyCraft1201;
import cn.li.forge1201.client.render.ForgeClientRenderRegistry;
import cn.li.mcbase.clj.ClojureInterop;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge discovers {@code @Mod.EventBusSubscriber} and invokes BER registration on the
 * mod bus. Clojure {@code IEventBus.addListener(Class, Consumer)} for inner event types
 * can fail to match generics, so the handler never ran (no scripted BER in-game).
 */
@Mod.EventBusSubscriber(modid = AcademyCraft1201.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        ClojureInterop.requireNamespace("cn.li.forge1201.client.obj-model-registration");
        ClojureInterop.invoke(
            "cn.li.forge1201.client.obj-model-registration",
            "register-additional-obj-models!",
            event);
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        ClojureInterop.requireNamespace("cn.li.forge1201.client.obj-model-registration");
        ClojureInterop.invoke(
            "cn.li.forge1201.client.obj-model-registration",
            "replace-obj-composite-models!",
            event);
    }
}
