/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.crafting.api.event;

import cn.academy.crafting.item.ItemMatterUnit.MatterMaterial;
import cpw.mods.fml.common.eventhandler.Event;
import net.minecraft.entity.player.EntityPlayer;

/**
 * @author WeAthFolD
 */
public class MatterUnitHarvestEvent extends Event {
    
    public final EntityPlayer player;
    public final MatterMaterial mat;
    
    public MatterUnitHarvestEvent(EntityPlayer _player, MatterMaterial _mat) {
        player = _player;
        mat = _mat;
    }
    
}
