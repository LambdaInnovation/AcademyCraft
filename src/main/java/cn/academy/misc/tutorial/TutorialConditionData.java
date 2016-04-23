/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.misc.tutorial;

import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.networkcall.s11n.RegSerializable.SerializeField;
import cn.lambdalib.s11n.SerializeIncluded;
import cn.lambdalib.s11n.nbt.NBTS11n;
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

import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * This class simply stores activate data that trigger-type condition needs.
 */
@Registrant
@RegDataPart(EntityPlayer.class)
public class TutorialConditionData extends DataPart<EntityPlayer> {

    static TutorialConditionData get(EntityPlayer player) {
        return EntityData.get(player).getPart(TutorialConditionData.class);
    }

    private static final String MSG_ACTIVATE = "activate";

    @SerializeIncluded
    private BitSet savedConditions = new BitSet();
    @SerializeIncluded
    private HashSet<String> activatedTuts = new HashSet<>();

    private final TickScheduler scheduler = new TickScheduler();
    private boolean dirty;

    public TutorialConditionData() {
        setTick(true);
        setClientNeedSync();
        setNBTStorage();

        scheduler.every(3).atOnly(Side.SERVER).run(() ->
        {
            if (dirty) {
                dirty = false;
                TutorialRegistry.enumeration().forEach(tut -> {
                    if (!activatedTuts.contains(tut.id) &&
                            tut.isActivated(getEntity()) &&
                            !tut.isDefaultInstalled()) {
                        activatedTuts.add(tut.id);

                        onTutorialActivate(tut.id);
                        sendToLocal(MSG_ACTIVATE, tut.id);
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

    boolean isCondActivate(int index) {
        return savedConditions.get(index);
    }

    void setCondActivate(int index) {
        checkSide(Side.SERVER);

        if (!savedConditions.get(index)) {
            savedConditions.set(index);
            dirty = true;
        }
    }

    @Override
    public void toNBT(NBTTagCompound tag) {
        NBTS11n.write(tag, this);
    }

    @Override
    public void fromNBT(NBTTagCompound tag) {
        NBTS11n.read(tag, this);
    }

    @Listener(channel=MSG_ACTIVATE, side={Side.CLIENT, Side.SERVER})
    private void onTutorialActivate(String tutName) {
        ACTutorial tut = TutorialRegistry.getTutorial(tutName);
        Preconditions.checkNotNull(tut);

        MinecraftForge.EVENT_BUS.post(new TutorialActivatedEvent(getEntity(), tut));
    }

}