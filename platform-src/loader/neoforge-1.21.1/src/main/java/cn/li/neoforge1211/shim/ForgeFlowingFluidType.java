package cn.li.neoforge1211.shim;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * Fluid type carrying the texture/tint data consumed by RegisterClientExtensionsEvent
 * (the 1.21.1 replacement for the removed {@code initializeClient} hook).
 */
public final class ForgeFlowingFluidType extends FluidType {
    private final ResourceLocation stillTexture;
    private final ResourceLocation flowingTexture;
    private final ResourceLocation overlayTexture;
    private final int tintColor;

    public ForgeFlowingFluidType(Properties properties,
                                 ResourceLocation stillTexture,
                                 ResourceLocation flowingTexture,
                                 ResourceLocation overlayTexture,
                                 int tintColor) {
        super(properties);
        this.stillTexture = stillTexture;
        this.flowingTexture = flowingTexture;
        this.overlayTexture = overlayTexture;
        this.tintColor = tintColor;
    }

    public ResourceLocation getStillTexture() {
        return stillTexture;
    }

    public ResourceLocation getFlowingTexture() {
        return flowingTexture;
    }

    public ResourceLocation getOverlayTexture() {
        return overlayTexture;
    }

    public int getTintColor() {
        return tintColor;
    }
}
