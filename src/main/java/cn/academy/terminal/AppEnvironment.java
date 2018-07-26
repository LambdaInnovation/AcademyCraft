package cn.academy.terminal;

import cn.academy.terminal.client.TerminalUI;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

/**
 * @author WeAthFolD
 */
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

    @SideOnly(Side.CLIENT)
    protected EntityPlayer getPlayer() {
        return Minecraft.getMinecraft().thePlayer;
    }

}