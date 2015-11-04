package cn.academy.terminal;

import cn.academy.core.registry.ACRecipeNamesRegistration.RegACRecipeNames;
import cn.academy.terminal.item.ItemApp;
import cn.academy.terminal.item.ItemTerminalInstaller;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInit;
import cn.lambdalib.annoreg.mc.RegItem;
import cn.liutils.crafting.CustomMappingHelper.RecipeName;

@Registrant
@RegInit
@RegACRecipeNames
public class ModuleTerminal {
	
	@RegItem
	@RegItem.HasRender
	@RecipeName("terminal")
	public static ItemTerminalInstaller terminalInstaller;
	
	public static void init() {
		ItemApp.registerItems();
	}
	
}
