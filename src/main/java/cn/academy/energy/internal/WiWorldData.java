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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cn.academy.core.AcademyCraft;
import cn.academy.energy.api.IWirelessGenerator;
import cn.academy.energy.api.IWirelessNode;

/**
 * @author WeathFolD
 *
 */
public class WiWorldData {
    
    final World world;
    
    public WiWorldData(World _world) {
        world = _world;
    }
    
    void tick() {
        
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
     * -Coord of node
     * -Coord of generator/receiver
     */
    Map<Coord, NodeConn> nodeConns = new HashMap();
    
    public void linkGenerator(Coord gen, Coord node) {
        NodeConn conn = nodeConns.get(node);
        if((conn = nodeConns.get(node)) == null) {
            AcademyCraft.log.error("Try to link to a node that doesn't exist.");
            return;
        }
        conn.addGenerator(gen);
    }
    
    public void linkReceiver(Coord rec, Coord node) {
        NodeConn conn = nodeConns.get(node);
        if((conn = nodeConns.get(node)) == null) {
            AcademyCraft.log.error("Try to link to a node that doesn't exist.");
            return;
        }
        conn.addReceiver(rec);
    }
    
    private class NodeConn {
        
        Coord node;
        Set<Coord> 
            receivers = new HashSet(), 
            generators = new HashSet();
        
        public NodeConn(IWirelessNode _node) {
            TileEntity te = (TileEntity) _node;
            node = new Coord(te, BlockType.NODE);
        }
        
        public NodeConn(NBTTagCompound tag) {
            load(tag);
        }
        
        public void save(NBTTagCompound tag) {
            node.save(tag);
            tag.setInteger("receivers", receivers.size());
            int i = 0;
            for(Coord c : receivers) {
                NBTTagCompound tag2 = new NBTTagCompound();
                tag.setTag("rec" + i, tag2);
                c.save(tag2);
                ++i;
            }
            
            tag.setInteger("generators", generators.size());
            i = 0;
            for(Coord c : generators) {
                NBTTagCompound tag2 = new NBTTagCompound();
                tag.setTag("gen" + i, tag2);
                c.save(tag2);
                ++i;
            }
        }
        
        public void load(NBTTagCompound tag) {
            node = new Coord(world, tag, BlockType.NODE);
            int n = tag.getInteger("receivers");
            for(int i = 0; i < n; ++i) {
                Coord c = new Coord(world, 
                        (NBTTagCompound) tag.getTag("rec" + i), BlockType.RECEIVER);
                receivers.add(c);
                nodeConns.put(c, this); //Update the lookup
            }
            
            n = tag.getInteger("generators");
            for(int i = 0; i < n; ++i) {
                Coord c = new Coord(world, 
                        (NBTTagCompound) tag.getTag("gen" + i), BlockType.GENERATOR);
                generators.add(c);
                nodeConns.put(c, this); //Update the lookup
            }
        }
        
        public void addReceiver(Coord rec) {
            receivers.add(rec);
        }
        
        public void addGenerator(Coord gen) {
            generators.add(gen);
        }
        
        public void tick() {
            //Validate and balance
        }
        
    }
}
