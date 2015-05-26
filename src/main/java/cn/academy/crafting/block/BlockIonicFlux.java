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
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialLiquid;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import cn.academy.core.AcademyCraft;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.crafting.item.ItemMatterUnit;
import cn.academy.crafting.item.ItemMatterUnit.MatterMaterial;
import cn.annoreg.core.Registrant;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * TODO: Implement particle and fog effect
 * @author WeAthFolD
 */
@Registrant
public class BlockIonicFlux extends BlockFluidClassic implements ITileEntityProvider {
	
	public final MatterMaterial mat;

	public BlockIonicFlux() {
		super(ModuleCrafting.fluidImagIon, Material.water);
		setCreativeTab(AcademyCraft.cct);
		setBlockName("ac_ionic_flux");
		setBlockTextureName("academy:black");
		
		this.setQuantaPerBlock(3);
		
		mat = new MatterMaterial("imag_ionic", this);
		ItemMatterUnit.addMatterMaterial(mat);
		
		MinecraftForge.EVENT_BUS.register(this);
	}    
	
	@SideOnly(Side.CLIENT)
    @Override
    public int getRenderBlockPass() {
        return 0;
    }
	
	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int metadata) {
		return new TileIonicFlux();
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
