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
package cn.academy.core.entity;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import cn.academy.core.AcademyCraft;
import cn.academy.core.client.render.RenderEntityBlock;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegEntity;
import cn.lambdalib.networkcall.RegNetworkCall;
import cn.lambdalib.networkcall.s11n.StorageOption;
import cn.lambdalib.networkcall.s11n.StorageOption.Data;
import cn.lambdalib.util.entityx.EntityAdvanced;
import cn.lambdalib.util.entityx.event.CollideEvent;
import cn.lambdalib.util.entityx.event.CollideEvent.CollideHandler;
import cn.lambdalib.util.entityx.handlers.Rigidbody;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * An entity that renders block.
 * @author WeAthFolD
 */
@Registrant
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
    
    static final int BLOCKID = 4, META = 5;
    
    public Block block;
    public int metadata = 0;
    public TileEntity tileEntity;
    
    // other
    public boolean placeWhenCollide = true;
    
    EntityPlayer player;
    
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
    
    @Override
    public void entityInit() {
        dataWatcher.addObject(BLOCKID, Short.valueOf((short) 0));
        dataWatcher.addObject(META, Byte.valueOf((byte) 0));
        
        setSize(1, 1);
    }
    
    @Override
    public void onUpdate() {
        super.onUpdate();
        if(worldObj.isRemote) {
            block = Block.getBlockById(dataWatcher.getWatchableObjectShort(BLOCKID));
            metadata = dataWatcher.getWatchableObjectByte(META);
        } else {
            if(block != null) {
                dataWatcher.updateObject(BLOCKID, Short.valueOf((short) Block.getIdFromBlock(block)));
                dataWatcher.updateObject(META, Byte.valueOf((byte) metadata));
                
                if(tileEntity != null)
                    tileEntity.blockMetadata = metadata;
            }
        }
    }
    
    @Override
    public void onFirstUpdate() {
        if(placeWhenCollide) {
            this.regEventHandler(new CollideHandler() {

                @Override
                public void onEvent(CollideEvent event) {
                    if(!worldObj.isRemote && event.result.typeOfHit == MovingObjectType.BLOCK) {
                        int tx = event.result.blockX,
                                ty = event.result.blockY,
                                tz = event.result.blockZ;
                        Block hitblock = worldObj.getBlock(tx, ty, tz);
                        if(!hitblock.isReplaceable(worldObj, tx, ty, tz)) {
                            ForgeDirection dir = ForgeDirection.values()[event.result.sideHit];
                            tx += dir.offsetX;
                            ty += dir.offsetY;
                            tz += dir.offsetZ;
                        }
                        
                        Vec3 vec = event.result.hitVec;
                        ((ItemBlock) Item.getItemFromBlock(block)).placeBlockAt(
                            new ItemStack(block, 0, metadata), player, worldObj, tx, ty, tz, event.result.sideHit, 
                            (float) vec.xCoord, (float) vec.yCoord, (float) vec.zCoord, metadata);
                        
                        setDead();
                    }
                }
                
            });
        }
        
        if(!worldObj.isRemote && tileEntity != null) {
            try {
                NBTTagCompound tag = new NBTTagCompound();
                tileEntity.writeToNBT(tag);
                
                syncTileEntity(tileEntity.getClass().getName(), tag);
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
                    .createNewTileEntity(worldObj, meta);
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
    
    @RegNetworkCall(side = Side.CLIENT, thisStorage = StorageOption.Option.INSTANCE)
    private void syncTileEntity(@Data String klassName, @Data NBTTagCompound initTag) {
        try {
            TileEntity te = (TileEntity) Class.forName(klassName).newInstance();
            te.readFromNBT(initTag);
            te.setWorldObj(worldObj);
            tileEntity = te;
        } catch(Exception e) {
            AcademyCraft.log.error("Unable to sync tileEntity " + klassName, e);
        }
    }

}
