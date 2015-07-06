package cn.academy.terminal;

import cn.academy.terminal.item.ItemApp;
import cn.academy.terminal.item.ItemTerminalInstaller;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegInit;
import cn.annoreg.mc.RegItem;

@Registrant
@RegInit
public class ModuleTerminal {

	@RegItem
	public static ItemApp itemApp;
	
	@RegItem
	@RegItem.HasRender
	public static ItemTerminalInstaller terminalInstaller;
	
	public static void init() {
		
	}
	
}
