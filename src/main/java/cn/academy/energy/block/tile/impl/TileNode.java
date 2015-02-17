/**
 * 
 */
package cn.academy.energy.block.tile.impl;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.energy.block.tile.base.TileNodeBase;
import cn.academy.energy.util.EnergyUtils;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegTileEntity;
import cn.liutils.api.draw.DrawObject;
import cn.liutils.api.draw.tess.Rect;
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
public class TileNode extends TileNodeBase implements IInventory {
	
	@SideOnly(Side.CLIENT)
	@RegTileEntity.Render
	public static NodeRender renderer;

	public TileNode() {
		super(10000, 128, 30);
	}
	
	@SideOnly(Side.CLIENT)
	/**
	 * Not using utils because we almost don't render anything this way.
	 * if we (in the future) have just make this a base class
	 * @author WeathFolD
	 */
	public static class NodeRender extends TileEntitySpecialRenderer {
		
		static final ResourceLocation 
			TEX = new ResourceLocation("academy:textures/blocks/nodes_tex.png"),
			TEX_BTM = new ResourceLocation("academy:textures/blocks/nodes_main.png");
		
		DrawObject piece;
		Rect rect;
		
		public NodeRender() {
			piece = new DrawObject();
			rect = new Rect(1, 1);
			piece.addHandler(rect);
		}
		
		final int[] rots = { 0, 90, -90, 180 };
		final int[][] offsets = {
			{0, 0}, {0, 1}, {1, 0}, {1, 1}
		};

		@Override
		public void renderTileEntityAt(TileEntity te, double x,
				double y, double z, float w) {
			TileNode node = (TileNode) te;
			RenderUtils.loadTexture(TEX);
			GL11.glPushMatrix(); {
				GL11.glTranslated(x, y, z);
				
				for(int i = 0; i < 4; ++i) {
					GL11.glPushMatrix();
					GL11.glTranslated(offsets[i][0], 0, offsets[i][1]);
					GL11.glRotated(rots[i], 0, 1, 0);
					
					//original
					rect.setSize(1, 1);
					rect.map.set(0, 0, 32.0 / 34.0, 1);
					piece.draw();
					
					//energy bar
					GL11.glTranslated(-0.001, 4 / 32.0, 21.0 / 32.0);
					double cur = node.getEnergy() / node.getMaxEnergy();
					rect.map.set(32.0 / 34.0, 0, 2.0 / 34.0, cur * (15.0 / 32.0));
					rect.setSize(2.0 / 34.0, cur * (15.0 / 32.0));
					piece.draw();
					
					GL11.glPopMatrix();
				}
				rect.setSize(1, 1);
				
				RenderUtils.loadTexture(TEX_BTM);
				GL11.glPushMatrix();
				GL11.glTranslated(1, 0, 0);
				GL11.glRotated(90, 0, 0, 1);
				rect.map.set(0, 0, 1, 1);
				piece.draw();
				GL11.glPopMatrix();
				
				GL11.glPushMatrix();
				GL11.glTranslated(0, 1, 0);
				GL11.glRotated(-90, 0, 0, 1);
				piece.draw();
				GL11.glPopMatrix();
				
			} GL11.glPopMatrix();
		}
		
	}

	@Override
	public void updateEntity() {
		super.updateEntity();
		//System.out.println(energy);
	}
	
	
	//---Inventory part
	ItemStack battery; //battery slot
	
	@Override
	public int getSizeInventory() {
		return 1;
	}

	@Override
	public ItemStack getStackInSlot(int var1) {
		return battery;
	}

	@Override
	public ItemStack decrStackSize(int slot, int count) {
		if(battery.stackSize > 1) {
			--battery.stackSize;
			return battery;
		}
		battery = null;
		return null;
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int var1) {
		return battery;
	}

	@Override
	public void setInventorySlotContents(int slot, ItemStack stack) {
		battery = stack;
	}

	@Override
	public String getInventoryName() {
		return "ac_node";
	}

	@Override
	public boolean hasCustomInventoryName() {
		return false;
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer player) {
		return player.getDistanceSq(xCoord + .5, yCoord + .5, zCoord + .5) < 12;
	}

	@Override
	public void openInventory() {}

	@Override
	public void closeInventory() {}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		return EnergyUtils.isElecItem(stack);
	}
	
    public void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);
        NBTTagCompound tmp = tag.getCompoundTag("battery");
        if(tmp != null)
        	battery = ItemStack.loadItemStackFromNBT(tmp);
    }

    public void writeToNBT(NBTTagCompound tag)
    {
        super.writeToNBT(tag);
        if(battery != null) {
        	NBTTagCompound tmp = new NBTTagCompound();
        	battery.writeToNBT(tmp);
        	tag.setTag("battery", tmp);
        }
    }

}
