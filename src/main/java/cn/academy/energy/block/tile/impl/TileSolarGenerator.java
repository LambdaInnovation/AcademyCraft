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

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.IModelCustom;

import org.lwjgl.opengl.GL11;

import cn.academy.api.energy.IWirelessGenerator;
import cn.academy.api.energy.IWirelessNode;
import cn.academy.api.energy.IWirelessTile;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.energy.block.tile.base.ACGeneratorBase;
import cn.academy.energy.block.tile.base.TileUserBase;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegTileEntity;
import cn.liutils.util.RenderUtils;
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
    
    @RegTileEntity.Render
	@SideOnly(Side.CLIENT)
	public static SGRender render;
    
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
    
	@SideOnly(Side.CLIENT)
	public static class SGRender extends TileEntitySpecialRenderer {

		IModelCustom model = ACClientProps.MDL_SOLAR;
		ResourceLocation tex = ACClientProps.TEX_MDL_SOLAR;
		
		@Override
		public void renderTileEntityAt(TileEntity te, double x,
				double y, double z, float u) {
			GL11.glPushMatrix();
			GL11.glTranslated(x + .5, y, z + .5);
			double scale = 0.018;
			GL11.glScaled(scale, scale, scale);
			RenderUtils.loadTexture(tex);
			model.renderAll();
			GL11.glPopMatrix();
		}
		
	}

	@Override
	public double getSearchRange() {
		return 24;
	}
	
}
