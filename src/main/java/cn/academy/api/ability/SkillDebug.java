package cn.academy.api.ability;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.PatternDown;
import cn.academy.api.ctrl.pattern.PatternUp;

public class SkillDebug extends SkillBase {

	@Override
	public void initPattern(RawEventHandler reh) {
		
		reh.addPattern(new PatternDown() {
			@Override
			public boolean onKeyDown(EntityPlayer player) {
				if (player.worldObj.isRemote) {
					player.addChatMessage(new ChatComponentText("Skill debug: onKeyDown (client side)."));
				} else {
					player.addChatMessage(new ChatComponentText("Skill debug: onKeyDown (server side)."));
				}
				return false;
			}
		});
		
		reh.addPattern(new PatternUp() {
			@Override
			public boolean onKeyUp(EntityPlayer player) {
				if (player.worldObj.isRemote) {
					player.addChatMessage(new ChatComponentText("Skill debug: onKeyUp (client side)."));
				} else {
					player.addChatMessage(new ChatComponentText("Skill debug: onKeyUp (server side)."));
				}
				return false;
			}
		});
		
	}
	
}
