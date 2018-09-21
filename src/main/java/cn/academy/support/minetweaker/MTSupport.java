package cn.academy.support.minetweaker;

import cn.academy.AcademyCraft;
import cn.lambdalib2.registry.StateEventCallback;
import crafttweaker.CraftTweakerAPI;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraftforge.fml.common.Optional;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

/**
 * 
 * @author 3TUSK
 */
public class MTSupport {

    private static final String MODID = "crafttweaker";

    @StateEventCallback
    @Optional.Method(modid=MODID)
    private static void init(FMLInitializationEvent event) {
        CraftTweakerAPI.registerClass(ImagFusorSupport.class);
        CraftTweakerAPI.registerClass(MetalFormerSupport.class);
        AcademyCraft.log.info("MineTweaker API support has been loaded.");
    }

    @Optional.Method(modid=MODID)
    public static ItemStack toStack(IItemStack s) {
        return CraftTweakerMC.getItemStack(s);
    }

}