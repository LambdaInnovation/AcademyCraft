package cn.academy.ability.vanilla.teleporter.util;

import cn.academy.ability.vanilla.util.HandlerLifePeroidEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * @author WeAthFolD
 */
public class GravityCancellor extends HandlerLifePeroidEvent
{
    private final EntityPlayer p;

    public GravityCancellor(EntityPlayer _p, int _ticks) {
        super(_ticks);
        p = _p;
    }

    public final boolean isDead() {
        return tick >= maxTick;
    }

    public final void setDead() {
        this.tick = maxTick;
    }

    public final void reset() {
        this.tick = 0;
    }

    @Override
    public boolean onTick() {
        if (!p.capabilities.isFlying) {
            if (!p.onGround) {
                p.motionY += 0.072;
            }
        }
        return true;
    }

    @Override
    public void onDeath(){
        MinecraftForge.EVENT_BUS.unregister(this);
    }

}