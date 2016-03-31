/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.api.context;

import cn.academy.ability.api.Controllable;
import cn.academy.ability.api.cooldown.CooldownManager;
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
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StatCollector;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Handles objects in client that is player-local and dynamic - Ability keys, cooldown and stuff.
 * @author EAirPeter, WeAthFolD
 */
@Registrant
@SideOnly(Side.CLIENT)
@RegDataPart(value=EntityPlayer.class, side=Side.CLIENT)
public class ClientRuntime extends DataPart<EntityPlayer> {

    public static final String DEFAULT_GROUP = "def";
    private static final String OVERRIDE_GROUP = "AC_ClientRuntime";

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

    private final Map<Integer, CooldownData> cooldown = new HashMap<>();
    private final Set<Integer> cooldownServer = new HashSet<>();

    private final LinkedList<IActivateHandler> activateHandlers = new LinkedList<>();

    private boolean ctrlDirty = true;

    {
        setTick(true);
        setClearOnDeath();
    }

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

        ctrlDirty = true;
    }

    public Collection<KeyDelegate> getDelegates(String group) {
        return delegateGroups.get(group).stream()
                .map(node -> node.delegate)
                .collect(Collectors.toList());
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
        return delegates.values().stream().anyMatch(node -> getKeyState(node.keyID).state);
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

    private KeyState getKeyState(int keyID) {
        if (keyStates.containsKey(keyID)) {
            return keyStates.get(keyID);
        } else {
            KeyState ret = new KeyState();
            keyStates.put(keyID, ret);
            return ret;
        }
    }

    public void setCooldownRawFromServer(int id, int cd) {
        if (cd == 0)
            cooldownServer.remove(id);
        else {
            cooldownServer.add(id);
            if(isInCooldownRaw(id)) {
                CooldownData data = cooldown.get(id);
                if(data.max < cd) data.max = cd;
                data.current = Math.max(cd, data.current);
            } else {
                cooldown.put(id, new CooldownData(cd));
            }
        }
    }

    public boolean isInCooldown(KeyDelegate delegate) {
        return isInCooldownRaw(delegate.getIdentifier());
    }

    public CooldownData getCooldown(KeyDelegate delegate) {
        return getCooldownDataRaw(delegate.getIdentifier());
    }

    public boolean isInCooldownRaw(int id) {
        return cooldown.containsKey(id);
    }

    public CooldownData getCooldownDataRaw(int id) {
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
                cpData.setActivateState(!cpData.isActivated());
            }

            @Override
            public String getHint() {
                return null;
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

        int[] set = delegates.values().stream().mapToInt(n -> n.keyID).toArray();
        ControlOverrider.override(OVERRIDE_GROUP, set);
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
                final CPData cpData = CPData.get(rt.getEntity());

                for (DelegateNode node : rt.delegates.values()) {
                    final KeyState state = rt.getKeyState(node.keyID);
                    final boolean keyDown = KeyManager.getKeyDown(node.keyID);

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

                    state.realState = keyDown;
                }

                // Remove dead keys
                {
                    Iterator<Entry<Integer, KeyState>> iter = rt.keyStates.entrySet().iterator();
                    while (iter.hasNext()) {
                        Entry<Integer, KeyState> ent = iter.next();
                        if (!ent.getValue().realState && !rt.delegates.containsKey(ent.getKey())) {
                            iter.remove();
                        }
                    }
                }

                // TODO optimize
                // Update cooldown
                Iterator<Entry<Integer, CooldownData>> iter = rt.cooldown.entrySet().iterator();
                while(iter.hasNext()) {
                    Entry<Integer, CooldownData> entry = iter.next();
                    CooldownData data = entry.getValue();
                    if (!rt.cooldownServer.contains(entry.getKey()))
                        iter.remove();
                    else if(--data.current < 0)
                        data.current = 0;
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

        @SubscribeEvent
        public void presetSwitch(PresetSwitchEvent evt) {
            updateDefaultGroup();
        }

        @SubscribeEvent
        public void presetEdit(PresetUpdateEvent evt) {
            if (SideHelper.isClient()) {
                System.out.println("presetEdit");
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

            rt.clearKeys(DEFAULT_GROUP);

            Preset preset = PresetData.get(rt.getEntity()).getCurrentPreset();

            for (int i = 0; i < PresetData.MAX_PRESETS; ++i) {
                if (preset.hasMapping(i)) {
                    Controllable c = preset.getControllable(i);
                    c.activate(rt, ClientHandler.getKeyMapping(i));
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
