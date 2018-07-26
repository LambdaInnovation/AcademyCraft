package cn.academy.terminal;

import cn.academy.core.Resources;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

/**
 */
public abstract class App {

    int appid;
    private final String name;
    protected ResourceLocation icon;

    private boolean preInstalled = false;

    public App(String _name) {
        name = _name;
        icon = getTexture("icon");
    }

    protected ResourceLocation getTexture(String texname) {
        return Resources.getTexture("guis/apps/" + name + "/" + texname);
    }

    protected String local(String key) {
        return StatCollector.translateToLocal("ac.app." + name + "." + key);
    }

    public ResourceLocation getIcon() {
        return icon;
    }

    public App setPreInstalled() {
        preInstalled = true;
        return this;
    }

    public int getID() {
        return appid;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return local("name");
    }

    public final boolean isPreInstalled() {
        return preInstalled;
    }

    void getEnvironment() {
        AppEnvironment env = createEnvironment();
        env.app = this;
    }

    public abstract AppEnvironment createEnvironment();

}