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
package cn.academy.core.ctrl;

import cn.academy.api.ctrl.SkillEventType;
import io.netty.buffer.ByteBuf;
import cpw.mods.fml.common.network.simpleimpl.IMessage;

/**
 * The network message used by EventHandlerClient and EventHandlerServer.
 * No handler in this class. Client and server use different handlers. 
 * @author acaly
 *
 */
public class ControlMessage implements IMessage {
	
	public int skillId;
	public SkillEventType eventType;
	public int time;
	
	public ControlMessage() {
	}
	
	public ControlMessage(int skillId, SkillEventType eventType, int time) {
		this.skillId = skillId;
		this.eventType = eventType;
		this.time = time;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		skillId = buf.readInt();
		eventType = SkillEventType.values()[buf.readInt()];
		time = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(skillId);
		buf.writeInt(eventType.ordinal());
		buf.writeInt(time);
	}

}
