package cn.academy.ability.api;

import cpw.mods.fml.relauncher.Side;
import cn.academy.ability.api.proxy.ProxyHelper;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.RegSerializable;
import cn.annoreg.mc.s11n.StorageOption;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Base class for all skill actions.
 * There MUST be a constructor receiving an EntityPlayer.
 * @author acaly
 *
 */
@RegistrationClass
@RegSerializable(instance = ActionSerializer.class)
public class SyncAction extends Tickable {
    
    public final EntityPlayer player;
    public final boolean isRemote;
    
    /**
     * Internal id, used by serializer.
     * You SHOULD NOT modify this field.
     */
    public String id;
    
    public SyncAction(EntityPlayer player) {
        this.player = player;
        this.isRemote = player.worldObj.isRemote;
    }

    protected final void validate(Validation v) {
        if (!v.validate())
            cancelAction();
    }
    
    protected void onActionStarted() {}
    protected void onActionCancelled() {}
    protected void onActionFinished() {}
    protected void onActionTicked() {}
    
    /**
     * Call this function to cancel this action.
     */
    protected final void cancelAction() {
        if (isRemote)
            sendCancelToServer();
        else
            sendCancelToClient();
        doCancelAction();
    }
    
    /**
     * This function should be called right after it is created in the first side.
     * Note that creating one action on both sides results in two actions on both.
     */
    protected final void startSync() {
        //First get an id
        ProxyHelper.get().registerAction(this);
        
        if (isRemote)
            sendSyncStartToServer();
        else
            sendSyncStartToClient();
        
        onActionStarted();
    }
    
    /**
     * Call this function on BOTH sides to finish this action.
     * Note that this event is not synchronized.
     */
    protected final void normalEnd() {
        onActionFinished();
        doFinalizeAction();
    }
    
    /*
     * Internal implementations
     */
    
    /**
     * Called on both sides. Remove the action from sync list.
     */
    private void doFinalizeAction() {
        ProxyHelper.get().removeAction(this);
    }
    
    /**
     * DO NOT USE THIS!
     * Called on both sides. Call callback and finalize.
     * Public used in remote call delegate.
     */
    public final void doCancelAction() {
        onActionCancelled();
        doFinalizeAction();
    }
    
    /**
     * DO NOT USE THIS!
     * Public used in remote call delegate.
     */
    public final void remoteStartAction() {
        onActionStarted();
    }
    
    @Override
    protected final void onTick() {
        onActionTicked();
    }
    
    @RegNetworkCall(side = Side.SERVER, thisStorage = StorageOption.Option.INSTANCE)
    private void sendCancelToServer() {
        doCancelAction();
    }
    
    @RegNetworkCall(side = Side.CLIENT, thisStorage = StorageOption.Option.INSTANCE)
    private void sendCancelToClient() {
        doCancelAction();
    }

    //Still use INSTANCE option.
    //DATA option should be used with great care, for the DataSerializer for the class
    //may not exist.
    @RegNetworkCall(side = Side.SERVER, thisStorage = StorageOption.Option.INSTANCE)
    private void sendSyncStartToServer() {
        //Creation has been done in ActionSerializer.
        remoteStartAction();
    }
    
    @RegNetworkCall(side = Side.CLIENT, thisStorage = StorageOption.Option.INSTANCE)
    private void sendSyncStartToClient() {
        //Creation has been done in ActionSerializer.
        remoteStartAction();
    }
}
