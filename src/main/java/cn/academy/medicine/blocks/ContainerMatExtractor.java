package cn.academy.medicine.blocks;

import cn.lambdalib.template.container.CleanContainer;
import net.minecraft.entity.player.EntityPlayer;

public class ContainerMatExtractor extends CleanContainer {
    @Override
    public boolean canInteractWith(EntityPlayer player){
        return true;
    }
}