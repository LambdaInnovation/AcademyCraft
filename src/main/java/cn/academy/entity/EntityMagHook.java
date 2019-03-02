package cn.academy.entity;

import cn.academy.ACItems;
import cn.academy.AcademyCraft;
import cn.academy.Resources;
import cn.academy.ability.vanilla.VanillaCategories;
import cn.academy.client.render.entity.RendererMagHook;
import cn.academy.client.sound.ACSounds;
import cn.lambdalib2.registry.mc.RegEntity;
import cn.lambdalib2.util.EntitySelectors;
import cn.lambdalib2.util.entityx.EntityAdvanced;
import cn.lambdalib2.util.entityx.MotionHandler;
import cn.lambdalib2.util.entityx.event.CollideEvent;
import cn.lambdalib2.util.entityx.handlers.Rigidbody;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegEntity
public class EntityMagHook extends EntityAdvanced
{

    private static final DataParameter<Boolean> IS_HIT = EntityDataManager.<Boolean>createKey(EntityMagHook.class, DataSerializers.BOOLEAN);
    private static final DataParameter<EnumFacing> HIT_SIDE = EntityDataManager.<EnumFacing>createKey(EntityMagHook.class, DataSerializers.FACING);
    private static final DataParameter<Integer> HOOK_X = EntityDataManager.<Integer>createKey(EntityMagHook.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> HOOK_Y = EntityDataManager.<Integer>createKey(EntityMagHook.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> HOOK_Z = EntityDataManager.<Integer>createKey(EntityMagHook.class, DataSerializers.VARINT);
    {
        Rigidbody rb = new Rigidbody();
        rb.gravity = 0.05;
        addMotionHandler(rb);
        setSize(.5f, .5f);
    }

    public boolean isHit;
    public EnumFacing hitSide = EnumFacing.DOWN;
    public int hookX, hookY, hookZ;
    
    boolean doesSetStill;
    
    public EntityMagHook(final EntityPlayer player) {
        super(player.getEntityWorld());
        setPosition(player.posX, player.posY + player.eyeHeight, player.posZ);
        Vec3d look = player.getLookVec();
        motionX = look.x * 2;
        motionY = look.y * 2;
        motionZ = look.z * 2;

        setRotation(player.rotationYaw, player.rotationPitch);
        
        Rigidbody rb = this.getMotionHandler(Rigidbody.class);
        rb.entitySel = EntitySelectors.exclude(player);
        
        this.regEventHandler(new CollideEvent.CollideHandler() {

            @Override
            public void onEvent(CollideEvent event) {
                RayTraceResult res = event.result;
                if(res.typeOfHit == RayTraceResult.Type.ENTITY) {
                    if(!(res.entityHit instanceof EntityMagHook) || ((EntityMagHook)res.entityHit).isHit) {
                        if(!(res.entityHit instanceof EntityMagHook))
                            res.entityHit.attackEntityFrom(DamageSource.causePlayerDamage(player), 4);
                        dropAsItem();
                    }
                } else {
                    isHit = true;
                    hitSide = res.sideHit;
                    hookX = res.getBlockPos().getX();
                    hookY = res.getBlockPos().getY();
                    hookZ = res.getBlockPos().getZ();
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
        dataManager.register(HIT_SIDE, EnumFacing.DOWN);
        dataManager.register(IS_HIT, false);
        dataManager.register(HOOK_X, 0);
        dataManager.register(HOOK_Y, 0);
        dataManager.register(HOOK_Z, 0);
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
//        if(!world.isRemote && ticksExisted > 20)
//            this.dropAsItem();
    }
    
    private void sync() {
        //System.out.println("sync " + posX + " " + posY + " " + posZ + " " + world.isRemote + " " + isHit + " " + this);
        if(getEntityWorld().isRemote) {
            boolean lastHit = isHit;

            hitSide = dataManager.get(HIT_SIDE);
            isHit = dataManager.get(IS_HIT);

            hookX = dataManager.get(HOOK_X);
            hookY = dataManager.get(HOOK_Y);
            hookZ = dataManager.get(HOOK_Z);
            if(!lastHit && isHit) {
                setStill();
            }
        } else {
            dataManager.set(HIT_SIDE, hitSide);
            dataManager.set(IS_HIT, isHit);

            dataManager.set(HOOK_X, hookX);
            dataManager.set(HOOK_Y, hookY);
            dataManager.set(HOOK_Z, hookZ);
        }
    }
    
    @Override
    public boolean attackEntityFrom(DamageSource ds, float dmg) {
        if(isHit && !getEntityWorld().isRemote && ds.getTrueSource() instanceof EntityPlayer) {
            dropAsItem();
        }
        return true;
    }
    
    @Override
    public boolean canBeCollidedWith() {
        return this.isHit;
    }
    
    private void dropAsItem() {
        getEntityWorld().spawnEntity(new EntityItem(getEntityWorld(), posX, posY, posZ, new ItemStack(ACItems.mag_hook)));
        setDead();
    }
    
    private void setStill() {
        this.doesSetStill = true;
    }
    
    private void realSetStill() {    
        motionX = motionY = motionZ = 0;
        if(getEntityWorld() != null) {
            this.playSound(Resources.sound("maghook_land"), .8f, 1.0f);
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
                    if(getEntityWorld().isAirBlock(new BlockPos(hookX, hookY, hookZ))) {
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
        tag.setInteger("hitSide", hitSide.getIndex());
        tag.setInteger("hookX", hookX);
        tag.setInteger("hookY", hookY);
        tag.setInteger("hookZ", hookZ);
    }
    
    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        isHit = tag.getBoolean("isHit");
        hitSide = EnumFacing.byIndex(tag.getInteger("hitSide"));
        hookX = tag.getInteger("hookX");
        hookY = tag.getInteger("hookY");
        hookZ = tag.getInteger("hookZ");
        
        if(isHit) {
            setStill();
        }
    }
    
    public void preRender() {
        if(this.isHit) {
            switch(hitSide.getIndex()) {
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
//            EnumFacing fd = EnumFacing.getFront(hitSide);
            setPosition(hookX + 0.5 + hitSide.getDirectionVec().getX() * 0.51,
                    hookY + 0.5 + hitSide.getDirectionVec().getY() * 0.51,
                    hookZ + 0.5 + hitSide.getDirectionVec().getZ() * 0.51);
        }
    }

}