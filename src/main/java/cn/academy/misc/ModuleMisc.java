/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.misc;

import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cn.lambdalib.annoreg.mc.RegItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;

import static net.minecraftforge.common.ChestGenHooks.*;

/**
 * @author WeAthFolD
 */
@Registrant
public class ModuleMisc {

/*    @RegItem
    public static ItemMedia itemMedia;

    @RegInitCallback
    public static void init() {
        String[] mediaApperance = { MINESHAFT_CORRIDOR, PYRAMID_DESERT_CHEST, PYRAMID_JUNGLE_CHEST, STRONGHOLD_LIBRARY,
                DUNGEON_CHEST };

        for (String s : mediaApperance) {
            for (int i = 0; i < MediaRegistry.getMediaCount(); ++i) {
                ItemStack stack = new ItemStack(itemMedia, 1, i);
                ChestGenHooks.addItem(s, new WeightedRandomChestContent(stack, 1, 1, 4));
            }
        }
    }*/

}
