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

import net.minecraft.world.World;
import cn.academy.energy.block.tile.base.ACGeneratorBase;
import cn.academy.energy.client.render.tile.RenderSolarGen;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegTileEntity;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 太阳能发电机TileEntity
 * @author WeAthFolD, Jiangyue
 */
@RegistrationClass
@RegTileEntity
@RegTileEntity.HasRender
public class TileSolarGenerator extends ACGeneratorBase {
    
    private final double MAX_EU = 2000.0;
    private final double LATENCY = 400.0;
    
	@SideOnly(Side.CLIENT)
	@RegTileEntity.Render
	public static RenderSolarGen render;
    
    public TileSolarGenerator() {
        super();
        this.setMaxEnergy(MAX_EU);
    }
    
    /**
     * Add energy to self on tick update.
     */
    @Override
    public void updateEntity() {
        super.updateEntity();
        /* Judge the state to determine how much Energy should offer */
        World theWorld = this.getWorldObj();
        double brightLev = theWorld.isDaytime() && theWorld.canBlockSeeTheSky(xCoord, yCoord + 1, zCoord) ? 1.0 : 0.0;
        int euToAdd = (int) brightLev * 5;
        addEnergy(euToAdd);
    }

	@Override
	public double getSearchRange() {
		return 24;
	}
	
}
