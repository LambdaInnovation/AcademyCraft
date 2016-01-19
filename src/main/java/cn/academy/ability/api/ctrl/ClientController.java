/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.ability.api.ctrl;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;

import cn.academy.ability.api.Controllable;
import cn.academy.ability.api.ctrl.SkillInstance.State;
import cn.academy.ability.api.data.CPData;
import cn.academy.ability.api.data.PresetData;
import cn.academy.ability.api.data.PresetData.Preset;
import cn.academy.ability.api.event.AbilityActivateEvent;
import cn.academy.ability.api.event.AbilityDeactivateEvent;
import cn.academy.ability.api.event.PresetSwitchEvent;
import cn.academy.ability.api.event.PresetUpdateEvent;
import cn.academy.core.ModuleCoreClient;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegEventHandler;
import cn.lambdalib.annoreg.mc.RegInit;
import cn.lambdalib.util.key.KeyHandler;
import cn.lambdalib.util.key.KeyManager;
import cn.lambdalib.util.mc.ControlOverrider;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

/**
 * This class handles the ability key and their controlling, 
 * and the overriding of vanilla MC control.
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
// @Registrant
// @RegInit
// @RegEventHandler
public class ClientController {
    
    public static final int MAX_KEYS = PresetData.MAX_KEYS, STATIC_KEYS = 4;
    
    /**
     * Remaps a Special Key.
     */
    public static void remap(int id, int keyID) {
        if(id >= 4 || id < 0)
            throw new IllegalStateException("id overflow");
        ModuleCoreClient.dynKeyManager.resetBindingKey("ability_" + (id + 4), keyID);
    }
    
    public static void init() { }

    private static boolean hasMutexInstance() {
        return false;
    }

    public static SkillInstance getMutexInstance() {
        return null;
    }

    static AbilityKey getMutexHandler() {
        return null;
    }
    
    /**
     * Stores KEYID in case the key mapping is editted.
     */
    private Integer[] lastOverrides;
    private boolean overrideInit;
    
    @SubscribeEvent
    public void changePreset(PresetSwitchEvent event) {
        checkOverrides(event.player);
    }
    
    @SubscribeEvent
    public void editPreset(PresetUpdateEvent event) {
        checkOverrides(event.player);
    }
    
    @SubscribeEvent
    public void activate(AbilityActivateEvent event) {
        checkOverrides(event.player);
    }
    
    @SubscribeEvent
    public void deactivate(AbilityDeactivateEvent event) {
        checkOverrides(event.player);
    }
    
    @SubscribeEvent
    public void changeCategory(AbilityActivateEvent event) {
        checkOverrides(event.player);
    }
    
    private void checkOverrides(EntityPlayer player) {
        if(player.worldObj.isRemote)
            rebuildOverrides();
    }
    
    private void rebuildOverrides() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        //if(player == null)
        //    return;
        
        PresetData pdata = PresetData.get(player);
        CPData cpData = CPData.get(player);
        
        if(lastOverrides != null) {
            for(int i : lastOverrides)
                ControlOverrider.removeOverride(i);
        }
        //System.out.println("{");
        if(cpData.isActivated()) {
            Preset preset = pdata.getCurrentPreset();
            if(preset != null) {
                List<Integer> list = new ArrayList<>();
                //System.out.println("    NotNull");
                for(int i = 0; i < MAX_KEYS; ++i) {
                    if(preset.hasMapping(i)) {
                        Controllable c = preset.getControllable(i);
                        if(c.shouldOverrideKey()) {
                            int mapping = -1;
                            
                            list.add(mapping);
                            ControlOverrider.override(mapping);
                        }
                    }
                }
                
                lastOverrides = list.toArray(new Integer[] {});
            }
        }
        //System.out.println("}");
    }

    static class AbilityKey extends KeyHandler {

        public AbilityKey(int id) {
        }

    }
}
