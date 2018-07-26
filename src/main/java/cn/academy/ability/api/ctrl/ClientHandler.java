package cn.academy.ability.api.ctrl;

import cn.academy.ability.api.context.ClientRuntime;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.ability.api.data.PresetData;
import cn.academy.ability.api.event.FlushControlEvent;
import cn.academy.ability.api.event.PresetSwitchEvent;
import cn.academy.ability.client.ui.CPBar;
import cn.academy.ability.client.ui.PresetEditUI;
import cn.academy.core.AcademyCraft;
import cn.academy.core.event.ConfigModifyEvent;
import cn.academy.core.registry.RegACKeyHandler;
import cn.academy.terminal.app.settings.PropertyElements;
import cn.academy.terminal.app.settings.SettingsUI;
import cn.lambdalib2.util.helper.GameTimer;
import cn.lambdalib2.util.key.KeyHandler;
import cn.lambdalib2.util.key.KeyManager;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import org.lwjgl.input.Keyboard;

/**
 * Misc key event listener for skill events.
 */
@SideOnly(Side.CLIENT)
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
    private static void init() {
        updateAbilityKeys();
        for (int i = 0; i < defaultKeysInit.length; ++i) {
            SettingsUI.addProperty(PropertyElements.KEY, "keys", "ability_" + i, defaultKeysInit[i], false);
        }
    }

    private static void updateAbilityKeys() {
        Configuration cfg = AcademyCraft.config;
        for (int i = 0; i < getKeyCount(); ++i) {
            defaultKeys[i] = cfg.getInt("ability_" + i, "keys",
                    defaultKeysInit[i], -1000, 1000, "Ability control key #" + i);
        }

        MinecraftForge.EVENT_BUS.post(new FlushControlEvent());
    }

    public static int getKeyMapping(int id) {
        return defaultKeys[id];
    }

    public static int getKeyCount() {
        return defaultKeysInit.length;
    }
    
    /**
     * The key to activate and deactivate the ability, might have other uses in certain circumstances,
     *  e.g. quit charging when using ability.
     */
    @RegACKeyHandler(name = KEY_ACTIVATE_ABILITY, defaultKey = Keyboard.KEY_V)
    public static KeyHandler keyActivate = new KeyHandler() {

        long lastKeyDown;

        @Override
        public void onKeyUp() {
            long delta = GameTimer.getTime() - lastKeyDown;
            if (delta < 300L) {
                EntityPlayer player = getPlayer();
                AbilityData aData = AbilityData.get(player);

                if(aData.hasCategory()) {
                    ClientRuntime.instance().getActivateHandler().onKeyDown(player);
                }
            }

            CPBar.instance.stopDisplayNumbers();
        }

        @Override
        public void onKeyDown() {
            lastKeyDown = GameTimer.getTime();
            CPBar.instance.startDisplayNumbers();
        }
        
    };
    
    @RegACKeyHandler(name = KEY_EDIT_PRESET, defaultKey = Keyboard.KEY_N)
    public static KeyHandler keyEditPreset = new KeyHandler() {
        @Override
        public void onKeyDown() {
            if(AbilityData.get(getPlayer()).hasCategory()) {
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
            
            if(cpData.isActivated()) {
                int next = (data.getCurrentID() + 1) % PresetData.MAX_PRESETS;
                data.switchFromClient(next);
                MinecraftForge.EVENT_BUS.post(new PresetSwitchEvent(data.getEntity()));
            }
        }
    };


    @Registrant
    @SideOnly(Side.CLIENT)
    public enum ConfigHandler {
        @RegEventHandler(Bus.Forge)
        instance;

        @SubscribeEvent
        public void onConfigModify(ConfigModifyEvent evt) {
            updateAbilityKeys();
        }
    }

}