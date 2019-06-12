package cn.academy.analytic.events;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.Event;

public class AnalyticLevelUpEvent extends Event {
    private EntityPlayer entityPlayer;
    public AnalyticLevelUpEvent(EntityPlayer entity){
        this.entityPlayer = entity;
    }

    public EntityPlayer getEntityPlayer() {
        return entityPlayer;
    }

    public void setEntityPlayer(EntityPlayer entityPlayer) {
        this.entityPlayer = entityPlayer;
    }
}
