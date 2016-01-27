/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
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
