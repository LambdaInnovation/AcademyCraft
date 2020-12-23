package cn.academy.energy.impl;

import cn.academy.AcademyCraft;
import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.impl.VBlocks.VWMatrix;
import cn.academy.energy.impl.VBlocks.VWNode;
import cn.lambdalib2.util.MathUtils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

//import cn.lambdalib2.util.MathUtils;

/**
 * @author WeAthFolD
 */
public class WirelessNet {

    private static final int UPDATE_INTERVAL = 40;
    private static final double BUFFER_MAX = 2000;

    private final WiWorldData data;
    World world;

    private List<VWNode> nodes = new LinkedList<>();

    private List<VWNode> toRemoveNodes = new ArrayList<>();

    private VWMatrix matrix;

    private String ssid;
    private String password;

    private double buffer;

    private int aliveUpdateCounter = UPDATE_INTERVAL;

    private boolean disposed = false;

    WirelessNet(WiWorldData data, VWMatrix matrix, String ssid, String pass) {
        this.data = data;

        this.matrix = matrix;

        this.ssid = ssid;
        this.password = pass;
    }

    WirelessNet(WiWorldData data, NBTTagCompound tag) {
        this.data = data;

        //Load the matrix
        matrix = new VWMatrix(tag.getCompoundTag("matrix"));

        //Load the info
        ssid = tag.getString("ssid");
        password = tag.getString("password");
        buffer = tag.getDouble("buffer");

        //Load the node list
        NBTTagList list = (NBTTagList) tag.getTag("list");
        for (int i = 0; i < list.tagCount(); ++i) {
            doAddNode(new VWNode(list.getCompoundTagAt(i)));
        }

        debug("Loading " + ssid + " from NBT, " + list.tagCount() + " nodes.");
    }

    NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("matrix", matrix.toNBT());
        tag.setString("ssid", ssid);
        tag.setString("password", password);
        tag.setDouble("buffer", buffer);

        NBTTagList list = new NBTTagList();
        for (VWNode vn : nodes) {
            if (!vn.isLoaded(world) || vn.get(world) != null) {
                list.appendTag(vn.toNBT());
            }
        }
        tag.setTag("list", list);

        debug(ssid + " toNBT()");

        return tag;
    }

    public String getSSID() {
        return ssid;
    }

    public void setSSID(String ssid) {
        this.ssid = ssid;
    }

    public String getPassword() {
        return password;
    }

    public boolean resetPassword(String np) {
        password = np;
        return true;
    }

    public boolean isDisposed() {
        return disposed;
    }

    public int getLoad() {
        return nodes.size();
    }

    public int getCapacity() {
        World world = data.world;
        IWirelessMatrix imat = matrix.get(world);
        return imat == null ? 0 : imat.getCapacity();
    }

    public IWirelessMatrix getMatrix() {
        return matrix.get(world);
    }

    /**
     * Dispose (a.k.a. destroy) this network and unlink all its linked nodes.
     */
    void dispose() {
        disposed = true;
    }

    boolean addNode(VWNode node, String password) {
        if (!password.equals(this.password))
            return false;
        if (getLoad() >= getCapacity())
            return false;

        IWirelessMatrix imat = matrix.get(world);
        if (imat == null) {
            return false;
        }

        double r = imat.getRange();
        if (node.distSq(matrix) > r * r)
            return false;

        WiWorldData data = getWorldData();

        //Check if this node is previously added
        WirelessNet other = data.getNetwork(node.get(world));
        if (other != null) {
            other.removeNode(node);
        }

        doAddNode(node);

        return true;
    }

    boolean validate() {
        if (matrix.isLoaded(world)) {
            IWirelessMatrix mat = matrix.get(world);
            if (mat == null) {
                disposed = true;
            }
        }

        return !disposed;
    }

    boolean isInRange(int x, int y, int z) {
        IWirelessMatrix imat = matrix.get(world);
        if (imat == null) {
            return false;
        }

        double r = imat.getRange();
        return MathUtils.distanceSq(x, y, z, matrix.x, matrix.y, matrix.z) <= r * r;
    }

    private void doAddNode(VWNode node) {
        //Really add
        WiWorldData data = getWorldData();
        nodes.add(node);
        data.netLookup.put(node, this);
    }

    void removeNode(VWNode node) {
        debug("Removing " + node + " from " + ssid);
        toRemoveNodes.add(node);
    }

    void onCreate(WiWorldData data) {
        data.netLookup.put(matrix, this);
    }

    void onCleanup(WiWorldData data) {
        data.netLookup.remove(ssid);
        data.netLookup.remove(matrix);

        for (VWNode n : nodes) {
            data.netLookup.remove(n);
        }
    }

    private WiWorldData getWorldData() {
        return data;
    }

    void tick() {
        validate();

        if (matrix.isLoaded(world)) {
            // Check whether the matrix is valid. The matrix is ALWAYS loaded.
            IWirelessMatrix imat = matrix.get(world);
            if (imat == null) {
                debug("WirelessNet with SSID " + ssid + " matrix destoryed, removing");
                dispose();
            } else {
                // Balance.
                // Shuffle in order to not balance one node all the time
                // Maybe a bit of slow?
                Collections.shuffle(nodes);

                double sum = 0, maxSum = 0;
                for (VWNode vn : nodes) {
                    if (vn.isLoaded(world)) {
                        IWirelessNode node = vn.get(world);
                        if (node == null) {
                            removeNode(vn);
                        } else {
                            sum += node.getEnergy();
                            maxSum += node.getMaxEnergy();
                        }
                    }
                }

                // Remove nodes
                data.netLookup.keySet().removeAll(toRemoveNodes);
                nodes.removeAll(toRemoveNodes);
                toRemoveNodes.clear();

                double percent = sum / maxSum;
                double transferLeft = imat.getBandwidth();
                // Loop through and calc
                for (VWNode vn : nodes) {
                    if (vn.isLoaded(world)) {
                        IWirelessNode node = vn.get(world);

                        double cur = node.getEnergy();
                        double targ = node.getMaxEnergy() * percent;

                        double delta = targ - cur;
                        delta = Math.signum(delta) * Math.min(Math.abs(delta), Math.min(transferLeft, node.getBandwidth()));

                        if (buffer + delta > BUFFER_MAX) {
                            delta = BUFFER_MAX - buffer;
                        } else if (buffer + delta < 0) {
                            delta = -buffer;
                        }

                        transferLeft -= Math.abs(delta);
                        buffer += delta;
                        node.setEnergy(cur + delta);

                        if (transferLeft == 0)
                            break;
                    }
                }
            }
        }
    }

    private void debug(Object msg) {
        if (AcademyCraft.DEBUG_MODE)
            AcademyCraft.log.info("WN:" + msg);
    }

}