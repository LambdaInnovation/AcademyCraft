package cn.academy.block.block;

import cn.academy.Resources;
import cn.lambdalib2.registry.StateEventCallback;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

public class ACFluids {
    public static Fluid fluidImagProj = new Fluid(
        "imagProj", Resources.getTexture("blocks/phase_liquid"), Resources.getTexture("blocks/phase_liquid")
    );
    static {
        fluidImagProj.setLuminosity(8).setDensity(7000).setViscosity(6000).setTemperature(0).setDensity(1);
        FluidRegistry.registerFluid(fluidImagProj);
    }

    @StateEventCallback
    private static void init(FMLInitializationEvent ev) {
            // TODO this interface is gone, figure out whether it's still necessary
//        FluidContainerRegistry.registerFluidContainer(new FluidStack(fluidImagProj, 1000),
//            matterUnit.create("phase_liquid"), matterUnit.create("none"));
    }

}
