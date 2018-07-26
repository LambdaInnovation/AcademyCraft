package cn.academy.energy.api;

import cn.academy.energy.api.block.*;
import cn.academy.energy.internal.NodeConn;
import cn.academy.energy.internal.WiWorldData;
import cn.academy.energy.internal.WirelessNet;
import cn.lambdalib2.util.BlockPos;
import cn.lambdalib2.util.IBlockSelector;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.WorldUtils;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

//import cn.lambdalib2.util.MathUtils;
//import cn.lambdalib2.util.BlockPos;
//import cn.lambdalib2.util.mc.IBlockSelector;
//import cn.lambdalib2.util.mc.WorldUtils;

/**
 * All kinds of funcs about wireless system.
 *
 * @author WeAthFolD
 */
public class WirelessHelper {

    //-----WirelessNetwork

    public static WirelessNet getWirelessNet(IWirelessMatrix matrix) {
        TileEntity tile = (TileEntity) matrix;
        return WiWorldData.get(tile.getWorld()).getNetwork(matrix);
    }

    public static WirelessNet getWirelessNet(IWirelessNode node) {
        TileEntity tile = (TileEntity) node;
        return WiWorldData.get(tile.getWorld()).getNetwork(node);
    }

    /**
     * @return Whether the wireless node is linked into a WEN
     */
    public static boolean isNodeLinked(IWirelessNode node) {
        return getWirelessNet(node) != null;
    }

    /**
     * @return Whether the matrix is initialized with an SSID
     */
    public static boolean isMatrixActive(IWirelessMatrix matrix) {
        return getWirelessNet(matrix) != null;
    }

    /**
     * Get a list of WirelessNet at the position within the given range.
     */
    public static Collection<WirelessNet> getNetInRange(World world, int x, int y, int z, double range, int max) {
        WiWorldData data = WiWorldData.get(world);
        return data.rangeSearch(x, y, z, range, max);
    }

    //-----Node Connection
    public static NodeConn getNodeConn(IWirelessNode node) {
        TileEntity tile = (TileEntity) node;
        return WiWorldData.get(tile.getWorld()).getNodeConnection(node);
    }

    public static NodeConn getNodeConn(IWirelessUser gen) {
        TileEntity tile = (TileEntity) gen;
        return WiWorldData.get(tile.getWorld()).getNodeConnection(gen);
    }

    public static boolean isReceiverLinked(IWirelessReceiver rec) {
        return getNodeConn(rec) != null;
    }

    public static boolean isGeneratorLinked(IWirelessGenerator gen) {
        return getNodeConn(gen) != null;
    }

    /**
     * Get a list of IWirelessNode that are linkable and can reach the given position.
     *
     * @return nodes in the area, does not guarantee any order
     */
    public static List<IWirelessNode> getNodesInRange(World world, int x, int y, int z) {
        double range = 20.0;
        List<BlockPos> list = WorldUtils.getBlocksWithin(world, x, y, z, range, 100, new IBlockSelector() {

            @Override
            public boolean accepts(World world, int x2, int y2, int z2, Block block) {
                TileEntity te = world.getTileEntity(new net.minecraft.util.math.BlockPos(x2, y2, z2));
                if (te instanceof IWirelessNode) {
                    IWirelessNode node = ((IWirelessNode) te);
                    NodeConn conn = getNodeConn((IWirelessNode) te);

                    double distSq = MathUtils.distanceSq(x, y, z, x2, y2, z2);
                    double range = node.getRange();

                    return range * range >= distSq && conn.getLoad() < conn.getCapacity();
                } else {
                    return false;
                }
            }

        });

        List<IWirelessNode> ret = new ArrayList<>();
        for (BlockPos bp : list) {
            ret.add((IWirelessNode) bp.getTile());
        }

        return ret;
    }

}