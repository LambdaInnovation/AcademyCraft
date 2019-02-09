package cn.academy.terminal.app;

import cn.academy.Resources;
import cn.academy.terminal.App;
import cn.academy.terminal.AppEnvironment;
import cn.academy.terminal.RegApp;
import cn.lambdalib2.cgui.CGuiScreen;
import cn.lambdalib2.cgui.WidgetContainer;
import cn.lambdalib2.cgui.loader.CGUIDocument;
import cn.lambdalib2.util.Debug;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public class AppAbout extends App {

    @RegApp(priority = -2)
    private static final AppAbout instance = new AppAbout();

    public AppAbout() {
        super("about");
        setPreInstalled();
    }

    @Override
    public AppEnvironment createEnvironment() {
        return new AppEnvironment() {
            @Override
            public void onStart() {
                Minecraft.getMinecraft().displayGuiScreen(new AboutUI());
            }
        };
    }

    static class AboutUI extends CGuiScreen {

        private static final WidgetContainer prefab = CGUIDocument.read(Resources.getGui("about"));

        AboutUI() {
            gui.addWidget("main", prefab.getWidget("main").copy());
        }

    }

}
