package cn.academy.core.proxy;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Keyboard;

import cn.academy.ability.electro.client.render.RenderRailgun;
import cn.academy.ability.electro.entity.EntityRailgun;
import cn.academy.ability.meltdowner.client.render.RenderElecDart;
import cn.academy.ability.meltdowner.entity.EntityElecDart;
import cn.academy.api.ctrl.EventHandlerClient;
import cn.academy.api.ctrl.PresetManager;
import cn.academy.core.AcademyCraftMod;
import cn.academy.core.block.dev.TileDeveloper;
import cn.academy.core.client.gui.GuiMainScreen;
import cn.academy.core.client.gui.GuiPresetSelect;
import cn.academy.core.client.gui.GuiPresetSettings;
import cn.academy.core.client.gui.dev.GuiDeveloper;
import cn.academy.core.client.render.RenderDeveloper;
import cn.academy.core.client.render.RenderVoid;
import cn.academy.core.client.render.SkillRenderingHandler;
import cn.academy.core.event.ClientEvents;
import cn.academy.core.register.ACItems;
import cn.academy.misc.client.render.RendererCoin;
import cn.academy.misc.entity.EntityThrowingCoin;
import cn.annoreg.core.RegistrationClass;
import cn.liutils.api.LIGeneralRegistry;
import cn.liutils.api.key.IKeyHandler;
import cn.liutils.api.key.LIKeyProcess;
import cn.liutils.api.register.Configurable;
import cn.liutils.core.event.LIClientEvents;
import cn.liutils.registry.ConfigurableRegistry.RegConfigurable;
import cn.liutils.util.ClientUtils;
import cn.liutils.util.GenericUtils;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;

@RegistrationClass
@RegConfigurable
public class ProxyClient extends ProxyCommon {

	@Override
	public void preInit() {
		AcademyCraftMod.guiHandler.addGuiElement(
			ACCommonProps.GUI_ID_PRESET_SETTINGS, GuiPresetSettings.element);
		AcademyCraftMod.guiHandler.addGuiElement(ACCommonProps.GUI_ID_ABILITY_DEV, new GuiDeveloper.Element());
	}
	
	@Override
	public void init() {
		super.init();
		AcademyCraftMod.INSTANCE.log.info("Loading client proxy of Academy Craft.");
		
	}
	
}
