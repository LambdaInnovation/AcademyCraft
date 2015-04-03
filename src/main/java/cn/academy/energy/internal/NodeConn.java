package cn.academy.energy.internal;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cn.academy.energy.api.IWirelessGenerator;
import cn.academy.energy.api.IWirelessNode;
import cn.academy.energy.api.IWirelessReceiver;

class NodeConn {
    
    final WiWorldData parent;
    final World world;

    /**
     * Flag indicating whether this connection is dead.
     */
    boolean dead;

    Coord node;
    Set<Coord> receivers = new HashSet(), generators = new HashSet();

    public NodeConn(WiWorldData _data, IWirelessNode _node) {
        parent = _data;
        world = _data.world;
        TileEntity te = (TileEntity) _node;
        node = new Coord(te, BlockType.NODE);
    }

    /**
     * Load a NodeConn from the NBT. Will automatically update the lookup table.
     */
    public NodeConn(WiWorldData _data, NBTTagCompound tag) {
        parent = _data;
        world = _data.world;
        load(tag);
    }

    void save(NBTTagCompound tag) {
        node.save(tag);
        tag.setInteger("receivers", receivers.size());
        int i = 0;
        for (Coord c : receivers) {
            NBTTagCompound tag2 = new NBTTagCompound();
            tag.setTag("rec" + i, tag2);
            c.save(tag2);
            ++i;
        }

        tag.setInteger("generators", generators.size());
        i = 0;
        for (Coord c : generators) {
            NBTTagCompound tag2 = new NBTTagCompound();
            tag.setTag("gen" + i, tag2);
            c.save(tag2);
            ++i;
        }
    }

    void load(NBTTagCompound tag) {
        node = new Coord(world, tag, BlockType.NODE);
        
        int n = tag.getInteger("receivers");
        for (int i = 0; i < n; ++i) {
            Coord c = new Coord(world, (NBTTagCompound) tag.getTag("rec" + i),
                    BlockType.RECEIVER);
            receivers.add(c);
            parent.nodeConns.put(c, this); // Update the lookup
        }

        n = tag.getInteger("generators");
        for (int i = 0; i < n; ++i) {
            Coord c = new Coord(world, (NBTTagCompound) tag.getTag("gen" + i),
                    BlockType.GENERATOR);
            generators.add(c);
            parent.nodeConns.put(c, this); // Update the lookup
        }
    }

    void onRemoved() {
        
    }
    
    public void addReceiver(Coord rec) {
        receivers.add(rec);
    }

    public void addGenerator(Coord gen) {
        generators.add(gen);
    }
    
    public void unlinkReceiver(Coord rec) {
        receivers.remove(rec);
        parent.nodeConns.remove(rec);
    }
    
    public void unlinkGenerator(Coord gen) {
        generators.remove(gen);
        parent.nodeConns.remove(gen);
    }

    public void tick() {
        // Validate and balance
        TileEntity tnode = node.getAndCheck();
        if (tnode == null) {
            setDead();
            return;
        }
        IWirelessNode inode = (IWirelessNode) tnode;

        // validate&calc generators
        Iterator<Coord> iter = generators.iterator();
        while (iter.hasNext()) {
            Coord c = iter.next();
            if(!c.isLoaded())
                continue;
            TileEntity te = c.getAndCheck();
            if (te == null) {
                iter.remove();
                continue;
            }
            IWirelessGenerator igen = (IWirelessGenerator) te;
            // calc
            double req = Math.min(
                    igen.getLatency(),
                    Math.min(inode.getLatency(),
                            inode.getMaxEnergy() - inode.getEnergy()));
            double gen = igen.getProvidedEnergy(req);
            inode.setEnergy(inode.getEnergy() + gen);
        }

        // validate&calc receivers
        iter = receivers.iterator();
        while (iter.hasNext()) {
            Coord c = iter.next();
            if(!c.isLoaded())
                continue;
            TileEntity te = c.getAndCheck();
            if (te == null) {
                iter.remove();
                continue;
            }
            //calc
            IWirelessReceiver irec = (IWirelessReceiver) te;
            double req = Math.min(irec.getLatency(), irec.getRequiredEnergy());
            double give = Math.min(req, inode.getEnergy());
            irec.injectEnergy(give);
        }
    }

    private void setDead() {
        dead = true;
    }

}