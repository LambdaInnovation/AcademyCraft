/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * This project is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.energy.internal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import cn.academy.core.AcademyCraft;
import cn.academy.energy.api.IWirelessNode;

/**
 * @author WeathFolD
 *
 */
public class WiWorldData extends WorldSavedData {
    
    public static final String ID = "WEN_DATA";
    
    final World world;
    
    public WiWorldData(World _world) {
        super(ID);
        world = _world;
    }
    
    void tick() {
        Iterator<Map.Entry<Coord, NodeConn>> iter1 = nodeConns.entrySet().iterator();
        while(iter1.hasNext()) {
            NodeConn conn = iter1.next().getValue();
            conn.tick();
            if(conn.dead) iter1.remove();
        }
    }

    //---Matrix Connections
    /**
     * You can use one of the following to look for a network:<br/>
     * -Strings(ssid) <br/>
     * -Coords(Matrix position or node position)
     */
    Map<Object, WirelessNetwork> aliveNetworks = new HashMap();
    
    //---Node Connections
    /**
     * You can use one of the following to look for a node connection: <br/>
     * -Coord of node </br>
     * -Coord of generator/receiver
     */
    Map<Coord, NodeConn> nodeConns = new HashMap();
    
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
            new NodeConn(this, (NBTTagCompound) tag.getTag("conn_" + i));
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
    }
    

}
