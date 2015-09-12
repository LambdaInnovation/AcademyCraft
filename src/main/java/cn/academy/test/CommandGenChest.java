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
package cn.academy.test;

import java.io.StringReader;
import java.util.Random;

import cn.academy.core.command.ACCommand;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegCommand;
import cn.liutils.ripple.ScriptProgram;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentTranslation;

/**
 * DEBUG COMMAND
 * @author WeAthFolD
 */
@Registrant
@RegCommand
public class CommandGenChest extends ACCommand {
	Random rnd = new Random();

	public CommandGenChest() {
		super("g");
	}

	@Override
	public void processCommand(ICommandSender ics, String[] str) {
		String a = "val(x) { 5 } ns { gadd(x) { x + val(x) } }";
		ScriptProgram program = new ScriptProgram();
		program.loadScript(new StringReader(a));
		ics.addChatMessage(new ChatComponentTranslation(
			"" + program.root.getFunction("ns.gadd").callInteger(5)));
	}

}
