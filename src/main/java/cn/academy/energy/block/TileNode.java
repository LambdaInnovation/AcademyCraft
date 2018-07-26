package cn.academy.energy.block;

import cn.academy.core.client.render.block.RenderDynamicBlock;
import cn.academy.core.tile.TileInventory;
import cn.academy.energy.api.IFItemManager;
import cn.academy.energy.api.WirelessHelper;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.block.BlockNode.NodeType;
import cn.academy.energy.internal.WirelessNet;
import cn.lambdalib2.s11n.network.TargetPoints;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * @author WeathFolD
 *
 */
@RegTileEntity
@RegTileEntity.HasRender
public class TileNode extends TileInventory implements IWirelessNode, IInventory {
    
    static IFItemManager itemManager = IFItemManager.instance;

    static final String MSG_SYNC = "sync";
    
    @SideOnly(Side.CLIENT)
    @RegTileEntity.Render
    public static RenderDynamicBlock renderer;

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
        placerName = player.getCommandSenderName();
    }

    public String getPlacerName() {
        return placerName;
    }
    
    @Override
    public void updateEntity() {
        if(!getWorldObj().isRemote) {
            ++updateTicker;
            if(updateTicker == 10) {
                updateTicker = 0;

                WirelessNet net = WirelessHelper.getWirelessNet(this);
                enabled = net != null;

                NetworkMessage.sendToAllAround(TargetPoints.convert(this, 20),
                        this, MSG_SYNC,
                        enabled, chargingIn, chargingOut, energy, name, password, placerName);
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
    public int getCapacity() {
        return getType().capacity;
    }

}