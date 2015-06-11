package cn.academy.terminal;

import cn.academy.terminal.item.ItemApp;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegItem;
import cn.annoreg.mc.RegSubmoduleInit;

@Registrant
@RegSubmoduleInit
public class ModuleTerminal {

	@RegItem
	public static ItemApp itemApp;
	
	public static void init() {
		
	}
	
}
