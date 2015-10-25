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
package cn.academy.crafting.block;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;
import net.minecraftforge.fluids.BlockFluidClassic;
import cn.academy.core.AcademyCraft;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.crafting.item.ItemMatterUnit;
import cn.academy.crafting.item.ItemMatterUnit.MatterMaterial;
import cn.annoreg.core.Registrant;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * TODO Implement particle and fog effect
 * @author WeAthFolD
 */
@Registrant
public class BlockImagPhase extends BlockFluidClassic implements ITileEntityProvider {
	
	public static class ItemPhaseLiq extends ItemBlock {
		
		IIcon icon;

		public ItemPhaseLiq(Block block) {
			super(block);
		}
		
		@Override
	    @SideOnly(Side.CLIENT)
	    public void registerIcons(IIconRegister ir) {
	    	icon = ir.registerIcon("academy:phase_liquid");
	    }
		
	    @Override
		@SideOnly(Side.CLIENT)
	    public IIcon getIconFromDamage(int meta) {
	        return icon;
	    }
		
	}
	
	public final MatterMaterial mat;
	public IIcon fluidIcon;
	
	public BlockImagPhase() {
		super(ModuleCrafting.fluidImagProj, Material.water);
		setCreativeTab(AcademyCraft.cct);
		setBlockName("ac_phase_liquid");
		setBlockTextureName("academy:black");
		
		this.setQuantaPerBlock(3);
		
		mat = new MatterMaterial("phase_liquid", this);
		ItemMatterUnit.addMatterMaterial(mat);
		
		MinecraftForge.EVENT_BUS.register(this);
	}
	
    @SideOnly(Side.CLIENT)
    @Override
    public void registerBlockIcons(IIconRegister ir) {
    	super.registerBlockIcons(ir);
    	fluidIcon = ir.registerIcon("academy:phase_liquid");
    }
	
	
	@SideOnly(Side.CLIENT)
    @Override
    public int getRenderBlockPass() {
        return 1;
    }
	
	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int metadata) {
		return new TileImagPhase();
	}

	@SubscribeEvent
	public void onInteract(PlayerInteractEvent event) {
		if(event.action == Action.RIGHT_CLICK_BLOCK) {
			Block b = event.world.getBlock(event.x, event.y, event.z);
			ItemStack stack = event.entityPlayer.getCurrentEquippedItem();
			if(b == this && stack != null && stack.getItem() == ModuleCrafting.matterUnit) {
				ModuleCrafting.matterUnit.setMaterial(stack, mat);
			}
		}
	}
	
}
