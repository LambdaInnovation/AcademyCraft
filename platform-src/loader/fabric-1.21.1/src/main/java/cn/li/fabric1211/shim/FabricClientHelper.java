package cn.li.fabric1211.shim;

import cn.li.mcver.ResourceLocations;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.material.Fluid;

/**
 * Fabric-only client helpers (entity renderer registration + fluid layers/handlers).
 * Shared BER / texture helpers live in {@link cn.li.mc1211.client.ClientHelper}.
 */
public final class FabricClientHelper {

    private FabricClientHelper() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void registerEntityRenderer(EntityType<?> entityType, EntityRendererProvider<?> provider) {
        EntityRendererRegistry.register((EntityType) entityType, (EntityRendererProvider) provider);
    }

    public static void setFluidRenderLayerTranslucent(Fluid sourceFluid, Fluid flowingFluid) {
        BlockRenderLayerMap.INSTANCE.putFluids(RenderType.translucent(), sourceFluid, flowingFluid);
    }

    public static void registerSimpleFluidRenderHandler(Fluid sourceFluid,
                                                        Fluid flowingFluid,
                                                        String stillTexture,
                                                        String flowingTexture,
                                                        String overlayTexture,
                                                        int tintColor) {
        ResourceLocation still = ResourceLocations.parse(stillTexture);
        ResourceLocation flowing = ResourceLocations.parse(flowingTexture);
        ResourceLocation overlay = overlayTexture == null || overlayTexture.isBlank()
            ? null
            : ResourceLocations.parse(overlayTexture);
        SimpleFluidRenderHandler handler = overlay == null
            ? new SimpleFluidRenderHandler(still, flowing, tintColor)
            : new SimpleFluidRenderHandler(still, flowing, overlay, tintColor);
        FluidRenderHandlerRegistry.INSTANCE.register(sourceFluid, flowingFluid, handler);
    }
}
