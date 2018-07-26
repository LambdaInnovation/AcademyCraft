package cn.academy.ability.develop;

import cn.academy.ability.develop.action.IDevelopAction;
import cn.lambdalib2.s11n.SerializeDynamic;
import cn.lambdalib2.s11n.SerializeIncluded;
import cn.lambdalib2.s11n.SerializeNullable;
import cn.lambdalib2.datapart.DataPart;
import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.datapart.RegDataPart;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayer;

@RegDataPart(EntityPlayer.class)
public class DevelopData extends DataPart<EntityPlayer> {

    public static DevelopData get(EntityPlayer player) {
        return EntityData.get(player).getPart(DevelopData.class);
    }

    public enum DevState { IDLE, FAILED, DEVELOPING, DONE }

    private boolean dirty = false;

    @SerializeIncluded
    @SerializeDynamic
    @SerializeNullable
    private IDeveloper developer;
    private IDevelopAction type;

    @SerializeIncluded
    private int stim;
    @SerializeIncluded
    private int maxStim;
    @SerializeIncluded
    private DevState state = DevState.IDLE;

    private int tickThisStim;
    private int tickSync;

    public DevelopData() {
        setTick(true);
    }

    // API
    /**
     * Make the player start developing with given Developer and Type.
     * If currently is developing the previous action will be overridden.
     */
    public void startDeveloping(IDeveloper _developer, IDevelopAction _type) {
        checkSide(Side.SERVER);

        resetProgress(false);
        developer = _developer;
        type = _type;
        state = DevState.DEVELOPING;
        maxStim = type.getStimulations(getEntity());
        dirty = true;
    }

    /**
     * @return Whether the player is developing ability.
     */
    public boolean isDeveloping() {
        return developer != null;
    }

    /**
     * @return The develop progress [0, 1], 0.0 if not developing
     */
    public double getDevelopProgress() {
        if (isDeveloping()) {
            return (double) stim / maxStim + (double) tickThisStim / developer.getType().getTPS();
        } else {
            return 0;
        }
    }

    /**
     * @return Current developer type or null if not developing
     */
    public IDevelopAction getDevelopType() {
        return type;
    }

    public int getStim() {
        return stim;
    }

    public DevState getState() {
        return state;
    }

    public int getMaxStim() {
        return maxStim;
    }

    public void abort() {
        checkSide(Side.SERVER);

        if(state == DevState.DEVELOPING) {
            resetProgress(true);
        }
    }

    public void reset() {
//        checkSide(Side.SERVER);

        resetProgress(false);
    }

    // Internal

    private void resetProgress(boolean failed) {
        developer = null;
        type = null;
        tickSync = 5;
        stim = maxStim = tickThisStim = 0;
        state = failed ? DevState.FAILED : DevState.IDLE;
        dirty = true;
    }

    @Override
    public void tick() {
        if(!isClient()) {
            EntityPlayer player = getEntity();

            if(dirty) {
                dirty = false;
                sync();
            }

            if(isDeveloping()) {
                DeveloperType devType = developer.getType();

                // Sync
                if(tickSync-- == 0) {
                    tickSync = 5;
                    sync();
                }

                // Logic
                double consume = devType.getCPS() / devType.getTPS();
                if(!developer.tryPullEnergy(consume)) {
                    resetProgress(true);
                    return;
                }

                if(++tickThisStim > devType.getTPS()) {
                    tickThisStim = 0;
                    ++stim;

                    if(stim >= maxStim) {
                        // try perform the action.
                        boolean success = type.validate(player, developer);
                        if(success) {
                            type.onLearned(player);
                        }
                        resetProgress(!success);
                        if (success) {
                            state = DevState.DONE;
                        }
                    }
                }
            }
        }
    }

}