package cn.academy.support.minetweaker;

import cn.academy.AcademyCraft;
import net.minecraftforge.fml.common.Optional;
import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
import net.minecraft.item.ItemStack;

/**
 * 
 * @author 3TUSK
 */
public class MTSupport {

    private static final String MODID = "MineTweaker3";

    @RegInitCallback
    @Optional.Method(modid=MODID)
    private static void init() {
        MineTweakerAPI.registerClass(ImagFusorSupport.class);
        MineTweakerAPI.registerClass(MetalFormerSupport.class);
        AcademyCraft.log.info("MineTweaker API support has been loaded.");
    }

    @Optional.Method(modid=MODID)
    public static ItemStack toStack(IItemStack s) {
        return MineTweakerMC.getItemStack(s);
    }

}