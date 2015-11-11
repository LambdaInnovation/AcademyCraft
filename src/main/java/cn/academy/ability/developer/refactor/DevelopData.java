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
package cn.academy.ability.developer.refactor;

import cn.academy.ability.developer.IDevelopType;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.networkcall.s11n.SerializationManager;
import cn.lambdalib.networkcall.s11n.StorageOption;
import cn.lambdalib.util.datapart.DataPart;
import cn.lambdalib.util.datapart.RegDataPart;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

@Registrant
@RegDataPart("AC_DevelopData")
public class DevelopData extends DataPart {

    private boolean dirty = false;

    private IDeveloper developer;
    private IDevelopType type;

    // API
    /**
     * Make the player start developing with given Developer and Type.
     * If currently is developing the previous action will be overridden.
     */
    public void startDeveloping(IDeveloper developer, IDevelopType type) {

    }

    /**
     * @return Whether the player is developing ability.
     */
    public boolean isDeveloping() {
        // TODO Implement
        return false;
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
    public IDevelopType getDevelopType() {
        return null;
    }

    // Internal

    @Override
    public void tick() {
        if(!isRemote()) {
            if(dirty) {
                sync();
            }
        }
    }

    @Override
    public NBTTagCompound toNBTSync() {
        NBTTagCompound ret = new NBTTagCompound();
        SerializationManager ser = SerializationManager.INSTANCE;
        NBTBase tagDeveloper = ser.serialize(developer, StorageOption.Option.INSTANCE);
        ret.setTag("d", tagDeveloper);
        return ret;
    }

    @Override
    public void fromNBTSync(NBTTagCompound tag) {
        developer = (IDeveloper) SerializationManager.INSTANCE.deserialize(null,
                tag.getTag("d"), StorageOption.Option.NULLABLE_INSTANCE);
    }

    @Override
    public void fromNBT(NBTTagCompound tag) {}

    @Override
    public NBTTagCompound toNBT() { return null; }

}
