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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import cn.academy.core.AcademyCraft;
import cn.academy.energy.api.block.IWirelessGenerator;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.api.block.IWirelessReceiver;
import cn.academy.energy.internal.VBlocks.VBlock;
import cn.academy.energy.internal.VBlocks.VNGenerator;
import cn.academy.energy.internal.VBlocks.VNNode;
import cn.academy.energy.internal.VBlocks.VNReceiver;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 *
 */
public class NodeConn {

    static final int UPDATE_INTERVAL = 40;
    
    final WiWorldData data;
    final VNNode node;
    
    boolean disposed = false;
    boolean active = false;
    int counter = UPDATE_INTERVAL;
    
    List<VNReceiver> receivers = new LinkedList();
    List<VNGenerator> generators = new LinkedList();
    
    public NodeConn(WiWorldData _data, VNNode _node) {
        data = _data;
        node = _node;
    }
    
    public NodeConn(WiWorldData _data, NBTTagCompound tag) {
        data = _data;
        
        node = new VNNode(tag.getCompoundTag("node"));

        NBTTagList list = (NBTTagList) tag.getTag("receivers");
        for(int i = 0; i < list.tagCount(); ++i) {
            addReceiver(new VNReceiver(list.getCompoundTagAt(i)));
        }
        
        NBTTagList list2 = (NBTTagList) tag.getTag("generators");
        for(int i = 0; i < list2.tagCount(); ++i) {
            addGenerator(new VNGenerator(list2.getCompoundTagAt(i)));
        }
    }
    
    NBTTagCompound toNBT() {
        World world = getWorld();
        
        NBTTagCompound ret = new NBTTagCompound();
        
        NBTTagList list;
        list = new NBTTagList();
        for(VNReceiver r : receivers) {
            if(!r.isLoaded(world) || r.get(world) != null) {
                list.appendTag(r.toNBT());
            }
        }
        ret.setTag("receivers", list);
        
        list = new NBTTagList();
        for(VNGenerator g : generators) {
            if(!g.isLoaded(world) || g.get(world) != null) {
                list.appendTag(g.toNBT());
            }
        }
        ret.setTag("generators", list);
        
        ret.setTag("node", node.toNBT());
        
        return ret;
    }
    
    public void dispose() {
        disposed = true;
    }
    
    public boolean isDisposed() {
        return disposed;
    }
    
    boolean addReceiver(VNReceiver receiver) {
        if(getLoad() >= getCapacity() || !checkRange(receiver))
            return false;
        
        World world = getWorld();
        if(world != null) {
            NodeConn old = data.getNodeConnection(receiver.get(world));
            if(old != null) {
                old.removeReceiver(receiver);
            }
        }
        
        receivers.add(receiver);
        data.nodeLookup.put(receiver, this);
        
        return true;
    }
    
    void removeReceiver(VNReceiver receiver) {
        receivers.remove(receiver);
        data.nodeLookup.remove(receiver);
    }
    
    boolean addGenerator(VNGenerator gen) {
        if(getLoad() >= getCapacity() || !checkRange(gen))
            return false;
        
        World world = getWorld();
        NodeConn old = data.getNodeConnection(gen.get(world));
        if(old != null) {
            old.removeGenerator(gen);
        }
        
        generators.add(gen);
        data.nodeLookup.put(gen, this);
        
        return true;
    }
    
    void removeGenerator(VNGenerator gen) {
        generators.remove(gen);
        data.nodeLookup.remove(gen);
    }
    
    void onAdded(WiWorldData data) {
        data.nodeLookup.put(node, this);
    }
    
    void onCleanup(WiWorldData data) {
        data.nodeLookup.remove(node);
        for(VNGenerator gen : generators)
            data.nodeLookup.remove(gen);
        for(VNReceiver rec : receivers)
            data.nodeLookup.remove(rec);
    }
    
    private boolean checkRange(VBlock<?> block) {
        IWirelessNode inode = node.get(getWorld());
        double range = inode == null ? 1000 : inode.getRange();
        return block.distSq(node) <= range * range;
    }
    
    private void checkIsActive() {
        World world = getWorld();
        
        for(VNGenerator gen : generators) {
            if(gen.isLoaded(world)) {
                active = true;
                return;
            }
        }
        
        for(VNReceiver rec : receivers) {
            if(rec.isLoaded(world)) {
                active = true;
                return;
            }
        }
        
        active = false;
    }
    
    public void tick() {
        World world = getWorld();
        if(!active) {
            --counter;
            if(counter == 0) {
                counter = UPDATE_INTERVAL;
                checkIsActive();
                checkAvailability();
            }
            return;
        }
        
        IWirelessNode iNode = node.get(world);
        if(iNode == null) {
            checkAvailability();
            return;
        }
        
        double transferLeft = iNode.getBandwidth();
        
        {
            Collections.shuffle(generators);
            
            Iterator<VNGenerator> iter = generators.iterator();
            while(transferLeft != 0 && iter.hasNext()) {
                VNGenerator gen = iter.next();
                if(gen.isLoaded(world)) {
                    IWirelessGenerator igen = gen.get(world);
                    if(igen == null) {
                        iter.remove();
                    } else {
                        double cur = iNode.getEnergy();
                        double required = Math.min(transferLeft, 
                            Math.min(igen.getBandwidth(), iNode.getMaxEnergy() - cur));
                        double amt = igen.getProvidedEnergy(required);
                        
                        if(amt > required) {
                            AcademyCraft.log.warn("Energy input overflow for generator " + igen);
                            amt = required;
                        }
                        
                        cur += amt;
                        iNode.setEnergy(cur);
                        transferLeft -= amt;
                    }
                }
            }
        }
        
        transferLeft = iNode.getBandwidth();
        {
            Collections.shuffle(receivers);
            
            Iterator<VNReceiver> iter = receivers.iterator();
            while(transferLeft != 0 && iter.hasNext()) {
                VNReceiver rec = iter.next();
                if(rec.isLoaded(world)) {
                    IWirelessReceiver irec = rec.get(world);
                    if(irec == null) {
                        iter.remove();
                    } else {
                        
                        double cur = iNode.getEnergy();
                        double give = Math.min(cur, Math.min(transferLeft, irec.getBandwidth()));
                        give = Math.min(irec.getRequiredEnergy(), give);
                        
                        give = give - irec.injectEnergy(give);
                        cur -= give;
                        transferLeft -= give;
                        iNode.setEnergy(cur);
                    }
                }
            }
        }
        
        // Kill dummy networks
        if(generators.size() == 0 && receivers.size() == 0) {
            dispose();
        }
    }
    
    private void checkAvailability() {
        World world = getWorld();
        IWirelessNode iNode = node.get(world);
        
        if(iNode == null || (generators.size() == 0 && receivers.size() == 0)) {
            if(node.isLoaded(world)) {
                log(node + " destroyed, destroy NodeConn...");
                dispose();
            }
            return;
        }
    }
    
    public IWirelessNode getNode() {
        return node.get(getWorld());
    }
    
    private World getWorld() {
        return data.world;
    }
    
    public int getLoad() {
        return receivers.size() + generators.size();
    }
    
    public int getCapacity() {
        World world = getWorld();
        IWirelessNode inode = world == null ? null : node.get(getWorld());
        return inode == null ? Integer.MAX_VALUE : inode.getCapacity();
    }
    
    private void log(String str) {
        if(AcademyCraft.DEBUG_MODE)
            AcademyCraft.log.info("[NodeConn] " + str);
    }
    
}
