package cn.academy.ability.context;

import cn.academy.ability.Controllable;
import cn.academy.ability.context.Context.Status;
import cn.academy.datapart.CooldownData;
import cn.academy.ability.ctrl.ClientHandler;
import cn.academy.datapart.CPData;
import cn.academy.datapart.PresetData;
import cn.academy.datapart.PresetData.Preset;
import cn.academy.util.ACKeyManager;
import cn.academy.AcademyCraft;
import cn.academy.client.auxgui.TerminalUI;
import cn.academy.event.ability.*;
import cn.lambdalib2.auxgui.AuxGuiHandler;
import cn.lambdalib2.registry.mc.RegEventHandler;
import cn.lambdalib2.util.ClientUtils;
import cn.lambdalib2.util.ControlOverrider;
import cn.lambdalib2.util.SideUtils;
import cn.lambdalib2.datapart.DataPart;
import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.datapart.RegDataPart;
import cn.lambdalib2.input.KeyManager;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Handles objects in client that is player-local and dynamic - Ability keys, cooldown and stuff.
 * @author EAirPeter, WeAthFolD
 */
@SideOnly(Side.CLIENT)
@RegDataPart(value=EntityPlayer.class, side=Side.CLIENT)
public class ClientRuntime extends DataPart<EntityPlayer> {

    public static final String DEFAULT_GROUP = "def";
    private static final String OVERRIDE_GROUP = "AC_ClientRuntime";

    public static ClientRuntime instance() {
        EntityPlayer player = Minecraft.getMinecraft().player;
        Preconditions.checkNotNull(player);
        return EntityData.get(player).getPart(ClientRuntime.class);
    }

    public static boolean available() {
        return Minecraft.getMinecraft().player != null;
    }

    private final Map<Integer, DelegateNode> delegates = new TreeMap<>(); // Preserve insersion order for rendering
    private final Multimap<String, DelegateNode> delegateGroups = ArrayListMultimap.create();
    private final Map<Integer, KeyState> keyStates = new HashMap<>();

    private final LinkedList<IActivateHandler> activateHandlers = new LinkedList<>();

    private boolean ctrlDirty = false;
    private boolean requireFlush = false;

    {
        setTick(true);
    }

    @Override
    public void tick() {
        final CPData cpData = CPData.get(getEntity());
        final CooldownData cdData = CooldownData.of(getEntity());

        for (DelegateNode node : delegates.values()) {
            final KeyState state = getKeyState(node.keyID);
            final boolean keyDown = KeyManager.getKeyDown(node.keyID);

            boolean shouldAbort =
                !ClientUtils.isPlayerInGame() ||
                    cdData.isInCooldown(node.delegate.getSkill(), node.delegate.getIdentifier()) ||
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
            Iterator<Entry<Integer, KeyState>> iter = keyStates.entrySet().iterator();
            while (iter.hasNext()) {
                Entry<Integer, KeyState> ent = iter.next();
                if (!ent.getValue().realState && !delegates.containsKey(ent.getKey())) {
                    iter.remove();
                }
            }
        }

        // Update override
        if (ctrlDirty) {
            rebuildOverrides();
        }

        if (requireFlush) {
            requireFlush = false;
            updateDefaultGroup();
        }
    }

    @Override
    public void wake() {
        ctrlDirty = true;
        requireFlush = true;
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

    @Override
    public void onPlayerDead() {
        clearAllKeys();
        keyStates.clear();
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
        EntityPlayer player = Minecraft.getMinecraft().player;
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

    private void updateDefaultGroup() {
        clearKeys(DEFAULT_GROUP);

        Preset preset = PresetData.get(getEntity()).getCurrentPreset();

        for (int i = 0; i < PresetData.MAX_PRESETS; ++i) {
            if (preset.hasMapping(i)) {
                Controllable c = preset.getControllable(i);
                c.activate(this, ClientHandler.getKeyMapping(i));
            }
        }
    }

    private void rebuildOverrides() {
        AcademyCraft.debug("RebuildOverrides");
        CPData cpData = CPData.get(getEntity());

        ctrlDirty = false;

        int[] set = cpData.isActivated() ? delegates.values().stream().mapToInt(n -> n.keyID).toArray() : new int[0];
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

    @SideOnly(Side.CLIENT)
    public enum Events {
        @RegEventHandler()
        instance;

        @SubscribeEvent
        public void presetSwitch(PresetSwitchEvent evt) {
            ClientRuntime.instance().updateDefaultGroup();
        }

        @SubscribeEvent
        public void presetEdit(PresetUpdateEvent evt) {
            if (SideUtils.isClient()) {
                ClientRuntime.instance().updateDefaultGroup();
            }
        }

        @SubscribeEvent
        public void activateAbility(AbilityActivateEvent evt) {
            if (SideUtils.isClient()) {
                ClientRuntime.instance().updateDefaultGroup();
            }
        }

        @SubscribeEvent
        public void deactivateAbility(AbilityDeactivateEvent evt) {
            if (SideUtils.isClient()) {
                ClientRuntime.instance().clearAllKeys();
            }
        }

        @SubscribeEvent
        public void flushControl(FlushControlEvent evt) {
            if (ClientRuntime.available())
                ClientRuntime.instance().requireFlush = true;
        }


    }

    @SideOnly(Side.CLIENT)
    public static class ActivateHandlers {
        public static IActivateHandler terminatesContext(Context ctx) {
            return new IActivateHandler() {
                @Override
                public boolean handles(EntityPlayer player) {
                    return ctx.getStatus() == Status.ALIVE;
                }

                @Override
                public void onKeyDown(EntityPlayer player) {
                    ctx.terminate();
                }

                @Override
                public String getHint() {
                    return ENDSPECIAL;
                }
            };
        }
    }

    @SideOnly(Side.CLIENT)
    public interface IActivateHandler {

        String ENDSPECIAL = "endspecial";

        boolean handles(EntityPlayer player);
        void onKeyDown(EntityPlayer player);
        String getHint();

        default Optional<String> getHintTranslated() {
            String kname = KeyManager.getKeyName(ACKeyManager.instance.getKeyID(ClientHandler.keyActivate));
            String hint = ClientRuntime.instance().getActivateHandler().getHint();
            return hint == null ? Optional.empty() : Optional.of("[" + kname + "]: " + I18n.format(
                    "ac.activate_key." + hint + ".desc"));
        }

    }
}