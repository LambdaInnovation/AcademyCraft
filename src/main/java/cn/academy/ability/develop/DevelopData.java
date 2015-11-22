/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.ability.develop;

import cn.academy.ability.develop.action.IDevelopAction;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.networkcall.s11n.SerializationManager;
import cn.lambdalib.networkcall.s11n.StorageOption;
import cn.lambdalib.util.datapart.DataPart;
import cn.lambdalib.util.datapart.EntityData;
import cn.lambdalib.util.datapart.RegDataPart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

@Registrant
@RegDataPart("AC_DevelopData")
public class DevelopData extends DataPart<EntityPlayer> {

    public static DevelopData get(EntityPlayer player) {
        return EntityData.get(player).getPart(DevelopData.class);
    }

    public enum DevState { IDLE, FAILED, DEVELOPING }

    private boolean dirty = false;

    private IDeveloper developer;
    private IDevelopAction type;


    // Synced states
    private int stim;
    private int maxStim;
    private DevState state = DevState.IDLE;
    // Sync states end

    private int tickThisStim;
    private int tickSync;

	public DevelopData() {
		setTick();
	}

    // API
    /**
     * Make the player start developing with given Developer and Type.
     * If currently is developing the previous action will be overridden.
     */
    public void startDeveloping(IDeveloper _developer, IDevelopAction _type) {
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
        return 0.0;
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
        if(state == DevState.DEVELOPING) {
            resetProgress(true);
        }
    }

    public void reset() {
        resetProgress(false);
    }

    // Internal

    private void resetProgress(boolean failed) {
        developer = null;
        type = null;
        tickSync = 5;
        stim = maxStim = tickThisStim = 0;
        state = failed ? DevState.FAILED : DevState.IDLE;
    }

    @Override
    public void tick() {
        if(!isRemote()) {
            EntityPlayer player = getEntity();

            if(dirty) {
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
                    }
                }
            }
        }
    }

    @Override
    public NBTTagCompound toNBTSync() {
        NBTTagCompound ret = new NBTTagCompound();
        SerializationManager ser = SerializationManager.INSTANCE;
        NBTBase tagDeveloper = ser.serialize(developer, StorageOption.Option.NULLABLE_INSTANCE);
        ret.setTag("d", tagDeveloper);

        ret.setInteger("0", stim);
        ret.setInteger("1", maxStim);
        ret.setInteger("2", state.ordinal());
        return ret;
    }

    @Override
    public void fromNBTSync(NBTTagCompound tag) {
        developer = (IDeveloper) SerializationManager.INSTANCE.deserialize(null,
                tag.getTag("d"), StorageOption.Option.NULLABLE_INSTANCE);

        stim = tag.getInteger("0");
        maxStim = tag.getInteger("1");
        state = DevState.values()[tag.getInteger("2")];
    }

    @Override
    public void fromNBT(NBTTagCompound tag) {}

    @Override
    public NBTTagCompound toNBT() { return null; }

}
