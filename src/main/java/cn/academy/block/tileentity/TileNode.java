package cn.academy.block.tileentity;

import cn.academy.block.block.BlockNode;
import cn.academy.client.render.block.RenderDynamicBlock;
import cn.academy.energy.api.IFItemManager;
import cn.academy.energy.api.WirelessHelper;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.block.block.BlockNode.NodeType;
import cn.academy.energy.impl.WirelessNet;
import cn.lambdalib2.registry.mc.RegTileEntity;
import cn.lambdalib2.s11n.network.TargetPoints;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * @author WeathFolD
 *
 */
@RegTileEntity
public class TileNode extends TileInventory implements IWirelessNode, IInventory, ITickable  {
    
    static IFItemManager itemManager = IFItemManager.instance;

    static final String MSG_SYNC = "sync";

    protected double energy;
    
    private int updateTicker;

    private String password = "";
    
    /**
     * Client-only flag. Only *roughly* indicates whether the block is linked.
     * Used for just rendering.
     */
    public boolean enabled = false;
    
    public boolean chargingIn = false;
    
    public boolean chargingOut = false;

    private String placerName = "";
    
    public TileNode() {
        super("wireless_node", 2);
    }

    public void setPlacer(EntityPlayer player) {
        placerName = player.getName();
    }

    public String getPlacerName() {
        return placerName;
    }
    
    @Override
    public void update() {
        if(!getWorld().isRemote) {
            ++updateTicker;
            if(updateTicker == 10) {
                updateTicker = 0;

                WirelessNet net = WirelessHelper.getWirelessNet(this);
                enabled = net != null;

                NetworkMessage.sendToAllAround(TargetPoints.convert(this, 20),
                        this, MSG_SYNC,
                        enabled, chargingIn, chargingOut, energy, name, password, placerName);
                rebuildBlockState();
            }
            
            updateChargeIn();
            updateChargeOut();
        }
    }
    
    public void setPassword(String _pass) {
        password = _pass;
    }
    
    private void updateChargeIn() {
        ItemStack stack = this.getStackInSlot(0);
        if(stack != null && itemManager.isSupported(stack)) {
            //Charge into the node.
            double req = Math.min(getBandwidth(), getMaxEnergy() - energy);
            double pull = itemManager.pull(stack, req, false);
            
            chargingIn = pull != 0;
            this.setEnergy(energy + pull);
        } else {
            chargingIn = false;
        }
    }
    
    private void updateChargeOut() {
        ItemStack stack = this.getStackInSlot(1);
        if(stack != null && itemManager.isSupported(stack)) {
            double cur = getEnergy();
            if(cur > 0) {
                cur = Math.min(getBandwidth(), cur);
                double left = itemManager.charge(stack, cur);
                
                chargingOut = left != cur;
                this.setEnergy(getEnergy() - (cur - left));
            }
        } else {
            chargingOut = false;
        }
    }

    private void rebuildBlockState() {
        IBlockState curState = world.getBlockState(pos);
        Block block = curState.getBlock();
        // sometime block and tileentity are mismatch
        if (block instanceof BlockNode) {
            boolean lastConnected = curState.getValue(BlockNode.CONNECTED);
            int lastPct = curState.getValue(BlockNode.ENERGY);

            int pct = (int) Math.min(4, Math.round((4 * getEnergy() / getMaxEnergy())));
            if (pct != lastPct || lastConnected != enabled) {
                world.setBlockState(pos,
                        curState
                                .withProperty(BlockNode.CONNECTED, enabled)
                                .withProperty(BlockNode.ENERGY, pct),
                        0);
            }
        }

    }

    @Override
    public double getMaxEnergy() {
        return getType().maxEnergy;
    }

    @Override
    public double getEnergy() {
        return energy;
    }

    @Override
    public void setEnergy(double value) {
        energy = value;
        rebuildBlockState();
    }

    @Override
    public double getBandwidth() {
        return getType().bandwidth;
    }

    @Override
    public double getRange() {
        return getType().range;
    }
    
    public NodeType getType() {
        return NodeType.values()[getBlockMetadata()];
    }
    
    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        energy = tag.getDouble("energy");
        name = tag.getString("nodeName");
        password = tag.getString("password");
        placerName = tag.getString("placer");
    }
    
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setDouble("energy", energy);
        tag.setString("nodeName", name);
        tag.setString("password", password);
        tag.setString("placer", placerName);
        return tag;
    }

    String name = "Unnamed";
    
    @Override
    public String getNodeName() {
        return name;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setNodeName(String name) {
        this.name = name;
    }

    @Listener(channel=MSG_SYNC, side=Side.CLIENT)
    void hSync(boolean enabled, boolean chargingIn, boolean chargingOut,
               double energy, String name, String pass, String placerName) {
        this.enabled = enabled;
        this.chargingIn = chargingIn;
        this.chargingOut = chargingOut;
        this.energy = energy;
        this.name = name;
        this.password = pass;
        this.placerName = placerName;
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate) {
        return oldState.getBlock() != newSate.getBlock();
    }

    @Override
    public int getCapacity() {
        return getType().capacity;
    }

}