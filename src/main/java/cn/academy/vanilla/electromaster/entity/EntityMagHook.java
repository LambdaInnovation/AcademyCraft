package cn.academy.vanilla.electromaster.entity;

import cn.academy.vanilla.ModuleVanilla;
import cn.academy.vanilla.electromaster.client.renderer.RendererMagHook;
import cn.lambdalib2.registry.mc.RegEntity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegEntity
public class EntityMagHook extends EntityAdvanced {
    
    {
        Rigidbody rb = new Rigidbody();
        rb.gravity = 0.05;
        addMotionHandler(rb);
        setSize(.5f, .5f);
    }
    
    @SideOnly(Side.CLIENT)
    public static RendererMagHook renderer;
    
    public boolean isHit;
    public int hitSide;
    public int hookX, hookY, hookZ;
    
    boolean doesSetStill;
    
    public EntityMagHook(final EntityPlayer player) {
        super(player.getEntityWorld());
        new Motion3D(player, true).multiplyMotionBy(2).applyToEntity(this);
        
        Rigidbody rb = this.getMotionHandler(Rigidbody.class);
        rb.entitySel = EntitySelectors.exclude(player);
        
        this.regEventHandler(new CollideHandler() {

            @Override
            public void onEvent(CollideEvent event) {
                MovingObjectPosition res = event.result;
                if(res.typeOfHit == MovingObjectType.ENTITY) {
                    if(!(res.entityHit instanceof EntityMagHook) || ((EntityMagHook)res.entityHit).isHit) {
                        if(!(res.entityHit instanceof EntityMagHook))
                            res.entityHit.attackEntityFrom(DamageSource.causePlayerDamage(player), 4);
                        dropAsItem();
                    }
                } else {
                    isHit = true;
                    hitSide = res.sideHit;
                    hookX = res.blockX;
                    hookY = res.blockY;
                    hookZ = res.blockZ;
                    setStill();
                }
            }
            
        });
        this.isAirBorne = true;
        this.onGround = false;
    }
    
    public EntityMagHook(World world) {
        super(world);
        this.isAirBorne = true;
        this.onGround = false;
        this.ignoreFrustumCheck = true;
    }
    
    @Override
    public void entityInit() {
        super.entityInit();
        dataWatcher.addObject(10, Byte.valueOf((byte) 0));
        dataWatcher.addObject(11, Integer.valueOf(0));
        dataWatcher.addObject(12, Integer.valueOf(0));
        dataWatcher.addObject(13, Integer.valueOf(0));
    }
    
    @Override
    public void onUpdate() {
        if(this.doesSetStill) {
            doesSetStill = false;
            realSetStill();
        }
        super.onUpdate();
        sync();
    }
    
    @Override
    public void onCollideWithPlayer(EntityPlayer par1EntityPlayer) {
//        if(!worldObj.isRemote && ticksExisted > 20)
//            this.dropAsItem();
    }
    
    private void sync() {
        //System.out.println("sync " + posX + " " + posY + " " + posZ + " " + worldObj.isRemote + " " + isHit + " " + this);
        if(getEntityWorld().isRemote) {
            boolean lastHit = isHit;
            byte b1 = dataWatcher.getWatchableObjectByte(10);
            isHit = (b1 & 1) != 0;
            hitSide = b1 >> 1;
            hookX = dataWatcher.getWatchableObjectInt(11);
            hookY = dataWatcher.getWatchableObjectInt(12);
            hookZ = dataWatcher.getWatchableObjectInt(13);
            if(!lastHit && isHit) {
                setStill();
            }
        } else {
            byte b1 = (byte) ((isHit ? 1 : 0) | (hitSide << 1));
            dataWatcher.updateObject(10, Byte.valueOf(b1));
            dataWatcher.updateObject(11, Integer.valueOf(hookX));
            dataWatcher.updateObject(12, Integer.valueOf(hookY));
            dataWatcher.updateObject(13, Integer.valueOf(hookZ));
        }
    }
    
    @Override
    public boolean attackEntityFrom(DamageSource ds, float dmg) {
        if(isHit && !getEntityWorld().isRemote && ds.getEntity() instanceof EntityPlayer) {
            dropAsItem();
        }
        return true;
    }
    
    @Override
    public boolean canBeCollidedWith() {
        return this.isHit;
    }
    
    private void dropAsItem() {
        getEntityWorld().spawnEntityInWorld(new EntityItem(getEntityWorld(), posX, posY, posZ, new ItemStack(ModuleVanilla.magHook)));
        setDead();
    }
    
    private void setStill() {
        this.doesSetStill = true;
    }
    
    private void realSetStill() {    
        motionX = motionY = motionZ = 0;
        if(getEntityWorld() != null) {
            //worldObj.playSoundAtEntity(this, "academy:maghook_land", .8f, 1.0f);
        }
        this.setSize(1f, 1f);
        this.removeMotionHandlers();
        this.addMotionHandler(new MotionHandler() {

            @Override
            public void onStart() {}
            
            @Override
            public void onUpdate() {
                preRender();
                if(!getEntityWorld().isRemote) {
                    //Check block consistency
                    if(getEntityWorld().isAirBlock(hookX, hookY, hookZ)) {
                        dropAsItem();
                    }
                }
            }

            @Override
            public String getID() {
                return "huh";
            }
            
        });
        
        
    }
    
    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        tag.setBoolean("isHit", isHit);
        tag.setInteger("hitSide", hitSide);
        tag.setInteger("hookX", hookX);
        tag.setInteger("hookY", hookY);
        tag.setInteger("hookZ", hookZ);
    }
    
    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        isHit = tag.getBoolean("isHit");
        hitSide = tag.getInteger("hitSide");
        hookX = tag.getInteger("hookX");
        hookY = tag.getInteger("hookY");
        hookZ = tag.getInteger("hookZ");
        
        if(isHit) {
            setStill();
        }
    }
    
    public void preRender() {
        if(this.isHit) {
            switch(hitSide) {
            case 0:
                rotationPitch = -90; break;
            case 1:
                rotationPitch = 90; break;
            case 2:
                rotationYaw = 0; rotationPitch = 0; break;
            case 3:
                rotationYaw = 180; rotationPitch = 0; break;
            case 4:
                rotationYaw = -90; rotationPitch = 0; break;
            case 5:
                rotationYaw = 90; rotationPitch = 0; break;
            }
            EnumFacing fd = EnumFacing.getFront(hitSide);
            setPosition(hookX + 0.5 + fd.offsetX * 0.51, 
                    hookY + 0.5 + fd.offsetY * 0.51, 
                    hookZ + 0.5 + fd.offsetZ * 0.51);
        }
    }

}