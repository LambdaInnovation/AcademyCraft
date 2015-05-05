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
package cn.academy.energy.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import cn.academy.core.AcademyCraft;
import cn.academy.energy.api.IWirelessMatrix;
import cn.academy.energy.api.IWirelessNode;
import cn.academy.energy.api.IWirelessTile;
import cn.liutils.util.GenericUtils;

/**
 * @author WeathFolD
 */
public class WiWorldData extends WorldSavedData {
    
    public static final String ID = "WEN_DATA";
    
    World world; //Instance injected by WirelessSystem
    
    public WiWorldData() {
    	this("WTF");
    }
    
    public WiWorldData(String wtf) {
        super(ID);
    }
    
    void tick() {
        Iterator<NodeConn> iter1 = nodeConns.values().iterator();
        while(iter1.hasNext()) {
            NodeConn conn = iter1.next();
            conn.tick();
            if(conn.dead) {
                conn.onRemoved();
                iter1.remove();
            }
        }
        
        Iterator<WirelessNetwork> iter2 = aliveNetworks.values().iterator();
        while(iter2.hasNext()) {
            WirelessNetwork net = iter2.next();
            net.tick();
            if(net.dead) {
                net.onDestroyed();
                iter2.remove();
            }
        }
        
        this.markDirty(); //Always save
    }

    //---Matrix Connections
    /**
     * You can use one of the following to look for a network:<br/>
     * -Strings(ssid) <br/>
     * -Coords(Matrix position or node position)
     * [Node position handled by WirelessNetwork itself!]
     */
    Map<Object, WirelessNetwork> aliveNetworks = new HashMap();
    
    public boolean isMatrixOrNodePresent(Coord pos) {
    	System.out.println("imnp");
    	return aliveNetworks.containsKey(pos);
    }
    
    public boolean isSSIDPresent(String ssid) {
    	return aliveNetworks.containsKey(ssid);
    }
    
    public String getSSID(Coord pos) {
    	WirelessNetwork net = aliveNetworks.get(pos);
    	return net == null ? null : net.ssid;
    }
    
    public boolean createNetwork(Coord mat, String ssid, boolean encrypted, String pwd) {
    	//TODO: Check if the network is already disposed that time
        if(aliveNetworks.get(ssid) != null || aliveNetworks.get(mat) != null) {
            AcademyCraft.log.error("Network already exists, cant duplicate #" 
                    + mat + " with ssid " + ssid);
            return false;
        }
        TileEntity te = mat.getAndCheck();
        if(te == null) {
            AcademyCraft.log.error("Invalid matrix connection request #" + mat);
            return false;
        }
        
        System.out.println("Creating network " + ssid);
        WirelessNetwork net = new WirelessNetwork(this, (IWirelessMatrix) te, ssid, encrypted, pwd);
        aliveNetworks.put(ssid, net);
        aliveNetworks.put(mat, net);
        return true;
    }
    
    public void destroyNetwork(Coord mat) {
        WirelessNetwork net = aliveNetworks.get(mat);
        if(net == null) {
            AcademyCraft.log.error("Try to destroy a non-existent wireless net #" + mat);
            return;
        }
        //Clean up the lookup
        net.onDestroyed();
        aliveNetworks.remove(net.ssid);
        aliveNetworks.remove(mat);
    }
    
    public boolean linkNode(String ssid, Coord node, String pwd) {
        if(aliveNetworks.containsKey(node)) {
            unlinkNode(node);
        }
        WirelessNetwork net = aliveNetworks.get(ssid);
        if(net == null) {
            AcademyCraft.log.error("Try to link node " + 
                    node + " to a non-existent net: " + ssid);
            return false;
        }
        //Validation
        if(net.isEncrypted && !pwd.equals(net.password)) {
            return false;
        }
        net.addNode(node);
        return true;
    }
    
    public void unlinkNode(Coord node) {
        WirelessNetwork net = aliveNetworks.get(node);
        if(net == null) {
            AcademyCraft.log.error("Try to unlink a not-connected node #" + node);
            return;
        }
        net.removeNode(node);
    }
    
    //---Node Connections
    /**
     * You can use one of the following to look for a node connection: <br/>
     * -Coord of node </br>
     * -Coord of generator/receiver [Handled by NodeConn itself]
     */
    Map<Coord, NodeConn> nodeConns = new HashMap();
    
    public boolean isUserPresent(Coord coord) {
    	return nodeConns.containsKey(coord);
    }
    
    public void linkGenerator(Coord gen, Coord node) {
        NodeConn conn = getConnFor(node);
        if(conn == null) {
            AcademyCraft.log.error("Try to link to a node that doesn't exist.");
            return;
        }
        conn.addGenerator(gen);
    }
    
    public void linkReceiver(Coord rec, Coord node) {
        NodeConn conn = getConnFor(node);
        if(conn == null) {
            AcademyCraft.log.error("Try to link to a node that doesn't exist.");
            return;
        }
        conn.addReceiver(rec);
    }
    
    public void unlinkGenerator(Coord c) {
        NodeConn conn = getConnFor(c);
        if(conn == null) {
            AcademyCraft.log.error("Try to link to a node that doesn't exist.");
            return;
        }
        conn.unlinkGenerator(c);
    }
    
    public void unlikReceiver(Coord c) {
        NodeConn conn = getConnFor(c);
        if(conn == null) {
            AcademyCraft.log.error("Try to link to a node that doesn't exist.");
            return;
        }
        conn.unlinkReceiver(c);
    }
    
    /**
     * Lazy init. Returns a correct connection as long as the target coord validates correctly.
     */
    private NodeConn getConnFor(Coord node) {
        TileEntity te = node.getAndCheck();
        if(te == null) return null;
        NodeConn ret = nodeConns.get(node);
        if(ret == null) {
            ret = new NodeConn(this, (IWirelessNode) te);
            nodeConns.put(node, ret);
        }
        return ret;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        //Restore conn
        int n = tag.getInteger("conns_size");
        
        for(int i = 0; i < n; ++i) {
            NodeConn nc = new NodeConn(this, (NBTTagCompound) tag.getTag("conn_" + i));
            nodeConns.put(nc.node, nc);
        }
        
        //Restore WENs
        n = tag.getInteger("wen_size");
        for(int i = 0; i < n; ++i) {
            WirelessNetwork wn = new WirelessNetwork(this, (NBTTagCompound) tag.getTag("wen_" + i));
            aliveNetworks.put(wn.matrix, wn);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        //Store conn
        tag.setInteger("conns_size", nodeConns.size());
        
        int i = 0;
        for(NodeConn conn : nodeConns.values()) {
            NBTTagCompound tag2 = new NBTTagCompound();
            conn.save(tag2);
            tag.setTag("conn_" + i, tag2);
            ++i;
        }
        
        //Store WENs
        tag.setInteger("wen_size", aliveNetworks.size());
        i = 0;
        for(WirelessNetwork wn : aliveNetworks.values()) {
            NBTTagCompound tag2 = new NBTTagCompound();
            wn.save(tag2);
            tag.setTag("wen_" + i, tag2);
            ++i;
        }
    }
    
    //Lookup part
    Map<ChunkCoord, Set<Coord>> 
        wirelessNetLookup = new HashMap(),
        nodeConnLookup = new HashMap();
    
    Set<Coord> lazyNetLookup(ChunkCoord cc) {
        Set<Coord> ret = wirelessNetLookup.get(cc);
        if(ret == null) {
            ret = new HashSet();
            wirelessNetLookup.put(cc, ret);
        }
        return ret;
    }
    
    Set<Coord> lazyNodeConnLookup(ChunkCoord cc) {
        Set<Coord> ret = nodeConnLookup.get(cc);
        if(ret == null) {
            ret = new HashSet();
            nodeConnLookup.put(cc, ret);
        }
        return ret;
    }
    
    /**
     * Get all the available SSIDs for an wirelss node.
     */
    public Collection<String> getAvailableSSIDs(IWirelessNode inode) {
        TileEntity te = (TileEntity) inode;
        double x = te.xCoord + .5, y = te.yCoord + .5, z = te.zCoord + .5;
        final int maxSize = 20;
        Set<String> ret = new HashSet();
        
        ChunkCoord cc = new ChunkCoord();
        for(int i = -1; i <= 1; ++i) {
            for(int j = -1; j <= 1; ++j) {
                cc.cx = te.xCoord >> 4 + i;
                cc.cz = te.zCoord >> 4 + j;
                Set<Coord> targets = wirelessNetLookup.get(cc);
                if(targets != null) {
                    for(Coord c : targets) {
                        WirelessNetwork net = aliveNetworks.get(c);
                        if(net == null) {
                            AcademyCraft.log.error("ERROR: WEN Lookup leak #" + c);
                            continue;
                        }
                        double dist = GenericUtils.distanceSq(new double[] { x, y, z }, new double[] { c.x + 0.5, c.y + 0.5, c.z + 0.5 });
                        if(net.range * net.range >= dist) {
                            ret.add(net.ssid);
                            if(ret.size() == maxSize)
                                return ret;
                        }
                    }
                }
            }
        }
        return ret;
    }
    
    /**
     * Get all the available nodes for an wireless user(receiver/generator).
     */
    public List<Coord> getAvailableNodes(IWirelessTile user) {
        TileEntity te = (TileEntity) user;
        final double x = te.xCoord + .5, y = te.yCoord + .5, z = te.zCoord + .5;
        final int maxSize = 30;
        List<Coord> ret = new ArrayList();
        
        ChunkCoord cc = new ChunkCoord();
        for(int i = -1; i <= 1 && ret.size() < maxSize; ++i) {
            for(int j = -1; j <= 1 && ret.size() < maxSize; ++j) {
                cc.cx = te.xCoord >> 4 + i;
                cc.cz = te.zCoord >> 4 + j;
                Set<Coord> targets = nodeConnLookup.get(cc);
                if(targets != null) {
                    for(Coord c : targets) {
                        IWirelessNode node;
                        if((node = (IWirelessNode) c.getAndCheck()) == null) {
                            AcademyCraft.log.error("ERROR: NodeConn Lookup leak #" + c);
                            continue;
                        }
                        double dist = GenericUtils.distanceSq(new double[] { x, y, z }, 
                                new double[] { c.x + 0.5, c.y + 0.5, c.z + 0.5 });
                        if(node.getLatency() * node.getLatency() >= dist) {
                            ret.add(c);
                            if(ret.size() == maxSize)
                                break;
                        }
                    }
                }
            }
        }
        //Sort by distance.
        Collections.sort(ret, new Comparator<Coord>() {
            @Override
            public int compare(Coord a, Coord b) {
                double d1 = GenericUtils.distanceSq(x, y, z, a.x + .5, a.y + .5, a.z + .5),
                        d2 = GenericUtils.distanceSq(x, y, z, a.x + .5, a.y + .5, a.z + .5);
                return d1 < d2 ? 1 : (d1 == d2 ? 0 : -1);
            }
        });
        return ret;
    }
    
    /**
     * Helper class representing Chunk Coordinate
     * @author WeathFolD
     */
    static class ChunkCoord {
        int cx, cz;
        
        public ChunkCoord() {}
        
        public ChunkCoord(int x, int z) {
            cx = x;
            cz = z;
        }
        
        public ChunkCoord(IWirelessTile te) {
            this((TileEntity) te);
        }
        
        public ChunkCoord(TileEntity te) {
            this(te.xCoord >> 4, te.zCoord >> 4);
        }
        
        public ChunkCoord(Coord c) {
            this(c.x >> 4, c.z >> 4);
        }
        
        @Override
        public int hashCode() {
            return cx ^ cz;
        }
        
        @Override
        public boolean equals(Object obj) {
            if(!(obj instanceof ChunkCoord))
                return false;
            ChunkCoord c = (ChunkCoord)obj;
            return c.cx == cx && c.cz == cz;
        }
    }

}
