package cn.academy.ability.api.ctrl;

import java.util.ArrayList;
import java.util.List;

import cn.academy.ability.api.Controllable;
import cn.academy.ability.api.context.ClientRuntime;
import cn.academy.ability.api.data.PresetData.Preset;
import cn.academy.core.AcademyCraft;
import cn.lambdalib.annoreg.mc.RegEventHandler.Bus;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.MinecraftForge;

import net.minecraftforge.common.config.Configuration;
import org.lwjgl.input.Keyboard;

import cn.academy.ability.api.ctrl.ClientController.AbilityKey;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.ability.api.data.PresetData;
import cn.academy.ability.api.event.PresetSwitchEvent;
import cn.academy.ability.client.ui.PresetEditUI;
import cn.academy.core.ModuleCoreClient;
import cn.academy.core.registry.RegACKeyHandler;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegEventHandler;
import cn.lambdalib.util.key.KeyHandler;
import cn.lambdalib.util.key.KeyManager;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Misc key event listener for skill events.
 */
@SideOnly(Side.CLIENT)
@Registrant
@RegEventHandler(Bus.Forge)
public final class ClientHandler {

    // Name constants for looking up keys in ACKeyHandler.
    public static final String
        KEY_SWITCH_PRESET = "switch_preset",
        KEY_EDIT_PRESET = "edit_preset",
        KEY_ACTIVATE_ABILITY = "ability_activation";

    private static final int[] defaultKeysInit = new int[] {
            KeyManager.MOUSE_LEFT,
            KeyManager.MOUSE_RIGHT,
            Keyboard.KEY_R,
            Keyboard.KEY_F
    };

    private static final int[] defaultKeys = new int[defaultKeysInit.length];

    @RegInitCallback
    public static void init() {
        updateAbilityKeys();
    }

    private static void updateAbilityKeys() {
        Configuration cfg = AcademyCraft.config;
        for (int i = 0; i < getKeyCount(); ++i) {
            defaultKeys[i] = cfg.getInt("keys", "ability_" + i,
                    defaultKeysInit[i], -1000, 1000, "Ability control key #" + i);
        }
    }

    public static int getKeyMapping(int id) {
        return defaultKeys[id];
    }

    public static int getKeyCount() {
        return defaultKeysInit.length;
    }

    private interface IActivateHandler {
        
        boolean handles(EntityPlayer player);
        void onKeyDown(EntityPlayer player);
        String getHint();
        
    }
    
    private static List<IActivateHandler> activateHandlers = new ArrayList<>();
    static {
        activateHandlers.add(new IActivateHandler() {
            @Override
            public boolean handles(EntityPlayer player) {
                AbilityKey mutexHandler = ClientController.getMutexHandler();
                return mutexHandler != null;
            }

            @Override
            public void onKeyDown(EntityPlayer player) {
                ClientController.getMutexHandler().onKeyAbort();
            }

            @Override
            public String getHint() {
                return "endskill";
            }
        });
        
        activateHandlers.add(new IActivateHandler() {
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
        
        activateHandlers.add(new IActivateHandler() {
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
    }
    
    public static String getActivateKeyHint() {
        String kname = KeyManager.getKeyName(ModuleCoreClient.keyManager.getKeyID(keyActivate));
        String hint = getActivateHandler().getHint();
        return hint == null ? null : ("[" + kname + "]: " + StatCollector.translateToLocal(
            "ac.activate_key." + hint + ".desc"));
    }
    
    private static IActivateHandler getActivateHandler() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        for(IActivateHandler h : activateHandlers) {
            if(h.handles(player))
                return h;
        }
        throw new RuntimeException();
    }
    
    /**
     * The key to activate and deactivate the ability, might have other uses in certain circumstances,
     *  e.g. quit charging when using ability.
     */
    @RegACKeyHandler(name = KEY_ACTIVATE_ABILITY, defaultKey = Keyboard.KEY_V)
    public static KeyHandler keyActivate = new KeyHandler() {

        @Override
        public void onKeyDown() {
            EntityPlayer player = getPlayer();
            AbilityData aData = AbilityData.get(player);
            
            if(aData.isLearned()) {
                getActivateHandler().onKeyDown(player);
            }
        }
        
    };
    
    @RegACKeyHandler(name = KEY_EDIT_PRESET, defaultKey = Keyboard.KEY_N)
    public static KeyHandler keyEditPreset = new KeyHandler() {
        @Override
        public void onKeyDown() {
            PresetData data = PresetData.get(Minecraft.getMinecraft().thePlayer);
            if(data.isActive()) {
                Minecraft.getMinecraft().displayGuiScreen(new PresetEditUI());
            }
        }
    };
    
    @RegACKeyHandler(name = KEY_SWITCH_PRESET, defaultKey = Keyboard.KEY_C)
    public static KeyHandler keySwitchPreset = new KeyHandler() {
        @Override
        public void onKeyDown() {
            PresetData data = PresetData.get(getPlayer());
            CPData cpData = CPData.get(getPlayer());
            
            if(cpData.isActivated() && !data.isOverriding() &&  data.isActive()) {
                int next = (data.getCurrentID() + 1) % 4;
                data.switchCurrent(next);
                MinecraftForge.EVENT_BUS.post(new PresetSwitchEvent(data.getEntity()));
            }
        }
    };

    @SubscribeEvent
    public void onPresetSwitch(PresetSwitchEvent evt) {
        if (!evt.player.worldObj.isRemote) return;

        final ClientRuntime rt = ClientRuntime.instance();
        final Preset preset = PresetData.get(evt.player).getCurrentPreset();

        rt.clearKeys(ClientRuntime.DEFAULT_GROUP);

        for (int i = 0; i < PresetData.MAX_PRESETS; ++i) {
            if (preset.hasMapping(i)) {
                Controllable c = preset.getControllable(i);
                c.activate(rt, getKeyMapping(i));
            }
        }
    }

}
