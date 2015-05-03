package cn.academy.ability.api;

import java.util.ArrayList;

import cpw.mods.fml.relauncher.Side;
import cn.academy.ability.api.proxy.ProxyHelper;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.RegSerializable;
import cn.annoreg.mc.s11n.StorageOption;
import cn.annoreg.mc.s11n.StorageOption.Instance;
import cn.annoreg.mc.s11n.StorageOption.Target;
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
     * Call this function on any side(s) to cancel this action.
     */
    protected final void cancelAction() {
        if (!isStarted) return;
        isStarted = false;
        
        for (SyncAction sub : subActions) {
            sub.cancelAction();
        }
        
        if (isRemote)
            sendCancelToServer(ProxyHelper.get().getThePlayer());
        else
            sendCancelToClient(null);
        
        doCancelAction();
    }
    
    /**
     * This function should be called right after it is created on the FIRST side.
     * Note that creating one action on both sides results in two actions on both.
     */
    protected final void startSync() {
        if (isStarted) return;
        isStarted = true;
        
        //First get an id
        ProxyHelper.get().registerAction(this);
        
        if (isRemote)
            sendSyncStartToServer(ProxyHelper.get().getThePlayer());
        else
            sendSyncStartToClient(null);
        
        onActionStarted();
    }
    
    /**
     * Call on ONE side. Finish the action.
     */
    protected final void normalEnd() {
        if (!isStarted) return;
        isStarted = false;
        
        for (SyncAction sub : subActions) {
            sub.normalEnd();
        }
        
        if (isRemote)
            sendEndToServer(ProxyHelper.get().getThePlayer());
        else
            sendEndToClient(null);
        
        doNormalEnd();
    }
    
    /**
     * Actually this is all actions that should finish as soon as this action finishes.
     * Note that a sub action may only present in this array on one side (server or client),
     * and that is enough for it to work.
     */
    private ArrayList<SyncAction> subActions = new ArrayList();
    
    /**
     * Add a sub action, and start it immediately (by calling startSync).
     * When this finishes, the subs are automatically terminated.
     * This function will handle the start of the sub. <BR/>
     * <B>IMPORTANT</B>: this call must be manually synchronized on all sides.
     * @param sub
     */
    protected final void addSubAction(String id, SyncAction sub) {
        subActions.add(sub);
        sub.startNonsync(this.id + ":" + id);
    }
    
    protected final void addSubActionAndWait(String id, SyncAction sub) {
        sub.subActions.add(this);
        sub.startNonsync(this.id + ":" + id);
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
     * If the first side is client, this function is called
     * twice on that side, with the second time by remote call
     * from server.
     */
    private void doNormalEnd() {
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
     * Called on both sides. Call callback and finalize.
     */
    private void doCancelAction() {
        isStarted = false;
        
        onActionCancelled();
        doFinalizeAction();
    }
    
    /**
     * If the first side is client, this function is called
     * twice on that side, with the second time by remote call
     * from server.
     */
    private void remoteStartAction() {
        if (isStarted) return;
        isStarted = true;
        
        onActionStarted();
    }
    
    @Override
    protected final void onTick() {
        onActionTicked();
    }
    
    
    /*
     * Network part.
     */
    
    @RegNetworkCall(side = Side.SERVER, thisStorage = StorageOption.Option.INSTANCE)
    private void sendCancelToServer(@Instance EntityPlayer source) {
        doCancelAction();
        sendCancelToClient(source);
    }
    
    @RegNetworkCall(side = Side.CLIENT, thisStorage = StorageOption.Option.INSTANCE)
    private void sendCancelToClient(@Target(range = Target.RangeOption.EXCEPT) EntityPlayer ex) {
        doCancelAction();
    }

    //Still use INSTANCE option.
    //DATA option should be used with great care, for the DataSerializer for the class
    //may not exist.
    @RegNetworkCall(side = Side.SERVER, thisStorage = StorageOption.Option.INSTANCE)
    private void sendSyncStartToServer(@Instance EntityPlayer source) {
        //Creation has been done in ActionSerializer.
        //No need to check newly-created-flag in starting.
        remoteStartAction();
        sendSyncStartToClient(source);
    }
    
    @RegNetworkCall(side = Side.CLIENT, thisStorage = StorageOption.Option.INSTANCE)
    private void sendSyncStartToClient(@Target(range = Target.RangeOption.EXCEPT) EntityPlayer ex) {
        //Creation has been done in ActionSerializer.
        //No need to check newly-created-flag in starting.
        remoteStartAction();
    }
    
    @RegNetworkCall(side = Side.SERVER, thisStorage = StorageOption.Option.INSTANCE)
    private void sendEndToServer(@Instance EntityPlayer source) {
        doNormalEnd();
        sendEndToClient(source);
    }
    
    @RegNetworkCall(side = Side.CLIENT, thisStorage = StorageOption.Option.INSTANCE)
    private void sendEndToClient(@Target(range = Target.RangeOption.EXCEPT) EntityPlayer ex) {
        doNormalEnd();
    }
}
