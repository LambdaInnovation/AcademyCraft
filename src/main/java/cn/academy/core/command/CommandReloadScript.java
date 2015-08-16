/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.core.command;

import net.minecraft.command.ICommandSender;
import cn.academy.core.AcademyCraft;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegCommand;

/**
 * Used in debugging. Reload all the ripple scripts at once.
 * @author WeAthFolD
 */
@Registrant
@RegCommand
public class CommandReloadScript extends ACCommand {

	public CommandReloadScript() {
		super("ac_reloadscript");
	}

	@Override
	public void processCommand(ICommandSender ics, String[] pars) {
		AcademyCraft.reloadScript();
	}

}
