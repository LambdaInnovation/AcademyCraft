package cn.academy.ability.api.context;

import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegEventHandler;
import cn.lambdalib.annoreg.mc.RegEventHandler.Bus;
import cn.lambdalib.util.client.ClientUtils;
import cn.lambdalib.util.datapart.DataPart;
import cn.lambdalib.util.datapart.EntityData;
import cn.lambdalib.util.datapart.RegDataPart;
import cn.lambdalib.util.key.KeyManager;
import com.google.common.collect.*;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import java.util.*;
import java.util.Map.Entry;

/**
 * Player control info at client runtime. This class handles all active key delegates at client.
 */
@Registrant
@SideOnly(Side.CLIENT)
@RegDataPart("AC_ClientRuntime")
public class ClientRuntime extends DataPart<EntityPlayer> {

    public static final String DEFAULT_GROUP = "def";

    public static ClientRuntime instance() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        return player == null ? null : EntityData.get(player).getPart(ClientRuntime.class);
    }

    private final List<DelegateNode> delegates = new LinkedList<>();
    private final Multimap<String, DelegateNode> delegateGroups = ArrayListMultimap.create();

    private final Map<Object, CooldownData> cooldown = new HashMap<>();

    public void addKey(int keyID, KeyDelegate delegate) {
        addKey(DEFAULT_GROUP, keyID, delegate);
    }

    public void addKey(String group, int keyID, KeyDelegate delegate) {
        DelegateNode node = new DelegateNode(delegate, keyID);
        delegates.add(node);
        delegateGroups.put(group, node);
    }

    public void clearKeys(String group) {
        Collection<DelegateNode> nodes = delegateGroups.get(group);

        nodes.stream()
                .filter(n -> n.state)
                .forEach(n -> n.delegate.onKeyAbort());

        delegates.removeAll(nodes);
        delegateGroups.removeAll(group);
    }

    public void setCooldown(KeyDelegate delegate, int cd) {
        setCooldownRaw(delegate.getIdentifier(), cd);
    }

    public void setCooldownRaw(Object id, int cd) {
        if(isInCooldownRaw(id)) {
            CooldownData data = cooldown.get(id);
            if(data.max < cd) data.max = cd;
            data.current = Math.max(cd, data.current);
        } else {
            cooldown.put(id, new CooldownData(cd));
        }
    }

    public boolean isInCooldown(KeyDelegate delegate) {
        return isInCooldownRaw(delegate.getIdentifier());
    }

    public CooldownData getCooldown(KeyDelegate delegate) {
        return getCooldownDataRaw(delegate.getIdentifier());
    }

    public boolean isInCooldownRaw(Object id) {
        return cooldown.containsKey(id);
    }

    public CooldownData getCooldownDataRaw(Object id) {
        return cooldown.get(id);
    }

    public void clearCooldown() {
        cooldown.clear();
    }

    /**
     * @return An immutable map of the raw cooldown data.
     */
    public ImmutableMap<Object, CooldownData> getCooldownRawData() {
        return ImmutableMap.copyOf(cooldown);
    }

    /**
     * @return An immutable list of raw delegate data.
     */
    public ImmutableList<DelegateNode> getDelegateRawData() {
        return ImmutableList.copyOf(delegates);
    }

    @Override
    public void fromNBT(NBTTagCompound tag) {}

    @Override
    public NBTTagCompound toNBT() {
        return null;
    }

    public class DelegateNode {
        public final KeyDelegate delegate;
        public final int keyID;

        boolean state = false;

        DelegateNode(KeyDelegate _delegate, int _keyID) {
            delegate = _delegate;
            keyID = _keyID;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof DelegateNode) {
                return ((DelegateNode) other).delegate == delegate;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }
    }

    @RegEventHandler(Bus.FML)
    public static class Events {

        @SideOnly(Side.CLIENT)
        @SubscribeEvent
        public void onClientTick(ClientTickEvent evt) {
            if (evt.phase == Phase.END) return;

            final ClientRuntime rt = ClientRuntime.instance();
            if (rt != null) {
                // Update key delegate
                rt.delegates.forEach(node -> {
                    final boolean keyDown = KeyManager.getKeyDown(node.keyID);
                    final boolean shouldAbort = ClientUtils.isPlayerInGame();
                    final KeyDelegate delegate = node.delegate;

                    if (keyDown && node.state && !shouldAbort) {
                        delegate.onKeyTick();
                    }

                    if (keyDown && !node.state && !shouldAbort) {
                        delegate.onKeyDown();
                        node.state = true;
                    }

                    if (!keyDown && node.state && !shouldAbort) {
                        delegate.onKeyUp();
                        node.state = false;
                    }

                    if (node.state && shouldAbort) {
                        delegate.onKeyAbort();
                        node.state = false;
                    }
                });

                // Update cooldown
                Iterator<Entry<Object, CooldownData>> iter = rt.cooldown.entrySet().iterator();

                while(iter.hasNext()) {
                    Entry< Object, CooldownData > entry = iter.next();
                    CooldownData data = entry.getValue();
                    if(data.current <= 0) {
                        iter.remove();
                    } else {
                        data.current -= 1;
                    }
                }
            }
        }

        @SideOnly(Side.CLIENT)
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void playerDeath(LivingDeathEvent event) {
            final EntityPlayer localPlayer = Minecraft.getMinecraft().thePlayer;
            final ClientRuntime rt = ClientRuntime.instance();
            if(rt != null && event.entityLiving.equals(localPlayer)) {
                rt.clearCooldown();
            }
        }

    }

    public static class CooldownData {
        private int current;
        private int max;

        public CooldownData(int time) {
            current = max = time;
        }

        /**
         * @return How many ticks until the cooldown is end
         */
        public int getTickLeft() {
            return current;
        }

        /**
         * @return the cooldown time specified at the start of the cooldown progress.
         */
        public int getMaxTick() {
            return max;
        }
    }
}
