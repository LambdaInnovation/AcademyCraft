/**
 * Copyright (C) Lambda-Innovation, 2013-2014
 * This code is open-source. Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer. 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
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
