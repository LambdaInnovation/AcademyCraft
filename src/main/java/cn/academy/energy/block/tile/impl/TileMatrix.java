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
package cn.academy.energy.block.tile.impl;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import cn.academy.core.energy.WirelessSystem;
import cn.academy.core.register.ACBlocks;
import cn.academy.energy.block.tile.base.TileNodeBase;
import cn.academy.energy.client.render.tile.RenderMatrix;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegTileEntity;
import cn.liutils.template.block.IMultiTile;
import cn.liutils.template.block.InfoBlockMulti;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegTileEntity
@RegTileEntity.HasRender
public class TileMatrix extends TileNodeBase implements IMultiTile {
	
	@RegTileEntity.Render
	@SideOnly(Side.CLIENT)
	public static RenderMatrix render;
	
	String channelToLoad, pwdToLoad;
	
	InfoBlockMulti info = new InfoBlockMulti(this);
	
	public TileMatrix() {
		super(100000, 512, 30);
	}
	
	public void onBreak() {
		String str = this.getChannel();
		if(str != null) {
			WirelessSystem.removeChannel(worldObj, str);
		}
	}
	
	@Override
	@SideOnly(Side.CLIENT)
    public boolean shouldRenderInPass(int pass) {
        return pass == 1;
    }
	
	//Net info read&write
	@Override
	public void updateEntity() {
		super.updateEntity();
		if(info != null)
			info.update();
		
		if(channelToLoad != null && pwdToLoad != null) {
			WirelessSystem.registerNode(this, channelToLoad);
			WirelessSystem.setPassword(worldObj, channelToLoad, pwdToLoad);
			channelToLoad = pwdToLoad = null;
		}
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		boolean b = WirelessSystem.isTileRegistered(this);
		tag.setBoolean("netLoaded", b);
		info.save(tag);
		if(b) {
			String channel = WirelessSystem.getTileChannel(this);
			tag.setString("netChannel", channel);
			tag.setString("netPwd", WirelessSystem.getPassword(worldObj, channel));
		}
    }
	
    @Override
	public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        boolean b = tag.getBoolean("netLoaded");
        info = new InfoBlockMulti(this, tag);
        if(b) {
        	channelToLoad = tag.getString("netChannel");
        	pwdToLoad = tag.getString("netPwd");
        }
    }

	
	//Head block redirection
	@Override
	public double getEnergy() {
		return getHead().energy;
	}
	
	@Override
	public void setEnergy(double value) {
		getHead().rawSetEnergy(value);
	}
	
	private void rawSetEnergy(double value) {
		super.setEnergy(value);
	}
	
	private TileMatrix getHead() {
		TileEntity ret = ACBlocks.grid.getOriginTile(this);
		return (TileMatrix) (ret instanceof TileMatrix ? ret : this);
	}
	
    @SideOnly(Side.CLIENT)
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
    	return INFINITE_EXTENT_AABB;
    }

	@Override
	public double getSearchRange() {
		return 0;
	}

	@Override
	public InfoBlockMulti getBlockInfo() {
		return info;
	}

	@Override
	public void setBlockInfo(InfoBlockMulti i) {
		info = i;
	}

}
