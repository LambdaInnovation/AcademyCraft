package cn.academy.block.block;

import cn.academy.block.tileentity.TileCatEngine;
import cn.academy.energy.api.WirelessHelper;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.event.energy.LinkUserEvent;
import cn.academy.event.energy.UnlinkUserEvent;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.List;

/**
 * @author WeAthFolD
 */
public class BlockCatEngine extends ACBlockContainer {

    public BlockCatEngine() {
        super(Material.ROCK, null);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileCatEngine();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntity te = world.getTileEntity(pos);
        if(te instanceof TileCatEngine) {
            if(!world.isRemote) {
                TileCatEngine gen = (TileCatEngine) te;
                if(WirelessHelper.isGeneratorLinked(gen)) {
                    MinecraftForge.EVENT_BUS.post(new UnlinkUserEvent(gen));
                    player.sendMessage(new TextComponentTranslation("ac.cat_engine.unlink"));
                } else {
                    List<IWirelessNode> nodes = WirelessHelper.getNodesInRange(world, pos);
                    if(nodes.isEmpty()) {
                        player.sendMessage(new TextComponentTranslation("ac.cat_engine.notfound"));
                    } else {
                        IWirelessNode node = nodes.get(RandUtils.rangei(0, nodes.size()));
                        player.sendMessage(new TextComponentTranslation("ac.cat_engine.linked", node.getNodeName()));
                        MinecraftForge.EVENT_BUS.post(new LinkUserEvent(gen, node));
                    }
                }
            }
            return true;
        }
        
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.INVISIBLE;
    }

}