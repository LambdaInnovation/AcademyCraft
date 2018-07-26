package cn.academy.energy.block;

import cn.academy.core.block.ACBlockContainer;
import cn.academy.energy.api.WirelessHelper;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.api.event.node.LinkUserEvent;
import cn.academy.energy.api.event.node.UnlinkUserEvent;
import cn.lambdalib2.template.client.render.block.RenderEmptyBlock;
import cn.lambdalib2.util.RandUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;

/**
 * @author WeAthFolD
 */
public class BlockCatEngine extends ACBlockContainer {

    public BlockCatEngine() {
        super("cat_engine", Material.rock, null);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileCatEngine();
    }
    
    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getRenderType() {
        return RenderEmptyBlock.id;
    }
    
    // just let it be...
    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, 
            float tx, float ty, float tz) {
        TileEntity te = world.getTileEntity(x, y, z);
        if(te instanceof TileCatEngine) {
            if(!world.isRemote) {
                TileCatEngine gen = (TileCatEngine) te;
                if(WirelessHelper.isGeneratorLinked(gen)) {
                    MinecraftForge.EVENT_BUS.post(new UnlinkUserEvent(gen));
                    player.addChatMessage(new ChatComponentTranslation("ac.cat_engine.unlink"));
                } else {
                    List<IWirelessNode> nodes = WirelessHelper.getNodesInRange(world, x, y, z);
                    if(nodes.isEmpty()) {
                        player.addChatMessage(new ChatComponentTranslation("ac.cat_engine.notfound"));
                    } else {
                        IWirelessNode node = nodes.get(RandUtils.rangei(0, nodes.size()));
                        player.addChatMessage(new ChatComponentTranslation("ac.cat_engine.linked", node.getNodeName()));
                        MinecraftForge.EVENT_BUS.post(new LinkUserEvent(gen, node));
                    }
                }
            }
            return true;
        }
        
        return false;
    }

}