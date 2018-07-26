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