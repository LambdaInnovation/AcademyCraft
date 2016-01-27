/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.core.block;

import cn.academy.core.AcademyCraft;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

/**
 * @author WeAthFolD
 */
public class ACBlock extends Block {

    public ACBlock(String name, Material mat) {
        super(mat);
        setCreativeTab(AcademyCraft.cct);
        setBlockTextureName("academy:" + name);
        setBlockName("ac_" + name);
    }

}
