package cn.academy.entity;

import cn.academy.client.render.util.IPointFactory;
import cn.academy.client.render.util.CubePointFactory;
import cn.academy.client.render.util.ArcFactory;
import cn.academy.client.render.util.ArcFactory.Arc;
import cn.academy.client.render.util.SubArcHandler;
import cn.lambdalib2.registry.mc.RegEntity;
import cn.lambdalib2.util.entityx.EntityAdvanced;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Spawn a surround arc effect around the specific entity or block.
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
@RegEntity
public class EntitySurroundArc extends EntityAdvanced {
    
    static final int TEMPLATES = 15;
    
    static {
        ArcFactory factory = new ArcFactory();
        factory.widthShrink = 0.9;
        factory.maxOffset = 0.8;
        factory.passes = 3;
        factory.width = 0.2;
        factory.branchFactor = 0.7;
        
        ArcType.THIN.templates = factory.generateList(10, 1.5, 2);
        
        factory.width = 0.3;
        ArcType.NORMAL.templates = factory.generateList(10, 3, 4);
        
        factory.passes = 3;
        factory.width = 0.35;
        factory.maxOffset =    1.2;
        factory.branchFactor = 0.45;
        ArcType.BOLD.templates = factory.generateList(10, 3.5, 4.5);
    }
    
    public enum ArcType {
        THIN(4), NORMAL(6), BOLD(5);
        
        public Arc[] templates;
        public int count;
        
        ArcType(int _count) {
            count = _count;
        }
    }
    
    @RegEntity.Render
    public static Renderer renderer;
    
    private ArcType arcType = ArcType.BOLD;
    private final PosObject pos;
    
    public boolean draw = true;
    
    public int life = 100;

    SubArcHandler arcHandler;
    
    IPointFactory pointFactory;
    
    public EntitySurroundArc(Entity follow) {
        this(follow, 1.3);
    }
    
    public EntitySurroundArc(Entity follow, double sizeMultiplyer) {
        super(follow.getEntityWorld());
        pos = new EntityPos(follow);
        pointFactory = new CubePointFactory(
            follow.width * sizeMultiplyer, 
            follow.height * sizeMultiplyer, 
            follow.width * sizeMultiplyer).setCentered(true);
    }
    
    public EntitySurroundArc(World world, double x, double y, double z, double wl, double h) {
        super(world);
        pos = new ConstPos(x, y, z);
        pointFactory = new CubePointFactory(wl, h, wl).setCentered(true);
    }
    
    public void updatePos(double x, double y, double z) {
        pos.x = x;
        pos.y = y;
        pos.z = z;
    }
    
    public EntitySurroundArc setArcType(ArcType type) {
        arcType = type;
        return this;
    }
    
    public EntitySurroundArc setLife(int life) {
        this.life = life;
        return this;
    }
    
    @Override
    public void entityInit() {
        ignoreFrustumCheck = true;
    }
    
    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 1;
    }
    
    @Override
    public void onFirstUpdate() {
        // Create the arcs!
        arcHandler = new SubArcHandler(arcType.templates);
        arcHandler.frameRate = 0.6;
        arcHandler.switchRate = 0.7;
        
        doGenerate();
    }
    
    /**
     * Do the arc generation.
     */
    protected void doGenerate() {
        for(int i = 0; i < arcType.count; ++i) {
            double yaw = rand.nextDouble() * Math.PI * 2;
            double pitch = rand.nextDouble() * Math.PI;
            
            double y = Math.sin(pitch),
                zz = Math.sqrt(1 - y * y),
                x = zz * Math.sin(yaw),
                z = zz * Math.cos(yaw);
            
            arcHandler.generateAt(pointFactory.next());
        }
    }
    
    @Override
    public void onUpdate() {
        super.onUpdate();
        if(arcHandler.isEmpty())
            doGenerate();
        
        arcHandler.tick();
        
        pos.tick();
        setPosition(pos.x, pos.y, pos.z);
        rotationYaw = pos.yaw;
        rotationPitch = pos.pitch;
        
        if(ticksExisted == life)
            setDead();
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        setDead();
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {}
    
    private abstract class PosObject {
        double x, y, z;
        float yaw, pitch;
        
        void tick() {}
    }
    
    public static class Renderer extends Render {

        @Override
        public void doRender(Entity entity, double x,
                double y, double z, float a,
                float b) {
            EntitySurroundArc esa = (EntitySurroundArc) entity;
            
            if(esa.draw && esa.arcHandler != null) {
                GL11.glPushMatrix();
                
                GL11.glTranslated(x, y, z);
                
                GL11.glRotatef(-esa.rotationYaw, 0, 1, 0);
                esa.arcHandler.drawAll();
                
                GL11.glPopMatrix();
            }
        }

        @Override
        protected ResourceLocation getEntityTexture(Entity entity) {
            return null;
        }
        
    }
    
    private class EntityPos extends PosObject {
        
        final Entity entity;
        final boolean isPlayer;
        
        public EntityPos(Entity e) {
            entity = e;
            isPlayer = e instanceof EntityPlayer && e.equals(Minecraft.getMinecraft().player);
        }
        
        @Override
        void tick() {
            x = entity.posX;
            y = isPlayer ? entity.posY - 1.6 : entity.posY;
            z = entity.posZ;
            yaw = entity instanceof EntityLivingBase ? ((EntityLivingBase)entity).rotationYawHead : entity.rotationYaw;
            pitch = entity.rotationPitch;
        }
    }
    
    private class ConstPos extends PosObject {
        
        public ConstPos(double _x, double _y, double _z) {
            x = _x;
            y = _y;
            z = _z;
        }
    }

}