/**
 * 
 */
package cn.academy.energy.msg.matrix;

import net.minecraft.world.World;
import io.netty.buffer.ByteBuf;
import cpw.mods.fml.common.network.simpleimpl.IMessage;

/**
 * @author WeathFolD
 *
 */
public class MsgChangePwd implements IMessage {

	public MsgChangePwd(World world, String _channel, String oldPwd, String newPwd) {
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see cpw.mods.fml.common.network.simpleimpl.IMessage#toBytes(io.netty.buffer.ByteBuf)
	 */
	@Override
	public void toBytes(ByteBuf buf) {
		// TODO Auto-generated method stub

	}

}
