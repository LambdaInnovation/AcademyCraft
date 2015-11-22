package cn.academy.ability.api.ctrl;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.MinecraftForge;

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
@RegEventHandler
public final class ClientHandler {
	
	// Name constants for looking up keys in ACKeyHandler.
	public static final String
		KEY_SWITCH_PRESET = "switch_preset",
		KEY_EDIT_PRESET = "edit_preset",
		KEY_ACTIVATE_ABILITY = "ability_activation";
	
	private interface IActivateHandler {
		
		boolean handles(EntityPlayer player);
		void onKeyDown(EntityPlayer player);
		String getHint();
		
	}
	
	private static List<IActivateHandler> activateHandlers = new ArrayList();
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
				// ACSounds.playClient(getPlayer(), "ability.preset_switch", 1.0f);
				MinecraftForge.EVENT_BUS.post(new PresetSwitchEvent(data.getEntity()));
			}
		}
	};
    
}
