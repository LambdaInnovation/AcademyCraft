package cn.academy.block.block;

import cn.academy.ACItems;
import cn.academy.AcademyCraft;
import cn.academy.block.tileentity.TileImagPhase;
import cn.academy.item.ItemMatterUnit;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
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

    public BlockImagPhase() {
        super(ACFluids.fluidImagProj, Material.WATER);
        setCreativeTab(AcademyCraft.cct);

        this.setQuantaPerBlock(3);
        
        MinecraftForge.EVENT_BUS.register(this);
    }

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