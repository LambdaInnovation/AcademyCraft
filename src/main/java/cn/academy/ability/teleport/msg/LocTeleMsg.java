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
package cn.academy.ability.teleport.msg;

import io.netty.buffer.ByteBuf;
import cn.academy.ability.teleport.CatTeleport;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.misc.msg.TeleportMsg;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * Teleport+CP consume.
 * @author WeathFolD
 */
@RegistrationClass
public class LocTeleMsg extends TeleportMsg {

	public float cp;

	public LocTeleMsg(int _dim, float _x, float _y, float _z, float _cp) {
		super(_dim, _x, _y, _z);
		cp = _cp;
	}

	public LocTeleMsg() {}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		super.fromBytes(buf);
		cp = buf.readFloat();
	}
	
	@Override
	public void toBytes(ByteBuf buf) {
		super.toBytes(buf);
		buf.writeFloat(cp);
	}
	
	@RegMessageHandler(msg = LocTeleMsg.class, side = RegMessageHandler.Side.SERVER)
	public static class Handler extends TeleportMsg.Handler<LocTeleMsg> {
		@Override
		public IMessage onMessage(LocTeleMsg msg, MessageContext ctx) {
			super.onMessage(msg, ctx);
			AbilityData data = AbilityDataMain.getData(ctx.getServerHandler().playerEntity);
			data.decreaseCP(msg.cp, CatTeleport.skillLocatingTele, true);
			return null;
		}
	}

}
