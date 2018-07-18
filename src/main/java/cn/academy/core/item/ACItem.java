/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.core.item;

import cn.academy.core.AcademyCraft;
import net.minecraft.item.Item;

/**
 * Base class for all hard-coded item in AC.
 * @author WeAthFolD
 */
public class ACItem extends Item {

    public ACItem(String name) {
        setUnlocalizedName("ac_" + name);
        // FIXME impl in json
//        setTextureName("academy:" + name);
        setCreativeTab(AcademyCraft.cct);
    }
    
}
