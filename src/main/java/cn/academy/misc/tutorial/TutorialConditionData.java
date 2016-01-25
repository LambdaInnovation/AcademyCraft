package cn.academy.misc.tutorial;

import cn.academy.core.AcademyCraft;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.s11n.network.NetworkMessage;
import cn.lambdalib.s11n.network.NetworkMessage.Listener;
import cn.lambdalib.util.datapart.DataPart;
import cn.lambdalib.util.datapart.EntityData;
import cn.lambdalib.util.datapart.RegDataPart;
import cn.lambdalib.util.helper.TickScheduler;
import com.google.common.base.Preconditions;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.MinecraftForge;

import java.util.*;
import java.util.stream.IntStream;

/**
 * This class simply stores activate data that trigger-type condition needs.
 */
@Registrant
@RegDataPart("AC_TutorialCondition")
public class TutorialConditionData extends DataPart<EntityPlayer> {

    public static TutorialConditionData get(EntityPlayer player) {
        return EntityData.get(player).getPart(TutorialConditionData.class);
    }

    private static final String MSG_ACTIVATE = "activate";

    private BitSet savedConditions = new BitSet();
    private Set<String> activatedTuts = new HashSet<>();
    private final TickScheduler scheduler = new TickScheduler();
    private boolean dirty;

    public TutorialConditionData() {
        setTick();
        scheduler.every(3).atOnly(Side.SERVER).run(() ->
        {
            if (dirty) {
                dirty = false;
                TutorialRegistry.enumeration().forEach(tut -> {
                    if (!activatedTuts.contains(tut.id) &&
                            tut.isActivated(getEntity()) &&
                            !tut.isDefaultInstalled()) {
                        activatedTuts.add(tut.id);

                        NetworkMessage.sendToSelf(this, MSG_ACTIVATE, tut.id);
                        NetworkMessage.sendTo((EntityPlayerMP) getEntity(), this, MSG_ACTIVATE, tut.id);
                    }
                });

                sync();
            }
        });
    }

    @Override
    public void tick() {
        scheduler.runTick();
    }

    public boolean getActivate(Condition cond) {
        return savedConditions.get(cond.index);
    }

    public void setActivate(Condition cond) {
        if (!savedConditions.get(cond.index)) {
            savedConditions.set(cond.index);
            dirty = true;
        }
    }

    @Override
    public void fromNBT(NBTTagCompound tag) {
        savedConditions = BitSet.valueOf(tag.getByteArray("s"));

        NBTTagList tutsTag = (NBTTagList) tag.getTag("t");
        if (tutsTag != null) {
            IntStream.range(0, tutsTag.tagCount())
                    .mapToObj(tutsTag::getStringTagAt)
                    .forEach(str -> activatedTuts.add(str));
        }
    }

    @Override
    public NBTTagCompound toNBT() {
        NBTTagCompound ret = new NBTTagCompound();
        ret.setByteArray("s", savedConditions.toByteArray());

        NBTTagList tutsTag = new NBTTagList();
        for (String s : activatedTuts) {
            tutsTag.appendTag(new NBTTagString(s));
        }
        ret.setTag("t", tutsTag);

        return ret;
    }

    @Listener(channel=MSG_ACTIVATE, side={Side.CLIENT, Side.SERVER})
    private void onTutorialActivate(String tutName) {
        ACTutorial tut = TutorialRegistry.getTutorial(tutName);
        Preconditions.checkNotNull(tut);

        MinecraftForge.EVENT_BUS.post(new TutorialActivatedEvent(getEntity(), tut));
    }

}