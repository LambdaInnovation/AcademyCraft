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

import java.util.HashSet;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cn.academy.energy.api.IWirelessMatrix;

/**
 * @author WeathFolD
 */
public class WirelessNetwork {
    
    final WiWorldData parent;
    final World world;
    
    //Basic info
    String ssid;
    boolean isEncrypted;
    String password;
    Coord matrix; //Matrix coord.
    
    //Matrix parameters. Copied in the network to prevent unloaded matrix.
    int capacity;
    double latency;
    double range;
    
    //Internal data
    Set<Coord> connectedNodes = new HashSet(); //Connected nodes coordinate set.
    
    public WirelessNetwork(WiWorldData _parent, IWirelessMatrix _mat, String _ssid, boolean isEnc, String pwd) {
        parent = _parent;
        world = _parent.world;
        
        TileEntity te = (TileEntity) _mat;
        
        matrix = new Coord(te, BlockType.MATRIX);
        ssid = _ssid;
        isEncrypted = isEnc;
        password = pwd;
        
        capacity = _mat.getCapacity();
        latency = _mat.getLatency();
        range = _mat.getRange();
    }

    public WirelessNetwork(WiWorldData _parent, NBTTagCompound tag) {
        parent = _parent;
        world = _parent.world;
        load(tag);
    }
    
    void tick() {}
    
    void onDestroyed() {
        for(Coord c : connectedNodes) {
            parent.aliveNetworks.remove(c);
        }
    }
    
    void addNode(Coord node) {
        connectedNodes.add(node);
        parent.aliveNetworks.put(node, this);
    }
    
    void removeNode(Coord node) {
        connectedNodes.remove(node);
        parent.aliveNetworks.remove(node);
    }
    
    private void save(NBTTagCompound tag) {
        tag.setString("ssid", ssid);
        tag.setBoolean("encrypted", isEncrypted);
        if(isEncrypted)
            tag.setString("pass", password);
        matrix.save(tag);
        
        tag.setInteger("capacity", capacity);
        tag.setDouble("latency", latency);
        tag.setDouble("range", range);
        
        tag.setInteger("nodes", connectedNodes.size());
        int i = 0;
        for(Coord c : connectedNodes) {
            NBTTagCompound t2 = new NBTTagCompound();
            c.save(t2);
            tag.setTag("node" + i, t2);
            ++i;
        }
    }
    
    private void load(NBTTagCompound tag) {
        ssid = tag.getString("ssid");
        isEncrypted = tag.getBoolean("encrypted");
        if(isEncrypted)
            password = tag.getString("pass");
        matrix = new Coord(world, tag, BlockType.MATRIX);
        
        capacity = tag.getInteger("capacity");
        latency = tag.getDouble("latency");
        range = tag.getDouble("range");
        
        int n = tag.getInteger("nodes");
        for(int i = 0; i < n; ++i) {
            addNode(new Coord(world, 
                    (NBTTagCompound)tag.getTag("node" + i), BlockType.NODE));
        }
    }
    
    

}
