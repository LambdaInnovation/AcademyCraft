package cn.academy.core.proxy;

import cn.academy.core.AcademyCraftMod;
import cn.academy.core.client.gui.GuiPresetSettings;
import cn.academy.core.client.gui.dev.GuiDeveloper;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.core.RegistrationManager;
import cn.liutils.core.LIUtils;
import cn.liutils.registry.ConfigurableRegistry.RegConfigurable;

@RegistrationClass
@RegConfigurable
public class ProxyClient extends ProxyCommon {

	@Override
	public void preInit() {
		AcademyCraftMod.guiHandler.addGuiElement(
			ACCommonProps.GUI_ID_PRESET_SETTINGS, GuiPresetSettings.element);
		RegistrationManager.INSTANCE.registerAll(AcademyCraftMod.INSTANCE, LIUtils.REGISTER_TYPE_AUXGUI);
		RegistrationManager.INSTANCE.registerAll(AcademyCraftMod.INSTANCE, LIUtils.REGISTER_TYPE_KEYHANDLER);
		AcademyCraftMod.guiHandler.addGuiElement(ACCommonProps.GUI_ID_ABILITY_DEV, new GuiDeveloper.Element());
	}
	
	@Override
	public void init() {
		super.init();
		AcademyCraftMod.INSTANCE.log.info("Loading client proxy of Academy Craft.");
		
	}
	
	@Override
	public void postInit() {
		
	}
	
}
