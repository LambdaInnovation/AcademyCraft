package cn.academy.entity;

import cn.academy.Resources;
import cn.academy.client.sound.ACSounds;
import cn.lambdalib2.particle.Particle;
import cn.lambdalib2.particle.ParticleFactory;
import cn.lambdalib2.particle.decorators.ParticleDecorator;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.mc.RegEntity;
import cn.lambdalib2.registry.mc.RegEntityRender;
import cn.lambdalib2.render.obj.ObjLegacyRender;
import cn.lambdalib2.util.*;
import cn.lambdalib2.util.entityx.EntityAdvanced;
import cn.lambdalib2.util.entityx.EntityCallback;
import cn.lambdalib2.util.entityx.MotionHandler;
import cn.lambdalib2.util.entityx.event.CollideEvent;
import cn.lambdalib2.util.entityx.handlers.Rigidbody;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;

/**
 * @author WeathFolD
 */
@RegEntity
public class EntitySilbarn extends EntityAdvanced
{
    private static final DataParameter<Byte> HIT_SYNC = EntityDataManager.createKey(EntitySilbarn.class, DataSerializers.BYTE);


    @SideOnly(Side.CLIENT)
    static ParticleFactory particles;

    @SideOnly(Side.CLIENT)
    @StateEventCallback
    public static void init(FMLInitializationEvent event) {
        Particle p = new Particle();
        p.texture = Resources.getTexture("entities/silbarn_frag");
        p.size = 0.1f;
        p.gravity = 0.03f;
        p.customRotation = true;
        
        particles = new ParticleFactory(p);
        particles.addDecorator(new ParticleDecorator() {
            
            double vx, vy;
            final double fac = 25;
            {
                double phi = RandUtils.nextDouble() * Math.PI * 2;
                vx = Math.sin(phi);
                vy = Math.cos(phi);
            }

            @SideOnly(Side.CLIENT)
            @Override
            public void decorate(Particle particle) {
                particle.addMotionHandler(new MotionHandler() {
                    @Override
                    public String getID() { return "Rotator"; }
                    @Override
                    public void onStart() {
                        particle.rotationYaw = RandUtils.nextFloat() * 360;
                        particle.rotationPitch = RandUtils.rangef(-90, 90);
                    }

                    @Override
                    public void onUpdate() {
                        particle.rotationYaw += vx * fac;
                        particle.rotationPitch += vy * fac;
                    }
                });
            }
            
        });
    }
    
    boolean hit;
    
    double createTime;
    
    Vec3d axis = new Vec3d(rand.nextInt(), rand.nextInt(), rand.nextInt());
    
    {
        final Rigidbody rigidbody = new Rigidbody();
        rigidbody.linearDrag = 0.8;
        rigidbody.entitySel = EntitySelectors.nothing();
        
        this.addMotionHandler(rigidbody);
        //this.addDaemonHandler(new GravityApply(this, 0.05));
        executeAfter((EntityCallback<EntitySilbarn>) ent -> rigidbody.gravity = 0.12, 50);
        setSize(.4f, .4f);
    }

    public EntitySilbarn(EntityPlayer player) {
        super(player.world);
        this.regEventHandler(new CollideEvent.CollideHandler() {
            
            @Override
            public void onEvent(CollideEvent event) {
                if(!hit) {
                    hit = true;
                    if(event.result.entityHit instanceof EntitySilbarn)
                        playSound(Resources.sound("entity.silbarn_heavy"), 0.5f, 1.0f);
                    else
                        playSound(Resources.sound("entity.silbarn_light"), 0.5f, 1.0f);
                    executeAfter(Entity::setDead, 10);
                }
            }
            
        });

        setPosition(player.posX, player.posY + player.eyeHeight, player.posZ);

        Vec3d look = player.getLookVec();
        motionX = look.x;
        motionY = look.y;
        motionZ = look.z;
        
        this.rotationYaw = player.rotationYawHead;
        this.isAirBorne = true;
        this.onGround = false;
    }

    public EntitySilbarn(World world) {
        super(world);
        if(SideUtils.isClient())
        {
            this.createTime = GameTimer.getTime();

            this.regEventHandler(new CollideEvent.CollideHandler() {
                @Override
                @SuppressWarnings("sideonly")
                public void onEvent(CollideEvent event) {
                    if(!hit) {
                        RayTraceResult res = event.result;
                        EnumFacing dir = res.sideHit;
                        final double mul = 0.1;
                        double tx = res.hitVec.x + dir.getYOffset() * mul,
                                ty = res.hitVec.y + dir.getYOffset() * mul,
                                tz = res.hitVec.z + dir.getZOffset() * mul;
                        spawnEffects(tx, ty, tz);
                        setDead();
                    }
                    hit = true;
                }
            });
        }

        
        this.isAirBorne = true;
        this.onGround = false;
    }
    
    @Override
    public void entityInit() {
        this.dataManager.register(HIT_SYNC, (byte) 0);
    }
    
    public boolean isHit() {
        return hit;
    }
    
    @Override
    public void onUpdate() {
        super.onUpdate();
        sync();
    }

    @SuppressWarnings("sideonly")
    private void sync() {
        if(world.isRemote) {
            boolean b = dataManager.get(HIT_SYNC) != 0;
            if(!hit && b) {
                spawnEffects(posX, posY, posZ);
            }
            hit = b;
        } else {
            dataManager.set(HIT_SYNC, (byte) (hit ? 1 : 0));
        }
    }
    
    @Override
    public boolean canBeCollidedWith() {
        return true;
    }
    
    @SideOnly(Side.CLIENT)
    private void spawnEffects(double tx, double ty, double tz) {
        int n = RandUtils.rangei(18, 27);
        for(int i = 0; i < n; ++i) {
            double vel = RandUtils.ranged(0.08, 0.18),
                vsq = vel * vel,
                vx = rand.nextDouble() * vel,
                vxsq = vx * vx,
                vy = rand.nextDouble() * Math.sqrt(vsq - vxsq),
                vz = Math.sqrt(vsq - vxsq - vy * vy);
            vx *= rand.nextBoolean() ? 1 : -1;
            vy *= rand.nextBoolean() ? 1 : -1;
            vz *= rand.nextBoolean() ? 1 : -1;
            vy += 0.2;
            
            particles.setPosition(posX, posY, posZ);
            particles.setVelocity(vx, vy, vz);
            world.spawnEntity(particles.next(world));
        }
        //TileMatrix
    }
    
    @SideOnly(Side.CLIENT)
    @RegEntityRender(EntitySilbarn.class)
    public static class RenderSibarn extends Render<EntitySilbarn> {
        
        private final ObjLegacyRender model = Resources.getModel("silbarn");
        private final ResourceLocation tex = Resources.getTexture("models/silbarn");

        public RenderSibarn(RenderManager renderManager) {
            super(renderManager);
        }

        @Override
        public void doRender(EntitySilbarn var1, double x, double y,
                double z, float var8, float var9) {
            EntitySilbarn sibarn = (EntitySilbarn) var1;
            if(sibarn.hit)
                return;
            GL11.glPushMatrix();
            GL11.glTranslated(x, y, z);
            RenderUtils.loadTexture(tex);
            double scale = .05;
            GL11.glScaled(scale, scale, scale);
            GL11.glRotated(0.03 * (long)((GameTimer.getTime() - sibarn.createTime) * 1000),
                    sibarn.axis.x, sibarn.axis.y, sibarn.axis.z);
            GL11.glRotated(-var1.rotationYaw, 0, 1, 0);
            GL11.glRotated(90, 1, 0, 0);
            model.renderAll();
            GL11.glPopMatrix();
        }

        @Nullable
        @Override
        protected ResourceLocation getEntityTexture(EntitySilbarn entity) {
            return null;
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        setDead();
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {}

}