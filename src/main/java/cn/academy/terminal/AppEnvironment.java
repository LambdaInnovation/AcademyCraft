package cn.academy.terminal;

import cn.academy.client.auxgui.TerminalUI;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
public class AppEnvironment {

    /*
     * Instances to be injected when startup
     */
    public App app;
    public TerminalUI terminal;

    /**
     * Called just before environment is activated on client side. Load the
     * data.
     */
    public void onStart() {
    }

    protected App getApp() {
        return app;
    }

    protected TerminalUI getTerminal() {
        return terminal;
    }

    protected EntityPlayer getPlayer() {
        return Minecraft.getMinecraft().player;
    }

}