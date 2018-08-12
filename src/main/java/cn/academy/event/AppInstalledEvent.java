package cn.academy.event;

import cn.academy.terminal.App;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Fired in both client and server when any app is installed, disregarding pre-installed apps.
 * @author WeAthFolD
 */
public class AppInstalledEvent extends Event {
    
    public final EntityPlayer player;
    public final App app;
    
    public AppInstalledEvent(EntityPlayer _player, App _app) {
        player = _player;
        app = _app;
    }
    
}