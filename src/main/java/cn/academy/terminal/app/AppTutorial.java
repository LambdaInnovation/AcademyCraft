package cn.academy.terminal.app;

import cn.academy.Resources;
import cn.academy.client.gui.GuiTutorial;
import cn.academy.terminal.App;
import cn.academy.terminal.AppEnvironment;
import cn.academy.terminal.RegApp;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class AppTutorial extends App {
    @RegApp
    private static final AppTutorial instance = new AppTutorial();

    public AppTutorial() {
        super("tutorial");
        setPreInstalled();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AppEnvironment createEnvironment() {
        return new AppEnvironment() {
            @Override
            @SideOnly(Side.CLIENT)
            public void onStart() {
                Minecraft.getMinecraft().displayGuiScreen(new GuiTutorial());
            }
        };
    }

    // Random gives icon for more fun >)
    @Override
    @SideOnly(Side.CLIENT)
    public ResourceLocation getIcon() {
        float rand = RandUtils.nextFloat();
        if (rand < 0.2f) {
            return icon(0);
        } else if (rand < 0.3f) {
            return icon(1);
        } else {
            return icon(2);
        }
    }

    @SideOnly(Side.CLIENT)
    private ResourceLocation icon(int id) {
        return Resources.preloadMipmapTexture("guis/apps/tutorial/icon_" + id);
    }
}
