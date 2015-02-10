package cn.academy.api.ctrl;

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
    
    int dimension;
    List<SkillStateMessage> states = new ArrayList();
    
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

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(DimensionSkillStateMessage msg, MessageContext ctx) {
            return null;
        }
    }
}
