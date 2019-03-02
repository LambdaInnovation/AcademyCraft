package cn.academy.block.tileentity;

import cn.academy.energy.api.block.IWirelessReceiver;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.TargetPoints;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraftforge.fml.relauncher.Side;

/**
 * BaseClass that should be used on all energy receivers.
 * This class will automatically sync its energy field to client side.
 * @author WeAthFolD
 */
public class TileReceiverBase extends TileInventory implements IWirelessReceiver, ITickable {
    
    private static final int UPDATE_WAIT = 20;

    int updateTicker = 0;
    
    final double maxEnergy;
    final double bandwidth;
    
    public double energy;

    public TileReceiverBase(String name, int invSize, double max, double bwidth) {
        super(name, invSize);
        maxEnergy = max;
        bandwidth = bwidth;
    }
    
    public void update() {
        if(!getWorld().isRemote) {
            if(++updateTicker == UPDATE_WAIT) {
                updateTicker = 0;
                NetworkMessage.sendToAllAround(TargetPoints.convert(this, 15), this, ".tile.sync", writeToNBT(new NBTTagCompound()));
            }
        }
    }

//    @Override
//    public void onLoad()
//    {
//        super.onLoad();
//        if(!world.isRemote)
//        {
//            NetworkMessage.sendToAllAround(TargetPoints.convert(this, 25),
//                    this, ".tile.sync", writeToNBT(new NBTTagCompound()));
//        }
//    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(super.getUpdateTag());
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        super.handleUpdateTag(tag);
        readFromNBT(tag);
    }
    
    @Override
    public double getRequiredEnergy() {
        return maxEnergy - energy;
    }

    @Override
    public double injectEnergy(double amt) {
        double req = maxEnergy - energy;
        double give = Math.min(amt, req);
        energy += give;
        return amt - give;
    }
    
    public double getEnergy() {
        return energy;
    }
    
    public double getMaxEnergy() {
        return maxEnergy;
    }

    @Override
    public double getBandwidth() {
        return bandwidth;
    }
    
    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        energy = tag.getDouble("energy");
    }
    
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setDouble("energy", energy);
        return tag;
    }

    @Listener(channel=".tile.sync", side=Side.CLIENT)
    private void hSync(NBTTagCompound nbt) {
        readFromNBT(nbt);
        IBlockState ibs = world.getBlockState(pos);
        world.markBlockRangeForRenderUpdate(pos, pos);
        world.notifyBlockUpdate(pos, ibs, ibs, 3);
        world.scheduleBlockUpdate(pos,this.getBlockType(),0,0);
    }

    @Override
    public double pullEnergy(double amt) {
        double a = Math.min(amt, energy);
        energy -= a;
        return a;
    }

}