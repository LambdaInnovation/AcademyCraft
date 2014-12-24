package cn.academy.core.proxy;

import net.minecraft.client.Minecraft;

import org.lwjgl.input.Keyboard;

import cn.academy.ability.electro.client.render.RenderElecArc;
import cn.academy.ability.electro.entity.EntityElecArcFX;
import cn.academy.api.ctrl.EventHandlerClient;
import cn.academy.api.ctrl.PresetManager;
import cn.academy.core.AcademyCraftMod;
import cn.academy.core.block.TileDeveloper;
import cn.academy.core.client.gui.GuiPresetSelect;
import cn.academy.core.client.gui.GuiPresetSettings;
import cn.academy.core.client.render.RenderDeveloper;
import cn.academy.core.client.render.SkillRenderingHandler;
import cn.liutils.api.LIGeneralRegistry;
import cn.liutils.api.client.key.IKeyHandler;
import cn.liutils.api.register.Configurable;
import cn.liutils.api.util.GenericUtils;
import cn.liutils.core.client.register.LIKeyProcess;
import cn.liutils.core.event.LIClientEvents;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;

public class ProxyClient extends ProxyCommon {
	
	@Configurable(category = "Control", key = "keyPresetSelect", defValueInt = Keyboard.KEY_C)
	public static int KEY_ID_PRESET_SELECT = Keyboard.KEY_C;
	
	@Configurable(category = "Control", key = "keyPresetSettings", defValueInt = Keyboard.KEY_N)
	public static int KEY_ID_PRESET_SETTINGS = Keyboard.KEY_N;

	@Override
	public void preInit() {
		AcademyCraftMod.guiHandler.addGuiElement(
			ACCommonProps.GUI_ID_PRESET_SETTINGS, GuiPresetSettings.element);
		LIClientEvents.registerAuxGui(GuiPresetSelect.instance);
	}
	
	@Override
	public void init() {
		super.init();

		AcademyCraftMod.INSTANCE.log.info("Loading client proxy of Academy Craft.");
		LIGeneralRegistry.loadConfigurableClass(AcademyCraftMod.config, ProxyClient.class);
		EventHandlerClient.init();
		PresetManager.init();
		SkillRenderingHandler.init();
		
		System.out.println("Preset key: " + KEY_ID_PRESET_SELECT);
		LIKeyProcess.instance.addKey("preset_select", KEY_ID_PRESET_SELECT, false, new IKeyHandler() {
			@Override
			public void onKeyDown(int keyCode, boolean tickEnd) {
				if(tickEnd) return;
				if(GuiPresetSelect.instance.isOpen())
					GuiPresetSelect.instance.closeGui();
				else if(GenericUtils.isPlayerInGame()) {
					GuiPresetSelect.instance.openGui();
				}
			}
			@Override public void onKeyUp(int keyCode, boolean tickEnd) {}
			@Override public void onKeyTick(int keyCode, boolean tickEnd) {}
		});
		
		LIKeyProcess.instance.addKey("preset_settings", KEY_ID_PRESET_SETTINGS, false, new IKeyHandler() {
			@Override
			public void onKeyDown(int keyCode, boolean tickEnd) {
				if(tickEnd || !GenericUtils.isPlayerInGame()) return;
				Minecraft mc = Minecraft.getMinecraft();
				mc.thePlayer.openGui(
						AcademyCraftMod.INSTANCE, 
						ACCommonProps.GUI_ID_PRESET_SETTINGS, 
						mc.theWorld, 0, 0, 0);
			}
			@Override public void onKeyUp(int keyCode, boolean tickEnd) {}
			@Override public void onKeyTick(int keyCode, boolean tickEnd) {}
		});
		
		//Rendering
		ClientRegistry.bindTileEntitySpecialRenderer(TileDeveloper.class, new RenderDeveloper());
		RenderingRegistry.registerEntityRenderingHandler(EntityElecArcFX.class, new RenderElecArc());
	}
	
}
