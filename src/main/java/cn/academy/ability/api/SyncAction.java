package cn.academy.ability.api;

import java.util.ArrayList;

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
    private boolean isStarted = false;
    
    /**
     * Internal id, used by serializer.
     * You SHOULD NOT modify this field.
     */
    public String id;
    
    public SyncAction(EntityPlayer player) {
        this.player = player;
        this.isRemote = player.worldObj.isRemote;
    }

    protected final boolean validate(Validation v) {
        if (!v.validate()) {
            cancelAction();
            return false;
        }
        return true;
    }
    
    protected void onActionStarted() {}
    protected void onActionCancelled() {}
    protected void onActionFinished() {}
    protected void onActionTicked() {}
    
    /**
     * Call this function to cancel this action.
     */
    protected final void cancelAction() {
        if (!isStarted) return;
        isStarted = false;
        
        for (SyncAction sub : subActions) {
            sub.cancelAction();
        }
        
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
        if (isStarted) return;
        isStarted = true;
        
        //First get an id
        ProxyHelper.get().registerAction(this);
        
        if (isRemote)
            sendSyncStartToServer();
        else
            sendSyncStartToClient();
        
        onActionStarted();
    }
    
    /**
     * Call on either side. Finish the action.
     */
    protected final void normalEnd() {
        if (!isStarted) return;
        isStarted = false;
        
        for (SyncAction sub : subActions) {
            sub.normalEnd();
        }
        
        if (isRemote)
            sendEndToServer();
        else
            sendEndToClient();
        
        doNormalEnd();
    }
    
    private ArrayList<SyncAction> subActions = new ArrayList();
    
    /**
     * Add a sub action, and start it immediately (by calling startSync).
     * When this finishes, the subs are automatically terminated.
     * This function will handle the start of the sub. <BR/>
     * <B>IMPORTANT</B>: this call must be manually synchronized on all sides, 
     * which means, you must call this function on all sides in the exact same order,
     * or the system level synchronization will break.
     * @param sub
     */
    protected final void addSubAction(SyncAction sub) {
        subActions.add(sub);
        sub.startNonsync(this.id + ":" + subActions.size());
    }
    
    /*
     * Internal implementations
     */
    
    private void startNonsync(String id) {
        if (isStarted) return;
        isStarted = true;
        
        ProxyHelper.get().registerAction(id, this);
        
        onActionStarted();
    }

    /**
     * DO NOT USE THIS!
     * Public used in remote call delegate.
     */
    public final void doNormalEnd() {
        isStarted = false;
        
        onActionFinished();
        doFinalizeAction();
    }
    
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
        isStarted = false;
        
        onActionCancelled();
        doFinalizeAction();
    }
    
    /**
     * DO NOT USE THIS!
     * Public used in remote call delegate.
     */
    public final void remoteStartAction() {
        isStarted = true;
        
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
    
    @RegNetworkCall(side = Side.SERVER, thisStorage = StorageOption.Option.INSTANCE)
    private void sendEndToServer() {
        doNormalEnd();
    }
    
    @RegNetworkCall(side = Side.CLIENT, thisStorage = StorageOption.Option.INSTANCE)
    private void sendEndToClient() {
        doNormalEnd();
    }
}
