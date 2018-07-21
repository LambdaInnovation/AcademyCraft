package cn.academy.core.entity;

import cn.academy.core.AcademyCraft;
import cn.academy.core.client.render.RenderEntityBlock;
import cn.lambdalib2.registry.mc.RegEntity;
import cn.lambdalib2.s11n.network.NetworkMessage;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * An entity that renders block.
 * @author WeAthFolD
 */
@RegEntity
@RegEntity.HasRender
public class EntityBlock extends EntityAdvanced {
    
    @SideOnly(Side.CLIENT)
    @RegEntity.Render
    public static RenderEntityBlock renderer;
    
    /**
     * Create an EntityBlock from the block given in the coordinate.
     * DOESN't set the coordinate, do it yourself!
     * @return An setup entity block, or null if convert failed.
     */
    public static EntityBlock convert(World world, int x, int y, int z) {
        EntityBlock ret = new EntityBlock(world);
        if(!ret.setBlock(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z)))
            return null;
        ret.setTileEntity(world.getTileEntity(x, y, z));
        return ret;
    }
    
    private static final int BLOCKID = 4, META = 5;
    
    public Block block;
    public int metadata = 0;
    public TileEntity tileEntity;
    
    // other
    public boolean placeWhenCollide = true;
    
    EntityPlayer player;

    public float yaw, lastYaw, pitch, lastPitch;
    
    /**
     * Server ctor. We use a player parameter to forge the ItemBlock#placeBlockAt function's parameter.
     */
    public EntityBlock(EntityPlayer _player) {
        this(_player.worldObj);
        player = _player;
    }

    public EntityBlock(World world) {
        super(world);
        Rigidbody rb = new Rigidbody();
        rb.accurateCollision = true;
        this.addMotionHandler(rb);
        ignoreFrustumCheck = true;
    }

    // Additional hack for scala...
    public void constructServer(EntityPlayer _player) {
        player = _player;
    }

    // Hack...
    public void setPlaceFromServer(boolean value) {
        placeWhenCollide = value;

        NetworkMessage.sendToAllAround(
                TargetPoints.convert(this, 20),
                this, "spfs", value
        );
    }

    @Listener(channel="spfs", side={Side.CLIENT})
    private void hSpfs(boolean value) {
        placeWhenCollide = value;
    }

    @Override
    public void entityInit() {
        dataWatcher.addObject(BLOCKID, (short) 0);
        dataWatcher.addObject(META, (byte) 0);
        
        setSize(1, 1);
    }
    
    @Override
    public void onUpdate() {
        super.onUpdate();

        lastYaw = yaw;
        lastPitch = pitch;

        if(worldObj.isRemote) {
            block = Block.getBlockById(dataWatcher.getWatchableObjectShort(BLOCKID));
            metadata = dataWatcher.getWatchableObjectByte(META);
        } else {
            if(block != null) {
                dataWatcher.updateObject(BLOCKID, (short) Block.getIdFromBlock(block));
                dataWatcher.updateObject(META, (byte) metadata);
                
                if(tileEntity != null)
                    tileEntity.blockMetadata = metadata;
            }
        }
    }
    
    @Override
    public void onFirstUpdate() {
        this.regEventHandler(new CollideHandler() {

            @Override
            public void onEvent(CollideEvent event) {
                if(placeWhenCollide && !worldObj.isRemote && event.result.typeOfHit == MovingObjectType.BLOCK) {
                    int tx = event.result.blockX,
                            ty = event.result.blockY,
                            tz = event.result.blockZ;

                    int iter = 10;
                    while (iter --> 0) {
                        Block hitblock = worldObj.getBlock(tx, ty, tz);
                        if(!hitblock.isReplaceable(worldObj, tx, ty, tz)) {
                            ForgeDirection dir = ForgeDirection.values()[event.result.sideHit];
                            tx += dir.offsetX;
                            ty += dir.offsetY;
                            tz += dir.offsetZ;
                        } else {
                            ((ItemBlock) Item.getItemFromBlock(block)).placeBlockAt(
                                    new ItemStack(block, 0, metadata), player, worldObj, tx, ty, tz, event.result.sideHit,
                                    tx, ty, tz, metadata);
                            break;
                        }
                    }

                    setDead();
                }
            }

        });
        
        if(!worldObj.isRemote && tileEntity != null) {
            try {
                NBTTagCompound tag = new NBTTagCompound();
                tileEntity.writeToNBT(tag);

                NetworkMessage.sendToDimension(worldObj.provider.dimensionId, this, "sync_te",
                        tileEntity.getClass().getName(), tag);
            } catch(Exception e) {
                AcademyCraft.log.error("Error syncing te", e);
            }
        }
    }
    
    public boolean isAvailable() {
        return block != null;
    }

    public void setBlock(Block _block) {
        block = _block;
    }
    
    public boolean setBlock(Block _block, int _metadata) {
        if(Item.getItemFromBlock(_block) == null)
            return false;
        block = _block;
        metadata = _metadata;
        return true;
    }
    
    public void fromItemStack(ItemStack stack) {
        Block block = Block.getBlockFromItem(stack.getItem());
        int meta = stack.getItemDamage();
        setBlock(block, meta);
        if(block instanceof ITileEntityProvider) {
            TileEntity te = ((ITileEntityProvider)block)
                    .createNewTileEntity(getWorld(), meta);
            setTileEntity(te);
        }
    }
    
    public void setTileEntity(TileEntity _te) {
        tileEntity = _te;
    }
    
    public boolean shouldRender() {
        return block != null;
    }
    
    @Override
    public boolean shouldRenderInPass(int pass) {
        return (block != null && block.getRenderBlockPass() == pass) || 
                (tileEntity != null && tileEntity.shouldRenderInPass(pass));
    }
    
    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        setDead();
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        
    }

    @Listener(channel="sync_te", side=Side.CLIENT)
    private void hSyncTE(String className, NBTTagCompound initTag) {
        try {
            TileEntity te = (TileEntity) Class.forName(className).newInstance();
            te.readFromNBT(initTag);
            te.setWorldObj(worldObj);
            tileEntity = te;
        } catch(Exception e) {
            AcademyCraft.log.error("Unable to sync tileEntity " + className, e);
        }
    }

}
