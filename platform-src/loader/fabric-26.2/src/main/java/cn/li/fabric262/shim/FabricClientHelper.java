package cn.li.fabric262.shim;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.material.Fluid;

/**
 * Fabric-only client helpers (entity renderer registration + fluid layers/handlers).
 * Shared BER / texture helpers live in {@link cn.li.mc262.client.ClientHelper}.
 */
public final class FabricClientHelper {

    private FabricClientHelper() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void registerEntityRenderer(EntityType<?> entityType, EntityRendererProvider<?> provider) {
        EntityRendererRegistry.register((EntityType) entityType, (EntityRendererProvider) provider);
    }

    public static void setFluidRenderLayerTranslucent(Fluid sourceFluid, Fluid flowingFluid) {
        // 26.2 uses the vanilla fluid model pipeline; the old render-layer map
        // was removed from Fabric API and is no longer needed.
    }

    public static void registerSimpleFluidRenderHandler(Fluid sourceFluid,
                                                        Fluid flowingFluid,
                                                        String stillTexture,
                                                        String flowingTexture,
                                                        String overlayTexture,
                                                        int tintColor) {
        // Kept as a stable DSL seam. Fluid textures are supplied by the
        // 26.2 vanilla fluid model definitions instead of SimpleFluidRenderHandler.
    }
}
