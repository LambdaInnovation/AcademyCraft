/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.terminal;

import cn.academy.terminal.event.TerminalInstalledEvent;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.networkcall.Future;
import cn.lambdalib.networkcall.Future.FutureCallback;
import cn.lambdalib.networkcall.RegNetworkCall;
import cn.lambdalib.networkcall.s11n.StorageOption;
import cn.lambdalib.networkcall.s11n.StorageOption.Data;
import cn.lambdalib.networkcall.s11n.StorageOption.Target;
import cn.lambdalib.util.datapart.DataPart;
import cn.lambdalib.util.datapart.EntityData;
import cn.lambdalib.util.datapart.RegDataPart;
import com.google.common.collect.ImmutableList;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author WeAthFolD
 */
@Registrant
@RegDataPart("terminal")
public class TerminalData extends DataPart<EntityPlayer> {

    private Set<Integer> installedList = new HashSet<>();
    private boolean isInstalled;

    public TerminalData() {
        for (App app : AppRegistry.enumeration()) {
            if (app.isPreInstalled())
                installedList.add(app.getID());
        }
    }

    public List<Integer> getInstalledApps() {
        return ImmutableList.copyOf(installedList);
    }

    public boolean isInstalled(int appid) {
        return installedList.contains(appid);
    }

    public boolean isInstalled(App app) {
        return isInstalled(app.getID());
    }

    public boolean isTerminalInstalled() {
        return isInstalled;
    }

    public void install() {
        checkSide(false);
        if (!isInstalled) {
            isInstalled = true;
            sync();

            MinecraftForge.EVENT_BUS.post(new TerminalInstalledEvent(getEntity()));
            informInstallAtClient(getEntity());
        }
    }

    /**
     * Make a sync query from client and call the callback when received sync.
     */
    @SideOnly(Side.CLIENT)
    public void querySync(final FutureCallback callback) {
        doQuerySync(Future.create(new FutureCallback() {

            @Override
            public void onReady(Object val) {
                callback.onReady(val);
                fromNBTSync((NBTTagCompound) val);
            }

        }));
    }

    public void installApp(App app) {
        installApp(app.getID());
    }

    /**
     * Must called in SERVER side.
     */
    public void installApp(int appid) {
        if (isRemote()) {
            throw new RuntimeException("Not allowed in client side!");
        }
        doInstall(appid);
        installSync(appid);
    }

    public static TerminalData get(EntityPlayer player) {
        return EntityData.get(player).getPart(TerminalData.class);
    }

    @Override
    public void tick() {
    }

    @Override
    public void fromNBT(NBTTagCompound tag) {
        isInstalled = tag.getBoolean("i");

        int[] arr = tag.getIntArray("learned");
        for (int i = 0; i < arr.length; ++i)
            installedList.add(arr[i]);
    }

    @Override
    public NBTTagCompound toNBT() {
        NBTTagCompound ret = new NBTTagCompound();

        Integer[] iarr = installedList.toArray(new Integer[0]);
        int[] arr = new int[iarr.length];
        for (int i = 0; i < arr.length; ++i)
            arr[i] = iarr[i];

        ret.setIntArray("learned", arr);

        ret.setBoolean("i", isInstalled);

        return ret;
    }

    @RegNetworkCall(side = Side.SERVER, thisStorage = StorageOption.Option.INSTANCE)
    private void doQuerySync(@Data Future future) {
        future.setAndSync(toNBTSync());
    }

    @RegNetworkCall(side = Side.SERVER, thisStorage = StorageOption.Option.INSTANCE)
    public void clientSync(@Target EntityPlayer player, @Data NBTTagCompound tag) {
        fromNBT(tag);
    }

    @RegNetworkCall(side = Side.CLIENT, thisStorage = StorageOption.Option.INSTANCE)
    private void installSync(@Data Integer appid) {
        doInstall(appid);
    }

    @RegNetworkCall(side = Side.CLIENT)
    private static void informInstallAtClient(@Target EntityPlayer player) {
        MinecraftForge.EVENT_BUS.post(new TerminalInstalledEvent(player));
    }

    private void doInstall(Integer appid) {
        installedList.add(appid);
    }
}
