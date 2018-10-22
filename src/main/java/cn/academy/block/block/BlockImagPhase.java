package cn.academy.block.block;

import cn.academy.ACItems;
import cn.academy.AcademyCraft;
import cn.academy.block.tileentity.TileImagPhase;
import cn.academy.worldgen.WorldGenInit;
import cn.academy.item.ItemMatterUnit;
import cn.academy.item.ItemMatterUnit.MatterMaterial;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fluids.BlockFluidClassic;

/**
 * TODO Implement particle and fog effect
 * @author WeAthFolD
 */
public class BlockImagPhase extends BlockFluidClassic implements ITileEntityProvider {
    
    public static class ItemImpl extends ItemBlock {
        
//        IIcon icon;

        public ItemImpl(Block block) {
            super(block);
        }
        
//        @Override
//        @SideOnly(Side.CLIENT)
//        public void registerIcons(IIconRegister ir) {
//            icon = ir.registerIcon("academy:phase_liquid");
//        }
//
//        @Override
//        @SideOnly(Side.CLIENT)
//        public IIcon getIconFromDamage(int meta) {
//            return icon;
//        }
        
    }
    

    public BlockImagPhase() {
        super(ACFluids.fluidImagProj, Material.WATER);
        setCreativeTab(AcademyCraft.cct);

        this.setQuantaPerBlock(3);
        

        MinecraftForge.EVENT_BUS.register(this);
    }
    
//    @SideOnly(Side.CLIENT)
//    @Override
//    public void registerBlockIcons(IIconRegister ir) {
//        super.registerBlockIcons(ir);
//        fluidIcon = ir.registerIcon("academy:phase_liquid");
//    }
//

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new TileImagPhase();
    }

    @SubscribeEvent
    public void onInteract(PlayerInteractEvent.RightClickBlock event) {
        Block b = event.getWorld().getBlockState(event.getPos()).getBlock();
        ItemStack stack = event.getEntityPlayer().getHeldItem(event.getHand());
        if(b == this && !stack.isEmpty() && stack.getItem() == ACItems.matter_unit) {
            ACItems.matter_unit.setMaterial(stack, ItemMatterUnit.MAT_PHASE_LIQUID);
        }
    }
    
}