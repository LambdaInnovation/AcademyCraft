package cn.academy.misc.tutorial;

import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.util.datapart.DataPart;
import cn.lambdalib.util.datapart.EntityData;
import cn.lambdalib.util.datapart.RegDataPart;
import cn.lambdalib.util.helper.TickScheduler;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import java.util.BitSet;

/**
 * This class simply stores activate data that trigger-type condition needs.
 */
@Registrant
@RegDataPart("AC_TutorialCondition")
public class TutorialConditionData extends DataPart {

    public static TutorialConditionData get(EntityPlayer player) {
        return EntityData.get(player).getPart(TutorialConditionData.class);
    }

    private BitSet saved = new BitSet();
    private final TickScheduler scheduler = new TickScheduler();
    private boolean dirty;

    public TutorialConditionData() {
        setTick();
        scheduler.every(3).atOnly(Side.SERVER).run(() ->
        {
            if (dirty) {
                dirty = false;
                sync();
            }
        });
    }

    @Override
    public void tick() {
        scheduler.runTick();
    }

    public boolean getActivate(Condition cond) {
        return saved.get(cond.index);
    }

    public void setActivate(Condition cond) {
        saved.set(cond.index);
        dirty = true;
    }

    @Override
    public void fromNBT(NBTTagCompound tag) {
        saved = BitSet.valueOf(tag.getByteArray("s"));
    }

    @Override
    public NBTTagCompound toNBT() {
        NBTTagCompound ret = new NBTTagCompound();
        ret.setByteArray("s", saved.toByteArray());
        return ret;
    }

}