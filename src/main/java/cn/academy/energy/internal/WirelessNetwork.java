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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cn.academy.core.AcademyCraft;
import cn.academy.energy.api.IWirelessMatrix;
import cn.academy.energy.api.IWirelessNode;
import cn.academy.energy.internal.WiWorldData.ChunkCoord;
import cn.liutils.util.GenericUtils;

/**
 * @author WeathFolD
 */
class WirelessNetwork {
    
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
    final double MAX_LAG = 0; //abs value.
    double energyLag = MAX_LAG / 2; //The "invisible buffer" used when balancing energy.
    boolean dead; //Death flag.
    
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
    
    void tick() {
    	//Check if matrix is still present
    	if(matrix.isLoaded() && matrix.getAndCheck() == null) {
    		dead = true;
    		AcademyCraft.log.info("Matrix not present for ssid " + ssid + ", killing the network.");
    		return;
    	}
    	
    	//AcademyCraft.log.info("Ticking network " + ssid + ".");
    	
        double totalEnergy = 0.0, totalMax = 0.0;
        Iterator<Coord> iter = connectedNodes.iterator();
        //Pass 1: Calculate average energy pct
        while(iter.hasNext()) {
            Coord c = iter.next();
            if(!c.isLoaded())
                continue;
            TileEntity te = c.getAndCheck();
            if(te == null) {
                iter.remove();
                continue;
            }
            IWirelessNode node = (IWirelessNode) te;
            totalEnergy += node.getEnergy();
            totalMax += node.getMaxEnergy();
        }
        totalMax += MAX_LAG;
        totalEnergy += energyLag;
        
        //Pass 2: Balance max up to latency.
        double alpha = Math.min(1, totalEnergy / totalMax);
        double totalChanged = 0.0;
        for(Coord c : connectedNodes) {
            if(!c.isLoaded())
                continue;
            IWirelessNode node = (IWirelessNode) c.getAndCheck();
            double targ = alpha * node.getEnergy();
            double delta = targ - node.getEnergy();
            delta = Math.signum(delta) * GenericUtils.min(latency - totalChanged, Math.abs(delta), node.getLatency());
            delta = queryChange(delta);
            totalChanged += Math.abs(delta);
            node.setEnergy(delta + node.getEnergy());
            if(totalChanged <= 0)
                break;
        }
    }
    
    private double queryChange(double c) {
        if(c > 0) {
            double give = Math.min(energyLag, c);
            energyLag -= give;
            return give;
        } else {
            double give = Math.min(MAX_LAG - energyLag, -c);
            energyLag += give;
            return -give;
        }
    }
    
    void onDestroyed() {
        for(Coord c : connectedNodes) {
            parent.aliveNetworks.remove(c);
            parent.lazyNetLookup(new ChunkCoord(c)).remove(c.getAndCheck());
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
    
    void save(NBTTagCompound tag) {
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
    
    void load(NBTTagCompound tag) {
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
    
    void setDead() {
        dead = true;
    }

}
