/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.support.minetweaker;

import cn.academy.core.AcademyCraft;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cpw.mods.fml.common.Optional;
import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
import net.minecraft.item.ItemStack;

/**
 * 
 * @author 3TUSK
 */
@Registrant
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
