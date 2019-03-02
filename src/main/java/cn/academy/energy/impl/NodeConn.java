package cn.academy.energy.impl;

import cn.academy.AcademyCraft;
import cn.academy.energy.api.block.IWirelessGenerator;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.api.block.IWirelessReceiver;
import cn.academy.energy.impl.VBlocks.VBlock;
import cn.academy.energy.impl.VBlocks.VNGenerator;
import cn.academy.energy.impl.VBlocks.VNNode;
import cn.academy.energy.impl.VBlocks.VNReceiver;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

import java.util.*;

/**
 * @author WeAthFolD
 *
 */
public class NodeConn {

    private static final int UPDATE_INTERVAL = 40;

    private final WiWorldData data;
    private final VNNode node;

    private boolean disposed = false;
    
    private List<VNReceiver> receivers = new LinkedList<>();
    private List<VNGenerator> generators = new LinkedList<>();

    private List<VNReceiver> toRemoveReceivers = new ArrayList<>();
    private List<VNGenerator> toRemoveGenerators = new ArrayList<>();
    
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
        toRemoveReceivers.add(receiver);
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
        toRemoveGenerators.add(gen);
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

    boolean validate() {
        World world = getWorld();
        if (!disposed && node.isLoaded(world)) {
            if (node.get(world) == null || (generators.size() == 0 && receivers.size() == 0)) {
                disposed = true;
            }
        }

        return !disposed;
    }

    private boolean checkRange(VBlock<?> block) {
        IWirelessNode inode = node.get(getWorld());
        double range = inode == null ? 1000 : inode.getRange();
        return block.distSq(node) <= range * range;
    }
    
    public void tick() {
        validate();

        World world = getWorld();
        if (node.isLoaded(world)) {
            IWirelessNode iNode = node.get(world);
            if(iNode == null) {
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
                            removeGenerator(gen);
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
                            removeReceiver(rec);
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

            // Remove toRemove receivers/generators
            data.nodeLookup.keySet().removeAll(toRemoveGenerators);
            generators.removeAll(toRemoveGenerators);

            data.nodeLookup.keySet().removeAll(toRemoveReceivers);
            receivers.removeAll(toRemoveReceivers);

            toRemoveGenerators.clear();
            toRemoveReceivers.clear();
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