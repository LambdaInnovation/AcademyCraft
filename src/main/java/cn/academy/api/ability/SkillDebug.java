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
		public void onFinish() {
			if (player.worldObj.isRemote) {
				player.addChatMessage(new ChatComponentText("Skill debug: onKeyUp (client side)."));
			} else {
				player.addChatMessage(new ChatComponentText("Skill debug: onKeyUp (server side)."));
			}
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
