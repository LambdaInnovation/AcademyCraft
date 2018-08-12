package cn.academy.block.block;

import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.mc.RegEventHandler;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static cn.academy.block.block.ACBlocksLegacy.imagPhase;

public class ACFluids {
    public static Fluid fluidImagProj = new Fluid("imagProj");
    static {
        fluidImagProj.setLuminosity(8).setDensity(7000).setViscosity(6000).setTemperature(0).setDensity(1);
        FluidRegistry.registerFluid(fluidImagProj);
    }

    @StateEventCallback
    private static void init(FMLInitializationEvent ev) {
        FluidContainerRegistry.registerFluidContainer(new FluidStack(fluidImagProj, 1000),
            matterUnit.create("phase_liquid"), matterUnit.create("none"));
    }

    private enum EventHandler {
        @RegEventHandler()
        instance;

        @SubscribeEvent
        @SideOnly(Side.CLIENT)
        public void textureHook(TextureStitchEvent.Post event) {
            fluidImagProj.setIcons(imagPhase.fluidIcon);
        }
    }
}
