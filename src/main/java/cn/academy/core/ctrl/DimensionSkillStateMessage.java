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
package cn.academy.core.ctrl;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@RegistrationClass
public class DimensionSkillStateMessage implements IMessage {
    
    public int dimension;
    public List<SkillStateMessage> states = new ArrayList();
    
    public DimensionSkillStateMessage() {}
    
    //Construct with 

    @Override
    public void fromBytes(ByteBuf buf) {
        dimension = buf.readInt();
        
        int count = buf.readInt();
        for (int i = 0; i < count; ++i) {
            SkillStateMessage msg = new SkillStateMessage();
            msg.fromBytes(buf);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(dimension);
        buf.writeInt(states.size());
        
        for (SkillStateMessage msg : states) {
            msg.toBytes(buf);
        }
    }

    @RegMessageHandler(msg = DimensionSkillStateMessage.class, side = RegMessageHandler.Side.CLIENT)
    public static class Handler implements IMessageHandler<DimensionSkillStateMessage, IMessage> {
        @SideOnly(Side.CLIENT)
        SkillStateMessage.Handler stateHandler;
        
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(DimensionSkillStateMessage msg, MessageContext ctx) {
            for (SkillStateMessage sm : msg.states) {
                stateHandler.onMessage(sm, ctx);
            }
            return null;
        }
    }
}
