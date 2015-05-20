package cn.academy.ability.api.action;

import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import cn.academy.ability.api.ctrl.SyncAction;

public abstract class ClientEntityAction extends SyncAction {

    public ClientEntityAction(EntityPlayer player) {
        super(player);
    }

    private Entity e;
    
    protected abstract Entity createEntity();
    
    @Override
    protected void onActionStarted() {
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            e = createEntity();
            e.worldObj.spawnEntityInWorld(e);
        }
    }

    @Override
    protected void onActionCancelled() {
        if (e != null) e.setDead();
    }

    @Override
    protected void onActionFinished() {
        if (e != null) e.setDead();
    }
    
}
