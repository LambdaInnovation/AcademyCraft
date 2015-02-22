/**
 * 
 */
package cn.academy.energy.block.tile.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelCustom;

import org.lwjgl.opengl.GL11;

import cn.academy.core.energy.WirelessSystem;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.core.register.ACBlocks;
import cn.academy.energy.block.tile.base.TileNodeBase;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegTileEntity;
import cn.liutils.template.client.render.block.RenderTileDirMulti;
import cn.liutils.util.DebugUtils;
import cn.liutils.util.RenderUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegTileEntity
@RegTileEntity.HasRender
public class TileMatrix extends TileNodeBase {
	
	@RegTileEntity.Render
	@SideOnly(Side.CLIENT)
	public static GridRender render;
	
	String channelToLoad, pwdToLoad;
	
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
		if(channelToLoad != null && pwdToLoad != null) {
			System.out.println("Restoring " + channelToLoad + " " + DebugUtils.formatArray(xCoord, yCoord, zCoord));
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
		int[] c = ACBlocks.grid.getOrigin(worldObj, xCoord, yCoord, zCoord, getBlockMetadata());
		TileEntity te = worldObj.getTileEntity(c[0], c[1], c[2]);
		return (TileMatrix) (te instanceof TileMatrix ? te : this);
	}
	
	@SideOnly(Side.CLIENT)
	public static class GridRender extends RenderTileDirMulti {
		
		IModelCustom model = ACClientProps.MDL_GRID;
		ResourceLocation tex = ACClientProps.TEX_MDL_GRID;

		public GridRender() {}
		
		@Override
		public void renderAtOrigin(TileEntity te) {
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glPushMatrix(); {
				GL11.glTranslated(-1, 0, -1);
				double scale = 0.22;
				GL11.glScaled(scale, scale, scale);
				RenderUtils.loadTexture(tex);
				
				GL11.glDepthMask(true);
				model.renderPart("base");
				
				GL11.glPushMatrix(); {
					GL11.glTranslated(0, 6.3, 0);
					drawCube();
				} GL11.glPopMatrix();
				
				GL11.glDepthMask(false);
				
				RenderUtils.loadTexture(tex);
				model.renderPart("plate");
				
			} GL11.glPopMatrix();
		}
		
		private void drawCube() {
			GL11.glTranslated(0, 0.6 * Math.sin(Minecraft.getSystemTime() / 400D), 0);
			GL11.glRotated(Minecraft.getSystemTime() / 25D, 1, 1, 1);
			GL11.glRotated(Minecraft.getSystemTime() / 50D, 2, 0, 1);
			final double size = 3.2, hs = size * 0.5;
			GL11.glTranslated(-hs, -hs, -hs);
			GL11.glColor4d(1, 1, 1, 0.7);
			RenderUtils.loadTexture(ACClientProps.TEX_MDL_GRID_BLOCK);
			RenderUtils.drawCube(size, size, size);
			GL11.glColor4d(1, 1, 1, 1);
		}
		
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

}
