package cn.academy.terminal;

import cn.academy.core.registry.ACRecipeNamesRegistration.RegACRecipeNames;
import cn.academy.terminal.item.ItemTerminalInstaller;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegItem;
import cn.lambdalib.crafting.CustomMappingHelper.RecipeName;

@Registrant
@RegACRecipeNames
public class ModuleTerminal {

    @RegItem
    @RegItem.HasRender
    @RecipeName("terminal")
    public static ItemTerminalInstaller terminalInstaller;

}
