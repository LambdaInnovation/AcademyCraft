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
package cn.academy.energy.block;

import net.minecraft.tileentity.TileEntity;
import cn.academy.core.block.TileGeneratorBase;
import cn.academy.energy.client.render.block.RenderCatEngine;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegTileEntity;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.StorageOption;
import cn.annoreg.mc.s11n.StorageOption.Data;
import cn.annoreg.mc.s11n.StorageOption.RangedTarget;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Cat Engine!
 * @author WeAthFolD
 */
@Registrant
@RegTileEntity
@RegTileEntity.HasRender
public class TileCatEngine extends TileGeneratorBase {
	
	@SideOnly(Side.CLIENT)
	@RegTileEntity.Render
	public static RenderCatEngine renderer;
	
	// Sync
	int syncTicker;
	
	// Intrusive render parameters
	public double thisTickGen;
	public double rotation;
	public long lastRender;

	public TileCatEngine() {
		super("infinite_generator", 0, 2000, 200);
	}

	@Override
	public void updateEntity() {
		super.updateEntity();
		
		if(!getWorldObj().isRemote) {
			if(++syncTicker == 20) {
				syncTicker = 0;
				syncGen(this, thisTickGen);
			}
		}
	}
	
	@Override
	public double getGeneration(double required) {
		return (thisTickGen = Math.min(required, 500));
	}
	
	@RegNetworkCall(side = Side.CLIENT)
	private void syncGen(@RangedTarget(range = 10) TileCatEngine te, @Data Double amt) {
		te.thisTickGen = amt;
	}

}