package cn.academy.ability.api;

import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.StorageOption;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayer;

@RegistrationClass
public class SkillInstance extends SyncAction {
    //TODO data access and utilities integration

    public SkillInstance(EntityPlayer player) {
        super(player);
    }
    
    protected void onKeyUp() {}
    
    /*
     * Testing
     */
    
    @Override
    protected void onActionStarted() {
        System.out.println("Skill start.");
    }
    
    @Override
    protected void onActionCancelled() {
        System.out.println("Skill cancelled.");
    }
    
    @Override
    protected void onActionFinished() {
        System.out.println("Skill finished.");
    }
    
    /**
     * Called by ClientHandler
     */
    void onClientKeyUp() {
        onKeyUp();
    }
}
