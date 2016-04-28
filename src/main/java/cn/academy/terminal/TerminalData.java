/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.terminal;

import cn.academy.terminal.event.AppInstalledEvent;
import cn.academy.terminal.event.TerminalInstalledEvent;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.networkcall.Future;
import cn.lambdalib.networkcall.Future.FutureCallback;
import cn.lambdalib.networkcall.RegNetworkCall;
import cn.lambdalib.networkcall.s11n.RegSerializable.SerializeField;
import cn.lambdalib.networkcall.s11n.StorageOption;
import cn.lambdalib.networkcall.s11n.StorageOption.Data;
import cn.lambdalib.networkcall.s11n.StorageOption.Target;
import cn.lambdalib.s11n.SerializeIncluded;
import cn.lambdalib.s11n.nbt.NBTS11n;
import cn.lambdalib.s11n.network.NetworkMessage;
import cn.lambdalib.s11n.network.NetworkMessage.Listener;
import cn.lambdalib.util.datapart.DataPart;
import cn.lambdalib.util.datapart.EntityData;
import cn.lambdalib.util.datapart.RegDataPart;
import com.google.common.collect.ImmutableList;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;

import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author WeAthFolD
 */
@Registrant
@RegDataPart(EntityPlayer.class)
public class TerminalData extends DataPart<EntityPlayer> {

    public static TerminalData get(EntityPlayer player) {
        return EntityData.get(player).getPart(TerminalData.class);
    }

    @SerializeIncluded
    private BitSet installedList = new BitSet();
    @SerializeIncluded
    private boolean isInstalled;

    public TerminalData() {
        setClientNeedSync();
        setNBTStorage();
    }

    public List<App> getInstalledApps() {
        return AppRegistry.enumeration().stream().filter(this::isInstalled).collect(Collectors.toList());
    }

    public boolean isInstalled(App app) {
        return app.isPreInstalled() || installedList.get(app.getID());
    }

    public boolean isTerminalInstalled() {
        return isInstalled;
    }

    /**
     * Server only. Installs the data terminal.
     */
    public void install() {
        checkSide(Side.SERVER);

        if (!isInstalled) {
            isInstalled = true;

            sync();

            informTerminalInstall();
            NetworkMessage.sendTo(getEntity(), this, "terminal_inst");
        }
    }

    /**
     * Server only. Installs the given app.
     */
    public void installApp(App app) {
        checkSide(Side.SERVER);

        if (!isInstalled(app)) {
            int id = app.getID();

            installedList.set(id, true);

            sync();

            informAppInstall(id);
            NetworkMessage.sendTo(getEntity(), this, "app_inst", id);
        }
    }

    @Override
    public void fromNBT(NBTTagCompound tag) {
        NBTS11n.read(tag, this);
    }

    @Override
    public void toNBT(NBTTagCompound tag) {
        NBTS11n.write(tag, this);
    }

    @Listener(channel="terminal_inst", side=Side.CLIENT)
    private void informTerminalInstall() {
        MinecraftForge.EVENT_BUS.post(new TerminalInstalledEvent(getEntity()));
    }

    @Listener(channel="app_inst", side=Side.CLIENT)
    private void informAppInstall(int appid) {
        MinecraftForge.EVENT_BUS.post(new AppInstalledEvent(getEntity(), AppRegistry.enumeration().get(appid)));
    }

}
