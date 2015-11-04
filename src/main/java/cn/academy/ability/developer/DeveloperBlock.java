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
package cn.academy.ability.developer;

import cn.academy.ability.block.TileDeveloper;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.networkcall.RegNetworkCall;
import cn.lambdalib.networkcall.s11n.InstanceSerializer;
import cn.lambdalib.networkcall.s11n.RegSerializable;
import cn.lambdalib.networkcall.s11n.SerializationManager;
import cn.lambdalib.networkcall.s11n.StorageOption;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.tileentity.TileEntity;

/**
 * @author WeAthFolD
 */
@Registrant
@RegSerializable(instance = DeveloperBlock.Serializer.class)
public class DeveloperBlock extends Developer {
	
	public final TileDeveloper tile;

	public DeveloperBlock(TileDeveloper _tile) {
		super(_tile.getType());
		tile = _tile;
	}

	@Override
	public EntityPlayer getUser() {
		return tile.getUser();
	}

	@Override
	public boolean pullEnergy(double amt) {
		return tile.pullEnergy(amt) == amt;
	}

	@Override
	public double getEnergy() {
		return tile.getEnergy();
	}
	
	@Override
	public double getMaxEnergy() {
		return tile.getMaxEnergy();
	}
	
	@Override
	public void onGuiClosed() {
		unuseAtServer();
	}
	
	@RegNetworkCall(side = Side.SERVER, thisStorage = StorageOption.Option.INSTANCE)
	private void unuseAtServer() {
		tile.unuse(getUser());
	}
	
	public static class Serializer implements InstanceSerializer<DeveloperBlock> {

		@Override
		public DeveloperBlock readInstance(NBTBase nbt) throws Exception {
			TileEntity te = tileSer().readInstance(nbt);
			return te instanceof TileDeveloper ? ((TileDeveloper) te).developer : null;
		}

		@Override
		public NBTBase writeInstance(DeveloperBlock obj) throws Exception {
			return tileSer().writeInstance(obj.tile);
		}
		
		private InstanceSerializer<TileEntity> tileSer() {
			return SerializationManager.INSTANCE.getInstanceSerializer(TileEntity.class);
		}
		
	}

}
