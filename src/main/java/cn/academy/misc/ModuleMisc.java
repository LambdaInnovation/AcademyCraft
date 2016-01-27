/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.misc;

import cn.academy.misc.media.ItemMedia;
import cn.academy.misc.media.MediaRegistry;
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

    @RegItem
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
    }

}
