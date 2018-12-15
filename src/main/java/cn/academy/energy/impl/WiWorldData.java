package cn.academy.energy.impl;

import cn.academy.AcademyCraft;
import cn.academy.energy.api.block.*;
import cn.academy.energy.impl.VBlocks.*;
import cn.lambdalib2.util.IBlockSelector;
import cn.lambdalib2.util.WorldUtils;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
//import net.minecraft.world.WorldSavedData;

import java.util.*;

/**
 * @author WeAthFolD
 *
 */
public class WiWorldData extends WorldSavedData {
    
    public static final String ID = "AC_WEN_";
    
    static String getID(World world) {
        return ID + world.provider.getDimension();
    }
    
    //Set by get method, which should be the ONLY way to access WiWorldData
    World world;

    public WiWorldData(String fuckyoumojang) {
        super(fuckyoumojang);
    }
    
    //-----WEN-----
    
    private IBlockSelector filterWirelessBlocks = new IBlockSelector() {

        @Override
        public boolean accepts(World world, int x, int y, int z, Block block) {
            TileEntity te = world.getTileEntity(new net.minecraft.util.math.BlockPos(x, y, z));
            return te instanceof IWirelessMatrix || te instanceof IWirelessNode;
        }
        
    };
    
    /***
     * Object valid for lookup: VWMatrix, VWNode
     */
    Map<Object, WirelessNet> netLookup = new HashMap<>();
    Set<WirelessNet> netList = new HashSet<>();
    
    /**
     * Internal, used to prevent concurrent modification.
     */
    private List<WirelessNet> toRemove = new ArrayList<>();
    
    private void tickNetwork() {
        for(WirelessNet net : toRemove) {
            this.doRemoveNetwork(net);
        }
        toRemove.clear();
        
        Iterator<WirelessNet> iter = netList.iterator();
        while (iter.hasNext()) {
            WirelessNet net = iter.next();
            if(net.isDisposed()) {
                toRemove.add(net);
            } else {
                net.world = world;
                net.tick();
            }
        }
    }
    
    boolean createNetwork(IWirelessMatrix matrix, String ssid, String password) {
        // Kill old net of the same matrix, if any
        VWMatrix vm = new VWMatrix(matrix);
        if(netLookup.containsKey(vm)) {
            WirelessNet old = netLookup.get(vm);
            doRemoveNetwork(old);
        }
        
        //Add new
        WirelessNet net = new WirelessNet(this, vm, ssid, password);
        doAddNetwork(net);
        
        return true;
    }

    private final List<BlockPos> _bufferBlockPos = new ArrayList<>();
    
    public Collection<WirelessNet> rangeSearch(int x, int y, int z, double range, int max) {
        WorldUtils.getBlocksWithin(_bufferBlockPos, world, x, y, z, range, max, filterWirelessBlocks);
        
        Set<WirelessNet> set = new HashSet<>();
        for(BlockPos bp : _bufferBlockPos) {
            TileEntity te = world.getTileEntity(bp);
            WirelessNet net;
            if(te instanceof IWirelessMatrix) {
                net = getNetwork((IWirelessMatrix) te);
            } else if(te instanceof IWirelessNode) {
                net = getNetwork((IWirelessNode) te);
            } else {
                throw new RuntimeException("Invalid TileEntity");
            }
            if(net != null && net.isInRange(x, y, z) && net.getLoad() < net.getCapacity()) {
                set.add(net);
                if(set.size() >= max)
                    return set;
            }
        }

        return set;
    }
    
    public WirelessNet getNetwork(IWirelessMatrix matrix) {
        return privateGetNetwork(new VWMatrix(matrix));
    }
    
    public WirelessNet getNetwork(IWirelessNode node) {
        return privateGetNetwork(new VWNode(node));
    }

    private WirelessNet privateGetNetwork(Object key) {
        WirelessNet ret = netLookup.get(key);
        if (ret != null && ret.validate()) {
            return ret;
        } else {
            return null;
        }
    }
    
    private void doRemoveNetwork(WirelessNet net) {
        netList.remove(net);
        net.onCleanup(this);
    }
    
    private void doAddNetwork(WirelessNet net) {
        netList.add(net);
        net.onCreate(this);
    }
    
    private void loadNetwork(NBTTagCompound tag) {
        NBTTagList list = (NBTTagList) tag.getTag("networks");
        for(int i = 0; i < list.tagCount(); ++i) {
            NBTTagCompound tag2 = list.getCompoundTagAt(i);
            WirelessNet net = new WirelessNet(this, tag2);
            doAddNetwork(net);
        }
    }
    
    private void saveNetwork(NBTTagCompound tag) {
        NBTTagList list = new NBTTagList();
        for(WirelessNet net : netList) {
            if(!net.isDisposed()) {
                list.appendTag(net.toNBT());
            }
        }
        tag.setTag("networks", list);
    }
    
    //-----NodeConn----
    Map<Object, NodeConn> nodeLookup = new HashMap<>();
    Set<NodeConn> nodeList = new HashSet<>();
    List<NodeConn> nToRemove = new ArrayList<>();
    
    /**
     * Get the node connection of a node. If not found will create a new one. Never returns null.
     */
    public NodeConn getNodeConnection(IWirelessNode node) {
        VNNode vnn = new VNNode(node);
        NodeConn ret = privateGetNodeConn(vnn);
        if(ret == null) {
            doAddNode(ret = new NodeConn(this, vnn));
        }
        return ret;
    }
    
    public NodeConn getNodeConnection(IWirelessUser user) {
        if(user instanceof IWirelessGenerator) {
            return privateGetNodeConn(new VNGenerator((IWirelessGenerator) user));
        } else if(user instanceof IWirelessReceiver) {
            return privateGetNodeConn(new VNReceiver((IWirelessReceiver) user));
        } else if (user == null) {
            return null;
        } else {
            throw new IllegalArgumentException("Invalid user type");
        }
    }

    private NodeConn privateGetNodeConn(Object key) {
        NodeConn ret = nodeLookup.get(key);
        if (ret != null && ret.validate()) {
            return ret;
        } else {
            return null;
        }
    }
    
    private void tickNode() {
        //if(true) return;
        
        for(NodeConn nc : nToRemove) {
            doRemoveNode(nc);
        }
        nToRemove.clear();
        
        Iterator<NodeConn> iter = nodeList.iterator();
        while(iter.hasNext()) {
            NodeConn conn = iter.next();
            if(conn.isDisposed()) {
                nToRemove.add(conn);
            } else {
                conn.tick();
            }
        }
    }
    
    private void doAddNode(NodeConn conn) {
        nodeList.add(conn);
        conn.onAdded(this);
    }
    
    private void doRemoveNode(NodeConn conn) {
        nodeList.remove(conn);
        conn.onCleanup(this);
    }
    
    private void loadNode(NBTTagCompound tag) {
        NBTTagList list = (NBTTagList) tag.getTag("list");
        debug("LoadNode " + list + (list.tagCount()));
        for(int i = 0; i < list.tagCount(); ++i) {
            doAddNode(new NodeConn(this, list.getCompoundTagAt(i)));
        }
    }
    
    private void saveNode(NBTTagCompound tag) {
        NBTTagList list = new NBTTagList();
        for(NodeConn c : nodeList) {
            if(!c.isDisposed()) {
                list.appendTag(c.toNBT());
            }
        }
        tag.setTag("list", list);
    }
    
    //-----Generic-----
    public void tick() {
        tickNetwork();
        tickNode();
    }
    
    @Override
    public void readFromNBT(NBTTagCompound tag) {
        
        NBTTagCompound tag1 = (NBTTagCompound) tag.getTag("net");
        if(tag1 != null)
            loadNetwork(tag1);
        
        tag1 = (NBTTagCompound) tag.getTag("node");
        if(tag1 != null)
            loadNode(tag1);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        NBTTagCompound tag1 = new NBTTagCompound();
        saveNetwork(tag1);
        tag.setTag("net", tag1);
        
        tag1 = new NBTTagCompound();
        saveNode(tag1);
        tag.setTag("node", tag1);
        return tag;
    }
    
    public static WiWorldData get(World world) {
        if(world.isRemote) {
            throw new RuntimeException("Not allowed to create WiWorldData in client");
        }
//        MapStorage storage = IS_GLOBAL ? world.getMapStorage() : world.getPerWorldStorage();
        MapStorage storage = world.getPerWorldStorage();
        String id = getID(world);
        WiWorldData ret = (WiWorldData) storage.getOrLoadData(WiWorldData.class, id);
        if(ret == null) {
            storage.setData(id, ret = new WiWorldData(id));
        }
        ret.world = world;
        return ret;
    }
    
    public static WiWorldData getNonCreate(World world) {
        MapStorage storage = world.getPerWorldStorage();
        WiWorldData data = (WiWorldData) storage.getOrLoadData(WiWorldData.class, getID(world));
        if(data != null) data.world = world;
        return data;
    }
    
    @Override
    public boolean isDirty() {
        return true;
    }
    
    private void debug(Object msg) {
        AcademyCraft.log.info("WiWorldData: " + msg);
    }

}