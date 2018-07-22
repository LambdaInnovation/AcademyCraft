package cn.academy.core;

import cn.academy.core.event.ConfigModifyEvent;
import cn.academy.terminal.app.settings.PropertyElements;
import cn.academy.terminal.app.settings.SettingsUI;
import cn.lambdalib2.input.KeyHandler;
import cn.lambdalib2.input.KeyManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
public class ModuleCoreClient {

    public static KeyManager keyManager = new ACKeyManager();

    public static KeyManager dynKeyManager = new KeyManager();

    public static class ACKeyManager extends KeyManager {

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
