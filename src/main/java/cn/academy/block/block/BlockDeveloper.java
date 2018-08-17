package cn.academy.block.block;

import cn.academy.ability.develop.DeveloperType;
import cn.academy.block.tileentity.TileDeveloper;
import cn.lambdalib2.registry.mc.RegEventHandler;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 *
 */
public class BlockDeveloper extends ACBlockMulti {
    
    public final DeveloperType type;

    public BlockDeveloper(DeveloperType _type) {
        super(Material.ROCK);
        type = _type;
        setHardness(4.0f);
        setHarvestLevel("pickaxe", 2);

        this.addSubBlock(0, 1, 0);
        this.addSubBlock(0, 0, 1);
        this.addSubBlock(0, 1, 1);
        this.addSubBlock(0, 2, 1);
        this.addSubBlock(0, 0, 2);
        this.addSubBlock(0, 1, 2);
        this.addSubBlock(0, 2, 2);
        
        finishInit();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        TileEntity te = world.getTileEntity(pos);
        if(te instanceof TileDeveloper && !player.isSneaking()) {
            if(!world.isRemote) {
                TileDeveloper td = (TileDeveloper) te;
                if(td.getUser() == null) {
                    td.use(player);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return type == DeveloperType.NORMAL ? new TileDeveloper.Normal() : new TileDeveloper.Advanced();
    }

    @Override
    public double[] getRotCenter() {
        return new double[] { 0.5, 0, 0.5 };
    }

    private enum EventHandler {
        @RegEventHandler
        instance;

        @SideOnly(Side.CLIENT)
        @SubscribeEvent
        public void onDrawBlockHighlight(DrawBlockHighlightEvent event) {
            RayTraceResult res = event.getTarget();
            if (res != null && res.typeOfHit == Type.BLOCK) {
                IBlockState blockState = event.getPlayer().world.getBlockState(
                    res.getBlockPos()
                );
                if (blockState.getBlock() instanceof BlockDeveloper)
                    event.setCanceled(true);
            }
        }
    }

}