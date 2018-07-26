package cn.academy.terminal.app.settings;

import cn.academy.terminal.App;
import cn.academy.terminal.AppEnvironment;
import cn.academy.terminal.registry.AppRegistration.RegApp;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;

/**
 * @author WeAthFolD
 *
 */
public class AppSettings extends App {

    @RegApp
    public static AppSettings instance = new AppSettings();
    
    private AppSettings() {
        super("settings");
        setPreInstalled();
    }

    @Override
    public AppEnvironment createEnvironment() {
        return new AppEnvironment() {
            @Override
            @SideOnly(Side.CLIENT)
            public void onStart() {
                Minecraft.getMinecraft().displayGuiScreen(new SettingsUI());
            }
        };
    }
}