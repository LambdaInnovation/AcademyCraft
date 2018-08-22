package cn.academy.entity;

import cn.academy.client.render.util.ACRenderingHelper;
import cn.academy.Resources;
import cn.lambdalib2.registry.mc.RegEntity;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.RenderUtils;
import cn.lambdalib2.util.entityx.EntityAdvanced;
import cn.lambdalib2.util.entityx.EntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/**
 * @author WeAthFolD
 */
@RegEntity
public class EntityMdBall extends EntityAdvanced
{
    private static final DataParameter<Integer> SPAWNER_ID = EntityDataManager.createKey(EntityMdBall.class, DataSerializers.VARINT);
    private static final DataParameter<Float> SUB_X = EntityDataManager.createKey(EntityMdBall.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> SUB_Y = EntityDataManager.createKey(EntityMdBall.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> SUB_Z = EntityDataManager.createKey(EntityMdBall.class, DataSerializers.FLOAT);
    private static final DataParameter<Integer> LIFE = EntityDataManager.createKey(EntityMdBall.class, DataSerializers.VARINT);

    @SideOnly(Side.CLIENT)
    public static R renderer;
    
    static final int MAX_TETXURES = 5;
    
    static final float RANGE_FROM = 0.8f, RANGE_TO = 1.3f;
    
    //Synced states
    EntityPlayer spawner;
    float subX = 0, subY = 0, subZ = 0;
    
    //Ctor init data
    int life = 50;
    
    //Client-side data
    int texID;
    
    double spawnTime;
    double lastTime;
    long burstTime = 400;
    double alphaWiggle = 0.8;
    double accel;
    
    double offsetX, offsetY, offsetZ;
    
    public EntityMdBall(EntityPlayer player) {
        this(player, 2333333, null);
    }
    
    public EntityMdBall(EntityPlayer player, int life) {
        this(player, life, null);
    }
    
    public EntityMdBall(EntityPlayer player, int life, final EntityCallback<EntityMdBall> callback) {
        super(player.getEntityWorld());
        this.spawner = player;
        
        // Calc the sub-offset
        float theta = -player.rotationYaw / 180 * MathUtils.PI_F +
            RandUtils.rangef(-MathUtils.PI_F * 0.45f, MathUtils.PI_F * 0.45f);
        
        float range = RandUtils.rangef(RANGE_FROM, RANGE_TO);
        subX = MathHelper.sin(theta) * range;
        subZ = MathHelper.cos(theta) * range;
        
        subY = RandUtils.rangef(-1.2f, 0.2f);
        
        // Pos init
        updatePosition();
        
        this.life = life;

        this.executeAfter((EntityCallback<EntityMdBall>) Entity::setDead, life);
        if(callback != null)
            this.executeAfter(callback, life - 2);
    }
    
    public EntityMdBall(World world) {
        super(world);
        spawnTime = GameTimer.getTime();
        ignoreFrustumCheck = true; // Small variation in render tick posupdate will cause problem
    }
    
    @Override
    public void entityInit() {
        super.entityInit();
        this.dataManager.register(SPAWNER_ID,0);
        this.dataManager.register(SUB_X,0F);
        this.dataManager.register(SUB_Y,0F);
        this.dataManager.register(SUB_Z,0F);
        this.dataManager.register(LIFE,0);
    }
    
    @Override
    public void onFirstUpdate() {
        if(!world.isRemote) {
            this.dataManager.set(SPAWNER_ID, spawner.getEntityId());
            this.dataManager.set(SUB_X, subX);
            this.dataManager.set(SUB_Y, subY);
            this.dataManager.set(SUB_Z, subZ);
            this.dataManager.set(LIFE, life);
        }
    }
    
    @Override
    public void onUpdate() {
        super.onUpdate();
        
        if(world.isRemote) {
            
            if(getSpawner() == null) {
                int eid = this.dataManager.get(SPAWNER_ID);
                Entity e = world.getEntityByID(eid);
                if(e instanceof EntityPlayer) {
                    spawner = (EntityPlayer) e;
                }
                
            } else {
                if(subX == 0 && subY == 0 && subZ == 0) {
                    subX = this.dataManager.get(SUB_X);
                    subY = this.dataManager.get(SUB_Y);
                    subZ = this.dataManager.get(SUB_Z);
                    life = this.dataManager.get(LIFE);
                } else {
                    updatePosition();
                }
            }
            
        } else {
            
            updatePosition();
            
        }
    }
    
    protected EntityPlayer getSpawner() {
        return spawner;
    }
    
    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 1;
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        setDead();
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {}
    
    @SideOnly(Side.CLIENT)
    private boolean updateRenderTick() {
        
        if(spawner == null || (subX == 0 && subY == 0 && subZ == 0)) 
            return false;
        
        final double maxAccel = 4;
        double time = GameTimer.getTime();
        double life = time - spawnTime;
        
        //Alpha wiggling
        if(lastTime != 0) {
            double dt = time - lastTime;
            if(rand.nextInt(8) < 3) {
                accel = RandUtils.ranged(-maxAccel, maxAccel);
                //System.out.println("AccelChange=>" + accel);
            }
            
            //System.out.println("AV=>" + alphaVel);
            alphaWiggle += accel * dt / 1000.0;
            if(alphaWiggle > 1) alphaWiggle = 1;
            if(alphaWiggle < 0) alphaWiggle = 0;
        }
        lastTime = time;
        
        //Texture wiggling
        if(rand.nextInt(8) < 2) {
            texID = rand.nextInt(MAX_TETXURES);
        }
        
        //Surrounding
        float phase = (float) (life / 300.0f);
        offsetX = 0.03 * MathHelper.sin(phase);
        offsetZ = 0.03 * MathHelper.cos(phase);
        offsetY = 0.04 * MathHelper.cos((float) (phase * 1.4 + Math.PI / 3.5));
        
        updatePosition();
        
        return true;
    }
    
    private double getAlpha() {
        int lifeMS = life * 50;
        double time = GameTimer.getTime();
        double dt = time - spawnTime;
        
        final int blendTime = 150;
        if(dt > lifeMS - blendTime)
            return Math.max(0, MathUtils.lerpf(1, 0, (float) (dt - (lifeMS - blendTime)) / blendTime));
        if(dt > lifeMS - burstTime)
            return MathUtils.lerp(0.6, 1.0, (double) (dt - (lifeMS - burstTime)) / (burstTime - blendTime));
        if(dt < 300)
            return MathUtils.lerp(0, 0.6, (double) dt / 300);
        return 0.6;
    }
    
    private float getSize() {
        int lifeMS = life * 50;
        double time = GameTimer.getTime();
        double dt = time - spawnTime;
        
        if(dt > lifeMS - 100)
            return Math.max(0, MathUtils.lerpf(1.5f, 0, (float) (dt - (lifeMS - 100)) / 100));
        if(dt > lifeMS - 300)
            return MathUtils.lerpf(1, 1.5f, (float) (dt - (lifeMS - 300)) / 200);
        return 1;
    }
    
    private void updatePosition() {
        posX = spawner.posX + subX;
        posY = spawner.posY + subY + (world.isRemote ? 0 : 1.6); //Fix for different sides
        posZ = spawner.posZ + subZ;
    }
    
    @SideOnly(Side.CLIENT)
    public static class R extends RenderIcon {
        
        ResourceLocation[] textures;
        ResourceLocation glowTexture;

        public R() {
            super(null);
            textures = Resources.getEffectSeq("mdball", MAX_TETXURES);
            glowTexture = Resources.getTexture("effects/mdball/glow");
            //this.minTolerateAlpha = 0.05f;
            this.shadowOpaque = 0;
        }
        
        @Override
        public void doRender(Entity par1Entity, double x, double y,
                double z, float par8, float par9) {
            if(RenderUtils.isInShadowPass()) {
                return;
            }
            
            EntityMdBall ent = (EntityMdBall) par1Entity;
            if(!ent.updateRenderTick())
                return;
            
            EntityPlayer clientPlayer = Minecraft.getMinecraft().player;
            
            //HACK: Force set the render pos to prevent glitches
            {
                x = ent.posX - clientPlayer.posX;
                y = ent.posY - clientPlayer.posY;
                z = ent.posZ - clientPlayer.posZ;
                
                if(!ACRenderingHelper.isThePlayer(ent.getSpawner()))
                    y += 1.6;
            }
            
            GL11.glPushMatrix();
            {
                ShaderSimple.instance().useProgram();
                GL11.glTranslated(ent.offsetX, ent.offsetY, ent.offsetZ);
                
                double alpha = ent.getAlpha();
                float size = ent.getSize();
                
                //Glow texture
                this.color.a = alpha * (0.3 + ent.alphaWiggle * 0.7);
                this.icon = glowTexture;
                this.setSize(0.7f * size);
                super.doRender(par1Entity, x, y, z, par8, par9);
                
                //Core
                this.color.a = alpha * (0.8 + 0.2 * ent.alphaWiggle);
                this.icon = textures[ent.texID];
                this.setSize(0.5f * size);
                super.doRender(par1Entity, x, y, z, par8, par9);
                GL20.glUseProgram(0);
            }
            GL11.glPopMatrix();
        }
        
    }

}