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

import java.util.BitSet;
import java.util.List;

import net.minecraft.command.ICommandSender;
import cn.academy.core.command.ACCommand;
import cn.academy.knowledge.Knowledge;
import cn.academy.knowledge.KnowledgeData;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegCommand;

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
				this.sendChat(ics, getLoc(c));
			}
			
		} else {
			KnowledgeData data = KnowledgeData.get(this.getCommandSenderAsPlayer(ics));
			
			switch(pars[0]) {
			
			case "stat":
			{
				StringBuilder sb = new StringBuilder();
				
				StringBuilder sb2 = new StringBuilder();
				
				boolean first = true;
				for(int i = 0; i < KnowledgeData.getKnowledgeCount(); ++i) {
					if(data.isLearned(i)) {
						sb.append(first ? "" : ",").append(data.getKnowledge(i));
						first = false;
					}
					if(data.isDiscovered(i)) {
						sb2.append(first ? "" : ",").append(data.getKnowledge(i));
					}
				}
				this.sendChat(ics, getLoc("stat2"), sb.toString());
				this.sendChat(ics, getLoc("stat3"), sb2.toString());
				break;
			}
			case "list":
			{
				this.sendChat(ics, getLoc("all"));
				StringBuilder sb = new StringBuilder();
				List<Knowledge> list = KnowledgeData.getKnowledgeList();
				
				for(int i = 0; i < list.size(); ++i) {
					Knowledge k = list.get(i);
					sb.append(k);
					if(i != list.size() - 1)
						sb.append(",");
				}
				this.sendChat(ics, sb.toString());
				break;
			}
			case "learn":
			{
				if(data.hasKnowledge(pars[1])) {
					data.learn(pars[1]);
					this.sendChat(ics, locSuccessful());
				} else {
					this.sendChat(ics, getLoc("notfound"), pars[1]);
				}
				break;
			}
			case "getall":
			{
				data.learnAll();
				this.sendChat(ics, locSuccessful());
				break;
			}
			case "unlearn":
			{
				if(data.hasKnowledge(pars[1])) {
					data.unlearn(pars[1]);
					this.sendChat(ics, locSuccessful());
				} else {
					this.sendChat(ics, getLoc("notfound"), pars[1]);
				}
				break;
			}
			case "clear":
			{
				data.unlearnAll();
				this.sendChat(ics, locSuccessful());
				break;
			}
			case "discover":
			{
				if(data.hasKnowledge(pars[1])) {
					data.discover(pars[1]);
					this.sendChat(ics, locSuccessful());
				} else {
					this.sendChat(ics, getLoc("notfound"), pars[1]);
				}
				break;
			}
			default:
			{
				this.sendChat(ics, locInvalid());
			}
			}
		}
	}

}
