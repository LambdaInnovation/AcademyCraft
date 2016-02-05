/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.develop;

import cn.academy.ability.ModuleAbility;
import cn.academy.energy.api.IFItemManager;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.util.datapart.DataPart;
import cn.lambdalib.util.datapart.EntityData;
import cn.lambdalib.util.datapart.RegDataPart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * The Developer instance for portable developer attached one per player.
 * @author WeAthFolD
 */
@Registrant
@RegDataPart(EntityPlayer.class)
public class PortableDevData extends DataPart<EntityPlayer> implements IDeveloper {
    
    public static PortableDevData get(EntityPlayer player) {
        return EntityData.get(player).getPart(PortableDevData.class);
    }

    private ItemStack stack() {
        ItemStack stack = getEntity().getCurrentEquippedItem();
        return stack != null && stack.getItem() == ModuleAbility.developerPortable ? stack : null;
    }

    @Override
    public DeveloperType getType() {
        return DeveloperType.PORTABLE;
    }

    @Override
    public boolean tryPullEnergy(double amount) {
        ItemStack stack = stack();
        if(stack == null)
            return false;
        return IFItemManager.instance.pull(stack, amount, true) == amount;
    }

    @Override
    public double getEnergy() {
        ItemStack stack = stack();
        return stack == null ? 0 : IFItemManager.instance.getEnergy(stack);
    }

    @Override
    public double getMaxEnergy() {
        ItemStack stack = stack();
        return stack == null ? 0 : IFItemManager.instance.getMaxEnergy(stack);
    }
}
