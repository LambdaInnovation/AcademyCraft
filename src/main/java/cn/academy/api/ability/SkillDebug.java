/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.api.ability;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.SkillState;
import cn.academy.api.ctrl.pattern.PatternDown;

public class SkillDebug extends SkillBase {
	
	private static class State extends SkillState {

		public State(EntityPlayer player) {
			super(player);
		}

		@Override
		public void onStart() {
			if (player.worldObj.isRemote) {
				player.addChatMessage(new ChatComponentText("Skill debug: onKeyDown (client side)."));
			} else {
				player.addChatMessage(new ChatComponentText("Skill debug: onKeyDown (server side)."));
			}
		}

		@Override
		public boolean onFinish(boolean result) {
			if (player.worldObj.isRemote) {
				player.addChatMessage(new ChatComponentText("Skill debug: onKeyUp (client side)."));
			} else {
				player.addChatMessage(new ChatComponentText("Skill debug: onKeyUp (server side)."));
			}
			return true;
		}
	}

	@Override
	public void initPattern(RawEventHandler reh) {
		
		reh.addPattern(new PatternDown() {

			@Override
			public SkillState createSkill(EntityPlayer player) {
				return new State(player);
			}

		});
	}
	
}
