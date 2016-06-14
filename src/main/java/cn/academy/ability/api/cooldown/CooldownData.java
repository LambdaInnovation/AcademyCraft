package cn.academy.ability.api.cooldown;

import cn.academy.ability.api.Controllable;
import cn.academy.ability.api.event.CategoryChangeEvent;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegEventHandler;
import cn.lambdalib.annoreg.mc.RegEventHandler.Bus;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cn.lambdalib.s11n.SerializeIncluded;
import cn.lambdalib.s11n.network.NetworkMessage.Listener;
import cn.lambdalib.s11n.network.NetworkS11n;
import cn.lambdalib.s11n.network.NetworkS11n.ContextException;
import cn.lambdalib.s11n.network.NetworkS11n.NetS11nAdaptor;
import cn.lambdalib.util.datapart.DataPart;
import cn.lambdalib.util.datapart.EntityData;
import cn.lambdalib.util.datapart.RegDataPart;
import cn.lambdalib.util.helper.TickScheduler;
import com.google.common.base.Preconditions;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Handles player cooldown data and update.
 */
@Registrant
@RegDataPart(EntityPlayer.class)
public class CooldownData extends DataPart<EntityPlayer> {

    public static CooldownData of(EntityPlayer player) {
        return EntityData.get(player).getPart(CooldownData.class);
    }

    @RegInitCallback
    private static void _init() {
        NetworkS11n.addDirect(SkillCooldown.class, new NetS11nAdaptor<SkillCooldown>() {
            @Override
            public void write(ByteBuf buf, SkillCooldown obj) {
                buf.writeShort(obj.maxTick).writeShort(obj.tickLeft);
            }

            @Override
            public SkillCooldown read(ByteBuf buf) throws ContextException {
                return new SkillCooldown(buf.readShort(), buf.readShort());
            }
        });
    }

    private static final SkillCooldown EMPTY_COOLDOWN = new SkillCooldown(100, 0);

    @SerializeIncluded
    private Map<Integer, SkillCooldown> cooldownMap = new HashMap<>();
    private final TickScheduler scheduler = new TickScheduler();

    {
        setTick(true);
        setClearOnDeath();

        scheduler.everyTick().run(() -> {
            for (Iterator<SkillCooldown> itr = cooldownMap.values().iterator();
                 itr.hasNext(); ) {
                SkillCooldown cd = itr.next();
                --cd.tickLeft;

                if (cd.tickLeft <= 0) {
                    itr.remove();
                }
            }
        });

        scheduler.every(15).atOnly(Side.SERVER).run(this::sync);
    }

    @Override
    public void tick() {
        scheduler.runTick();
    }

    public void set(Controllable ctrl, int cd) {
        setSub(ctrl, 0, cd);
    }

    /**
     *
     * @param ctrl The skill
     * @param id The sub id for this skill. 0 is reserved for skill itself.
     * @throws IllegalArgumentException if id < 0
     */
    public void setSub(Controllable ctrl, int id, int cd) {
        Preconditions.checkArgument(id >= 0);

        doSet(ctrl, id, cd);

        if (isClient()) {
            sendMessage("cross", ctrl, id, cd);
        } else {
            sendToLocal("cross", ctrl, id, cd);
        }
    }

    public boolean isInCooldown(Controllable ctrl) {
        return isInCooldown(ctrl, 0);
    }

    public boolean isInCooldown(Controllable ctrl, int id) {
        return getSub(ctrl, id) != EMPTY_COOLDOWN;
    }

    public SkillCooldown get(Controllable ctrl) {
        return getSub(ctrl, 0);
    }

    /**
     * @return The cooldown info for a skill. Always not null.
     */
    public SkillCooldown getSub(Controllable ctrl, int id) {
        int sid = toID(ctrl, id);
        return cooldownMap.containsKey(sid) ? cooldownMap.get(sid) : EMPTY_COOLDOWN;
    }

    public void clear() {
        cooldownMap.clear();
    }

    private void doSet(Controllable ctrl, int id, int cd) {
        SkillCooldown data = getSub(ctrl, id);
        if (data == EMPTY_COOLDOWN) {
            cooldownMap.put(toID(ctrl, id), new SkillCooldown(cd, cd));
        } else {
            data.maxTick = Math.max(cd, data.maxTick);
            data.tickLeft = Math.max(cd, data.tickLeft);
        }
    }

    private int toID(Controllable ctrl, int id) {
        return ctrl.getControlID() << 2 + id;
    }

    @Listener(channel="cross", side={Side.CLIENT, Side.SERVER})
    private void hCrossSet(Controllable ctrl, int id, int cd) {
        doSet(ctrl, id, cd);
    }

    public static class SkillCooldown {
        private int tickLeft;
        private int maxTick;

        private SkillCooldown(int maxTick) {
            this(maxTick, maxTick);
        }

        private SkillCooldown(int maxTick, int tickLeft) {
            checkArgument(maxTick > 0);
            this.maxTick = maxTick;
            this.tickLeft = tickLeft;
        }

        public int getTickLeft() {
            return tickLeft;
        }

        public int getMaxTick() {
            return maxTick;
        }
    }

    @Registrant
    public enum _Events {
        @RegEventHandler(Bus.Forge)
        instance;

        @SubscribeEvent
        public void onCategoryChange(CategoryChangeEvent evt) {
            if (!evt.player.worldObj.isRemote) {
                CooldownData.of(evt.player).clear();
            }
        }

    }
}
