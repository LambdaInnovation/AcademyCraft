package cn.academy.terminal;

import cn.academy.core.registry.ACRecipeNamesRegistration.RegACRecipeNames;
import cn.academy.item.ItemTerminalInstaller;
import cn.lambdalib2.crafting.CustomMappingHelper.RecipeName;

@RegACRecipeNames
public class ModuleTerminal {

    @RegItem
    @RegItem.HasRender
    @RecipeName("terminal")
    public static ItemTerminalInstaller terminalInstaller;

}