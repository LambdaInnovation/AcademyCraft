/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.api.context;

import cn.academy.ability.api.Controllable;
import cn.academy.ability.api.ctrl.ClientHandler;
import cn.academy.ability.api.data.CPData;
import cn.academy.ability.api.data.PresetData;
import cn.academy.ability.api.data.PresetData.Preset;
import cn.academy.ability.api.event.*;
import cn.academy.core.AcademyCraft;
import cn.academy.core.ModuleCoreClient;
import cn.academy.terminal.client.TerminalUI;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegEventHandler;
import cn.lambdalib.annoreg.mc.RegEventHandler.Bus;
import cn.lambdalib.util.client.ClientUtils;
import cn.lambdalib.util.client.auxgui.AuxGuiHandler;
import cn.lambdalib.util.datapart.DataPart;
import cn.lambdalib.util.datapart.EntityData;
import cn.lambdalib.util.datapart.RegDataPart;
import cn.lambdalib.util.key.KeyManager;
import cn.lambdalib.util.mc.ControlOverrider;
import cn.lambdalib.util.mc.SideHelper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Handles objects in client that is player-local and dynamic - Ability keys, cooldown and stuff.
 */
@Registrant
@SideOnly(Side.CLIENT)
@RegDataPart("AC_ClientRuntime")
public class ClientRuntime extends DataPart<EntityPlayer> {

    public static final String DEFAULT_GROUP = "def";

    public static ClientRuntime instance() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        Preconditions.checkNotNull(player);
        return EntityData.get(player).getPart(ClientRuntime.class);
    }

    public static boolean available() {
        return Minecraft.getMinecraft().thePlayer != null;
    }

    private final Map<Integer, DelegateNode> delegates = new TreeMap<>(); // Preserve insersion order for rendering
    private final Multimap<String, DelegateNode> delegateGroups = ArrayListMultimap.create();
    private final Map<Integer, KeyState> keyStates = new HashMap<>();

    private final Map<Object, CooldownData> cooldown = new HashMap<>();

    private final LinkedList<IActivateHandler> activateHandlers = new LinkedList<>();

    private boolean ctrlDirty = true;

    /**
     * Adds a key delegate into default group
     */
    public void addKey(int keyID, KeyDelegate delegate) {
        addKey(DEFAULT_GROUP, keyID, delegate);
    }

    /**
     * Adds a key delegate with specified group. Note that the delegate with same key musn't be previously present, or
     *  yields an error.
     */
    public void addKey(String group, int keyID, KeyDelegate delegate) {
        // Using same key multiple times is currently not supported.
        Preconditions.checkState(!delegateGroups.containsKey(keyID));

        DelegateNode node = new DelegateNode(delegate, keyID);
        delegates.put(keyID, node);
        delegateGroups.put(group, node);
        if (!keyStates.containsKey(keyID)) {
            keyStates.put(keyID, new KeyState());
        }

        ctrlDirty = true;
    }

    public void clearKeys(String group) {
        Collection<DelegateNode> nodes = delegateGroups.get(group);

        abortDelegates();

        delegates.values().removeAll(nodes);
        delegateGroups.removeAll(group);

        ctrlDirty = true;

        rebuildOverrides();
    }

    public void clearAllKeys() {
        List<String> all = new ArrayList<>();
        all.addAll(delegateGroups.keySet());

        for (String s : all) {
            clearKeys(s);
        }

        rebuildOverrides();
    }

    public boolean hasActiveDelegate() {
        return delegates.values().stream().anyMatch(node -> keyStates.get(node.keyID).state);
    }

    public void abortDelegates() {
        keyStates.entrySet().stream()
                .filter(e -> e.getValue().state)
                .forEach(e -> {
                    KeyState state = e.getValue();
                    state.state = false;
                    if (delegates.containsKey(e.getKey())) {
                        delegates.get(e.getKey()).delegate.onKeyAbort();
                    }
                });
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
     * @return A view of raw delegate data. Modifying yields undefined results.
     */
    public Multimap<String, DelegateNode> getDelegateRawData() {
        return delegateGroups;
    }

    /**
     * Adds an activation key handler. The handler is of the highest priority. Allows behaviour alternation
     *  of activation key.
     */
    public void addActivateHandler(IActivateHandler handler) {
        activateHandlers.addFirst(handler);
    }

    /**
     * Removes an activation key handler.
     */
    public void removeActiveHandler(IActivateHandler handler) {
        activateHandlers.remove(handler);
    }

    public IActivateHandler getActivateHandler() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        for(IActivateHandler h : activateHandlers) {
            if(h.handles(player))
                return h;
        }
        throw new RuntimeException();
    }

    { // Default activate handlers
        addActivateHandler(new IActivateHandler() {
            @Override
            public boolean handles(EntityPlayer player) {
                return true;
            }

            @Override
            public void onKeyDown(EntityPlayer player) {
                CPData cpData = CPData.get(player);
                if(cpData.isActivated()) {
                    cpData.deactivate();
                } else {
                    cpData.activate();
                }
            }

            @Override
            public String getHint() {
                return null;
            }
        });

        addActivateHandler(new IActivateHandler() {
            @Override
            public boolean handles(EntityPlayer player) {
                return PresetData.get(player).isOverriding();
            }

            @Override
            public void onKeyDown(EntityPlayer player) {
                PresetData.get(player).endOverride();
            }

            @Override
            public String getHint() {
                return "endspecial";
            }
        });

        addActivateHandler(new IActivateHandler() {
            @Override
            public boolean handles(EntityPlayer player) {
                return ClientRuntime.instance().hasActiveDelegate();
            }

            @Override
            public void onKeyDown(EntityPlayer player) {
                ClientRuntime.instance().abortDelegates();
            }

            @Override
            public String getHint() {
                return "endskill";
            }
        });
    }

    private Integer[] lastOverrides = new Integer[0];

    private void rebuildOverrides() {
        AcademyCraft.debug("RebuildOverrides");

        ctrlDirty = false;

        for(int i : lastOverrides)
            ControlOverrider.removeOverride(i);

        Set<Integer> set = delegates.values().stream().map(n -> n.keyID).collect(Collectors.toSet());
        lastOverrides = set.toArray(new Integer[set.size()]);

        for(int i : lastOverrides)
            ControlOverrider.override(i);
    }

    @Override
    public void fromNBT(NBTTagCompound tag) {}

    @Override
    public NBTTagCompound toNBT() {
        return null;
    }

    private class KeyState {
        boolean state = false;
        boolean realState = false;
    }

    public class DelegateNode {
        public final KeyDelegate delegate;
        public final int keyID;

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

    @RegEventHandler({Bus.FML, Bus.Forge})
    public static class Events {

        boolean requireFlush = false;

        @SubscribeEvent
        public void onClientTick(ClientTickEvent evt) {
            if (evt.phase == Phase.END) return;

            if (ClientRuntime.available()) {
                final ClientRuntime rt = ClientRuntime.instance();
                final Set<Integer> availKeys = rt.delegates.keySet();
                final CPData cpData = CPData.get(rt.getEntity());

                Iterator<Entry<Integer, KeyState>> itr = rt.keyStates.entrySet().iterator();
                while (itr.hasNext()) {
                    Entry<Integer, KeyState> entry = itr.next();
                    final KeyState state = entry.getValue();
                    final int keyid = entry.getKey();
                    final boolean avail = availKeys.contains(entry.getKey());

                    if (!state.realState && !avail) {
                        itr.remove();
                    } else {
                        final boolean keyDown = KeyManager.getKeyDown(keyid);

                        if (avail) {
                            DelegateNode node = rt.delegates.get(keyid);
                            // TODO a more elegant way to handle key disabling?
                            boolean shouldAbort =
                                    !ClientUtils.isPlayerInGame() || rt.isInCooldown(node.delegate) ||
                                            !cpData.canUseAbility() ||
                                            AuxGuiHandler.active().stream().anyMatch(a -> a instanceof TerminalUI);
                            final KeyDelegate delegate = node.delegate;

                            if (keyDown && state.state && !shouldAbort) {
                                delegate.onKeyTick();
                            }
                            if (keyDown && !state.state && !state.realState && !shouldAbort) {
                                delegate.onKeyDown();
                                state.state = true;
                            }
                            if (!keyDown && state.state && !shouldAbort) {
                                delegate.onKeyUp();
                                state.state = false;
                            }
                            if (state.state && shouldAbort) {
                                delegate.onKeyAbort();
                                state.state = false;
                            }
                        }

                        state.realState = keyDown;
                    }
                }

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

                // Update override
                if (rt.ctrlDirty) {
                    rt.rebuildOverrides();
                }

                if (requireFlush) {
                    requireFlush = false;
                    updateDefaultGroup();
                }
            }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void playerDeath(LivingDeathEvent event) {
            if (SideHelper.isClient()) {
                final EntityPlayer localPlayer = Minecraft.getMinecraft().thePlayer;
                final ClientRuntime rt = ClientRuntime.instance();
                if(rt != null && event.entityLiving.equals(localPlayer)) {
                    rt.clearCooldown();
                }

                requireFlush = true;
            }
        }

        @SubscribeEvent
        public void presetSwitch(PresetSwitchEvent evt) {
            updateDefaultGroup();
        }

        @SubscribeEvent
        public void presetEdit(PresetUpdateEvent evt) {
            if (SideHelper.isClient()) {
                updateDefaultGroup();
            }
        }

        @SubscribeEvent
        public void activateAbility(AbilityActivateEvent evt) {
            if (SideHelper.isClient()) {
                updateDefaultGroup();
            }
        }

        @SubscribeEvent
        public void deactivateAbility(AbilityDeactivateEvent evt) {
            if (SideHelper.isClient()) {
                ClientRuntime.instance().clearAllKeys();
            }
        }

        @SubscribeEvent
        public void flushControl(FlushControlEvent evt) {
            requireFlush = true;
        }

        private void updateDefaultGroup() {
            ClientRuntime rt = ClientRuntime.instance();
            if (rt == null) return;

            rt.clearKeys(DEFAULT_GROUP);

            if (CPData.get(rt.getEntity()).isActivated()) {
                Preset preset = PresetData.get(rt.getEntity()).getCurrentPreset();

                for (int i = 0; i < PresetData.MAX_PRESETS; ++i) {
                    if (preset.hasMapping(i)) {
                        Controllable c = preset.getControllable(i);
                        c.activate(rt, ClientHandler.getKeyMapping(i));
                    }
                }
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

    public interface IActivateHandler {
        String ENDSPECIAL = "endspecial";

        boolean handles(EntityPlayer player);
        void onKeyDown(EntityPlayer player);
        String getHint();

        default Optional<String> getHintTranslated() {
            String kname = KeyManager.getKeyName(ModuleCoreClient.keyManager.getKeyID(ClientHandler.keyActivate));
            String hint = ClientRuntime.instance().getActivateHandler().getHint();
            return hint == null ? Optional.empty() : Optional.of("[" + kname + "]: " + StatCollector.translateToLocal(
                    "ac.activate_key." + hint + ".desc"));
        }

    }
}
