package cn.academy.ability.vanilla.teleporter.util;

import cn.lambdalib2.util.ClientUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;

/**
 * @author WeAthFolD
 */
public class GravityCancellor {//extends LIHandler<ClientTickEvent> {
    private boolean dead = false;
    private final EntityPlayer p;
    private final int ticks;
    private int ticker = 0;

    public GravityCancellor(EntityPlayer _p, int _ticks) {
        p = _p;
        ticks = _ticks;
    }

    public final boolean isDead() {
        return this.dead;
    }

    public final void setDead() {
        this.dead = true;
    }

    public final void setAlive() {
        this.dead = false;
    }

    //@Override
    @SubscribeEvent
    protected boolean onEvent(ClientTickEvent event) {
        if(this.dead) {
            MinecraftForge.EVENT_BUS.unregister(this);
            return true;
        }
        if (event.phase == TickEvent.Phase.START && ClientUtils.isPlayerPlaying()) {
            if (p.isDead || (++ticker == ticks)) {
                this.setDead();
            } else {
                if (!p.capabilities.isFlying) {
                    if (!p.onGround) {
                        p.motionY += 0.072;
                    }
                }
            }
        }
        return true;
    }

    public final void trigger(ClientTickEvent event) {
        if (!this.onEvent(event)) {
            throw new RuntimeException("Unexpected event(" + event.getClass().getName() + ") for " + this.getClass().getName());
        }
    }

}