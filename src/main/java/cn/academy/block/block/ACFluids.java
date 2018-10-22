package cn.academy.block.block;

import cn.academy.Resources;
import cn.lambdalib2.registry.StateEventCallback;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ACFluids {
    public static Fluid fluidImagProj = new Fluid(
        "imagproj",
        new ResourceLocation("academy:blocks/phase_liquid"),
        new ResourceLocation("academy:blocks/phase_liquid")
    );
    static {
        fluidImagProj.setLuminosity(8).setDensity(7000).setViscosity(6000).setTemperature(0).setDensity(1);
        FluidRegistry.registerFluid(fluidImagProj);
    }

    @StateEventCallback
    private static void preInit(FMLPreInitializationEvent ev) {
            // TODO this interface is gone, figure out whether it's still necessary
//        FluidContainerRegistry.registerFluidContainer(new FluidStack(fluidImagProj, 1000),
//            matterUnit.create("phase_liquid"), matterUnit.create("none"));
    }

}
