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
package cn.academy.core;

import cn.academy.core.event.ConfigModifyEvent;
import cn.academy.terminal.app.settings.PropertyElements;
import cn.academy.terminal.app.settings.SettingsUI;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.util.key.KeyHandler;
import cn.lambdalib.util.key.KeyManager;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
@Registrant
public class ModuleCoreClient {

    public static KeyManager keyManager = new ACKeyManager();

    public static KeyManager dynKeyManager = new KeyManager();

    public static class ACKeyManager extends KeyManager {
        {
            MinecraftForge.EVENT_BUS.register(this);
        }

        @Override
        protected Configuration getConfig() {
            return AcademyCraft.config;
        }

        @SubscribeEvent
        public void onConfigModified(ConfigModifyEvent event) {
            if (event.property.isIntValue())
                resetBindingKey(event.property.getName(), event.property.getInt());
        }

        @Override
        public void addKeyHandler(String name, String keyDesc, int defKeyID, boolean global, KeyHandler handler) {
            super.addKeyHandler(name, keyDesc, defKeyID, global, handler);
            SettingsUI.addProperty(PropertyElements.KEY, "keys", name, defKeyID, false);
        }
    }

}
