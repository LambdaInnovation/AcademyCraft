/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.api.ctrl;

import cn.academy.ability.api.context.ClientRuntime;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.ability.api.data.PresetData;
import cn.academy.ability.api.event.FlushControlEvent;
import cn.academy.ability.api.event.PresetSwitchEvent;
import cn.academy.ability.client.ui.PresetEditUI;
import cn.academy.core.AcademyCraft;
import cn.academy.core.event.ConfigModifyEvent;
import cn.academy.core.registry.RegACKeyHandler;
import cn.academy.terminal.app.settings.PropertyElements;
import cn.academy.terminal.app.settings.SettingsUI;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegEventHandler;
import cn.lambdalib.annoreg.mc.RegEventHandler.Bus;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cn.lambdalib.util.key.KeyHandler;
import cn.lambdalib.util.key.KeyManager;
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

        @Override
        public void onKeyDown() {
            EntityPlayer player = getPlayer();
            AbilityData aData = AbilityData.get(player);
            
            if(aData.isLearned()) {
                ClientRuntime.instance().getActivateHandler().onKeyDown(player);
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
            
            if(cpData.isActivated() && data.isActive()) {
                int next = (data.getCurrentID() + 1) % 4;
                data.switchCurrent(next);
                MinecraftForge.EVENT_BUS.post(new PresetSwitchEvent(data.getEntity()));
            }
        }
    };

    @SubscribeEvent
    public void onConfigModify(ConfigModifyEvent evt) {
        updateAbilityKeys();
    }

}
