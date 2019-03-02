package cn.academy.block.tileentity;

import cn.academy.AcademyCraft;
import cn.academy.ability.client.ui.DeveloperUI;
import cn.academy.ability.develop.DeveloperType;
import cn.academy.ability.develop.IDeveloper;
import cn.academy.block.block.BlockDeveloper;
import cn.lambdalib2.multiblock.BlockMulti;
import cn.lambdalib2.multiblock.IMultiTile;
import cn.lambdalib2.multiblock.InfoBlockMulti;
import cn.lambdalib2.registry.mc.RegTileEntity;
import cn.lambdalib2.s11n.network.TargetPoints;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import com.google.common.base.Preconditions;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/**
 * @author WeAthFolD
 *
 */
public abstract class TileDeveloper extends TileReceiverBase implements IMultiTile, IDeveloper {

    private static final String
        MSG_OPEN_GUI = "gui",
        MSG_UNUSE = "unuse",
        MSG_SYNC  = "sync";

    @RegTileEntity
    public static class Normal extends TileDeveloper {
        public Normal() {
            super(DeveloperType.NORMAL);
        }
    }

    @RegTileEntity
    public static class Advanced extends TileDeveloper {
        public Advanced() {
            super(DeveloperType.ADVANCED);
        }
    }

    public final DeveloperType type;
    
    private int syncCD;
    
    private EntityPlayer user;
    
    private TileDeveloper(DeveloperType _type) {
        super("ability_developer", 2, _type.getEnergy(), _type.getBandwidth());
        type = _type;
    }

    @Override
    public void update() {
        if(info != null) {
            info.update();
            if(info.getSubID() != 0)
                return;
            
            super.update();
            
            if(++syncCD == 20) {
                syncCD = 0;
                send(MSG_SYNC, user);
            }
        }
    }
    
    public EntityPlayer getUser() {
        return user;
    }
    
    /**
     * SERVER only. Start let the player use the developer, if currently no user is using it.
     */
    public boolean use(EntityPlayer player) {
        Preconditions.checkState(!player.world.isRemote);

        if(info.getSubID() != 0) {
            TileDeveloper te = getOrigin();
            return te == null ? false : te.use(player);
        }

        if (user != null) {
            unuse();
        }

        user = player;
        send(MSG_OPEN_GUI, player);
        return true;
    }
    
    private TileDeveloper getOrigin() {
        BlockDeveloper dev = (BlockDeveloper) getBlockType();
        TileEntity te = dev.getOriginTile(this);
        
        return te instanceof TileDeveloper ? (TileDeveloper) te : null;
    }
    
    /**
     * Is effective in BOTH CLIENT AND SERVER. Let the current player(if is equal to argument) go away from the developer.
     */
    public void unuse(EntityPlayer p) {
        if(info.getSubID() != 0) {
            TileDeveloper te = getOrigin();
            if(te != null) {
                te.unuse(p);
            }
        } else {
            if(getWorld().isRemote) {
                send(MSG_UNUSE, p);
            } else {
                if(user != null && user.equals(p))
                    unuse();
            }
        }
    }
    
    private void unuse() {
        user = null;
    }

    @Override
    public boolean tryPullEnergy(double amount) {
        if(energy < amount)
            return false;
        pullEnergy(amount);
        return true;
    }

    @Override
    public void onGuiClosed() {
        send(MSG_UNUSE, user);
    }

    private void send(String channel, Object ...args) {
        if (getWorld().isRemote) {
            NetworkMessage.sendToServer(this, channel, args);
        } else {
            NetworkMessage.sendToAllAround(TargetPoints.convert(this, 10), this, channel, args);
        }
    }
    
    public final DeveloperType getType() {
        return type;
    }

    @SideOnly(Side.CLIENT)
    @Listener(channel=MSG_OPEN_GUI, side=Side.CLIENT)
    private void hOpenGui(EntityPlayer player) {
        // Sync the player right away to prevent bad lookup
        this.user = player;

        if (Minecraft.getMinecraft().player.equals(player)) {
            Minecraft.getMinecraft().displayGuiScreen(DeveloperUI.apply(this));
        }
    }

    @Listener(channel=MSG_UNUSE, side=Side.SERVER)
    private void hUnuse(EntityPlayer player) {
        unuse(player);
    }

    @Listener(channel=MSG_SYNC, side=Side.CLIENT)
    private void hSync(EntityPlayer player) {
        this.user = player;
    }
    
    private InfoBlockMulti info = new InfoBlockMulti(this);
    
    @Override
    public InfoBlockMulti getBlockInfo() {
        return info;
    }

    @Override
    public void setBlockInfo(InfoBlockMulti i) {
        info = i;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        info = new InfoBlockMulti(this, nbt);
    }
    
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        info.save(nbt);
        return nbt;
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        Block block = getBlockType();
        if(block instanceof BlockMulti) {
            return ((BlockMulti) block).getRenderBB(getPos(), info.getDir());
        } else {
            return super.getRenderBoundingBox();
        }
    }
    
}