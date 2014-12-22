package cn.academy.api.ability;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.PatternHold;

public class SkillHoldTest extends SkillBase {

	public static class State extends PatternHold.State {

		public State(EntityPlayer player) {
			super(player);
		}

		@Override
		public void onStart() {
			if (!player.worldObj.isRemote) {
				player.addChatComponentMessage(new ChatComponentText("Hold Skill: Start."));
			}
		}

		@Override
		public void onFinish() {
			if (!player.worldObj.isRemote) {
				player.addChatComponentMessage(new ChatComponentText("Hold Skill: Finish."));
			}
		}

		@Override
		public void onHold() {
			if (!player.worldObj.isRemote) {
				player.addChatComponentMessage(new ChatComponentText("Hold Skill: Hold."));
			}
		}
		
	}
	
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternHold(30) {

			@Override
			public State createSkill(EntityPlayer player) {
				return new SkillHoldTest.State(player);
			}
			
		});
	}
	
}
