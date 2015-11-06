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
package cn.academy.knowledge.command;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import cn.academy.core.command.ACCommand;
import cn.academy.knowledge.Knowledge;
import cn.academy.knowledge.KnowledgeData;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegCommand;
import cn.lambdalib.template.command.LICommandBase;

/**
 * @author WeAthFolD
 *
 */
@Registrant
@RegCommand
public class CommandKnowledge extends ACCommand {

	String cmds[] = { "stat", "list", "learn", "getall", "unlearn", "clear", "discover" };
	
	public CommandKnowledge() {
		super("knowledge");
	}

	@Override
	public void processCommand(ICommandSender ics, String[] pars) {
		// /knowledge help
		// /knowledge status
		// /knowledge clear
		// /knowledge learn id
		// /knowledge unlearn id
		
		if(pars.length == 0 || pars[0].equals("help")) {
			
			//Display the help message.
			for(String c : cmds) {
				LICommandBase.sendChat(ics, getLoc(c));
			}
			
		} else {
			KnowledgeData data = KnowledgeData.get(CommandBase.getCommandSenderAsPlayer(ics));
			
			switch(pars[0]) {
			
			case "stat":
			{
				StringBuilder sb = new StringBuilder();
				
				StringBuilder sb2 = new StringBuilder();
				
				boolean first = true;
				for(int i = 0; i < KnowledgeData.getKnowledgeCount(); ++i) {
					if(data.isLearned(i)) {
						sb.append(first ? "" : ",").append(KnowledgeData.getKnowledge(i));
						first = false;
					}
					if(data.isDiscovered(i)) {
						sb2.append(first ? "" : ",").append(KnowledgeData.getKnowledge(i));
					}
				}
				LICommandBase.sendChat(ics, getLoc("stat2"), sb.toString());
				LICommandBase.sendChat(ics, getLoc("stat3"), sb2.toString());
				break;
			}
			case "list":
			{
				LICommandBase.sendChat(ics, getLoc("all"));
				StringBuilder sb = new StringBuilder();
				List<Knowledge> list = KnowledgeData.getKnowledgeList();
				
				for(int i = 0; i < list.size(); ++i) {
					Knowledge k = list.get(i);
					sb.append(k);
					if(i != list.size() - 1)
						sb.append(",");
				}
				LICommandBase.sendChat(ics, sb.toString());
				break;
			}
			case "learn":
			{
				if(KnowledgeData.hasKnowledge(pars[1])) {
					data.learn(pars[1]);
					LICommandBase.sendChat(ics, locSuccessful());
				} else {
					LICommandBase.sendChat(ics, getLoc("notfound"), pars[1]);
				}
				break;
			}
			case "getall":
			{
				data.learnAll();
				LICommandBase.sendChat(ics, locSuccessful());
				break;
			}
			case "unlearn":
			{
				if(KnowledgeData.hasKnowledge(pars[1])) {
					data.unlearn(pars[1]);
					LICommandBase.sendChat(ics, locSuccessful());
				} else {
					LICommandBase.sendChat(ics, getLoc("notfound"), pars[1]);
				}
				break;
			}
			case "clear":
			{
				data.unlearnAll();
				LICommandBase.sendChat(ics, locSuccessful());
				break;
			}
			case "discover":
			{
				if(KnowledgeData.hasKnowledge(pars[1])) {
					data.discover(pars[1]);
					LICommandBase.sendChat(ics, locSuccessful());
				} else {
					LICommandBase.sendChat(ics, getLoc("notfound"), pars[1]);
				}
				break;
			}
			default:
			{
				LICommandBase.sendChat(ics, locInvalid());
			}
			}
		}
	}

}
