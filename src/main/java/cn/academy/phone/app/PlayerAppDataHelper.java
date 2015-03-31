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
package cn.academy.phone.app;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import cn.academy.phone.event.AppInfoUpdateEvent;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.StorageOption.Data;
import cn.annoreg.mc.s11n.StorageOption.Instance;
import cn.annoreg.mc.s11n.StorageOption.Target;
import cpw.mods.fml.relauncher.Side;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
public class PlayerAppDataHelper {

    public static int[] getInstalledApp(EntityPlayer player) {
        int[] arr = player.getEntityData().getIntArray("installedApp");
        if(arr.length == 0) {
            List<App> preInstalled = AppRegistry.instance.getAppListFor(0);
            arr = new int[] { preInstalled.size() };
            for(int i = 0; i < arr.length; ++i) {
                arr[i] = preInstalled.get(i).getID();
            }
        }
        return arr;
    }
    
    public static void installApp(EntityPlayer player, App app) {
        
    }
    
    @RegNetworkCall(side = Side.SERVER)
    public static void syncRequest(@Instance EntityPlayer player) {
        retrieveSync(player, getInstalledApp(player));
    }
    
    @RegNetworkCall(side = Side.CLIENT)
    public static void retrieveSync(@Target EntityPlayer player, @Data int[] list) {
        MinecraftForge.EVENT_BUS.post(new AppInfoUpdateEvent());
        player.getEntityData().setIntArray("installedApp", list);
    }

}
