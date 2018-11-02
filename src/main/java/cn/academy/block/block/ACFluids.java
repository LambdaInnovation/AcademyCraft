package cn.academy.block.block;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

public class ACFluids {

    public static Fluid fluidImagProj = new Fluid(
        "imagproj",
        // Note: All render effects are implemented by RenderImagPhaseLiquid
        //  but we have to see what to do when liquid behaves in other situations (e.g. in BC tanks).
        new ResourceLocation("academy:blocks/black"),
        new ResourceLocation("academy:blocks/black")
    );

    static {
        fluidImagProj.setLuminosity(8).setDensity(7000).setViscosity(6000).setTemperature(0).setDensity(1);
        FluidRegistry.registerFluid(fluidImagProj);
    }

}
