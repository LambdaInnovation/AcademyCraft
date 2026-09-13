package cn.li.neoforge262.shim;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.fluids.FluidType;

/** Fluid type carrying the texture/tint data consumed by RegisterFluidModelsEvent. */
public final class ForgeFlowingFluidType extends FluidType {
    private final Identifier stillTexture;
    private final Identifier flowingTexture;
    private final Identifier overlayTexture;
    private final int tintColor;

    public ForgeFlowingFluidType(Properties properties,
                                 Identifier stillTexture,
                                 Identifier flowingTexture,
                                 Identifier overlayTexture,
                                 int tintColor) {
        super(properties);
        this.stillTexture = stillTexture;
        this.flowingTexture = flowingTexture;
        this.overlayTexture = overlayTexture;
        this.tintColor = tintColor;
    }

    public Identifier getStillTexture() {
        return stillTexture;
    }

    public Identifier getFlowingTexture() {
        return flowingTexture;
    }

    public Identifier getOverlayTexture() {
        return overlayTexture;
    }

    public int getTintColor() {
        return tintColor;
    }
}
