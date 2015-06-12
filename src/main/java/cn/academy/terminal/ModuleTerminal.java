package cn.academy.terminal;

import cn.academy.terminal.item.ItemApp;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegItem;
import cn.annoreg.mc.RegInit;

@Registrant
@RegInit
public class ModuleTerminal {

	@RegItem
	public static ItemApp itemApp;
	
	public static void init() {
		
	}
	
}
