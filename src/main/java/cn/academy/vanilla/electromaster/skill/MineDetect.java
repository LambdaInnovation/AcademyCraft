/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.electromaster.skill;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.action.SyncActionInstant;
import cn.academy.ability.api.ctrl.instance.SkillInstanceInstant;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.core.Resources;
import cn.academy.core.client.sound.ACSounds;
import cn.academy.vanilla.electromaster.CatElectromaster;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegEntity;
import cn.lambdalib.util.client.RenderUtils;
import cn.lambdalib.util.deprecated.Material;
import cn.lambdalib.util.deprecated.Mesh;
import cn.lambdalib.util.deprecated.MeshUtils;
import cn.lambdalib.util.deprecated.SimpleMaterial;
import cn.lambdalib.util.entityx.EntityAdvanced;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.helper.BlockPos;
import cn.lambdalib.util.helper.Color;
import cn.lambdalib.util.mc.IBlockSelector;
import cn.lambdalib.util.mc.WorldUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

import static cn.lambdalib.util.generic.MathUtils.*;

/**
 * @author WeAthFolD
 */
@Registrant
public class MineDetect extends Skill {
    
    static final int TIME = 100;
    
    public static final MineDetect instance = new MineDetect();

    private MineDetect() {
        super("mine_detect", 3);
    }
    
    @Override
    public SkillInstance createSkillInstance(EntityPlayer player) {
        return new SkillInstanceInstant().addExecution(new MDAction());
    }
    
    public static class MDAction extends SyncActionInstant<MineDetect> {

        public MDAction() {
            super(instance);
        }

        @Override
        public boolean validate() {
            AbilityData aData = AbilityData.get(player);
            CPData cpData = CPData.get(player);

            float exp = ctx().getSkillExp();

            float cp = lerpf(1800, 1400, exp);
            float overload = lerpf(200, 180, exp);

            return cpData.perform(overload, cp);
        }

        @Override
        public void execute() {
            AbilityData aData = AbilityData.get(player);
            
            if(isLocal())
                spawnEffects(aData);
            
            if(!isRemote) {
                player.addPotionEffect(new PotionEffect(Potion.blindness.id, TIME));
                aData.addSkillExp(instance, 0.008f);
                instance.triggerAchievement(player);
            }
        }
        
        @SideOnly(Side.CLIENT)
        private void spawnEffects(AbilityData aData) {
            float exp = aData.getSkillExp(instance);

            player.worldObj.spawnEntityInWorld(
                    new HandlerEntity(player, TIME, getRange(), isAdvanced()));
            ACSounds.playClient(player, "em.minedetect", 0.5f);

            int cooldown = (int) lerpf(900, 400, exp);
            ctx().setCooldown(cooldown);
        }

        private float getRange() {
            return lerpf(15, 30, ctx().getSkillExp());
        }

        private boolean isAdvanced() {
            return ctx().getSkillExp() > 0.5f && ctx().aData.getLevel() >= 4;
        }
        
    }
    
    public static class MineElem {
        public final int x, y, z, level;
        public MineElem(int _x, int _y, int _z, int _lv) {
            x = _x;
            y = _y;
            z = _z;
            level = _lv; //this correspond to the color array index
        }
    }
    
    @SideOnly(Side.CLIENT)
    @RegEntity(clientOnly = true)
    @RegEntity.HasRender
    public static final class HandlerEntity extends EntityAdvanced {
        
        @RegEntity.Render
        public static HandlerRender renderer;
        
        static final IBlockSelector blockFilter = new IBlockSelector() {

            @Override
            public boolean accepts(World world, int x, int y, int z, Block block) {
                return CatElectromaster.isOreBlock(block);
            }
            
        };
        
        //Current elements to display.
        final List<MineElem> aliveSims = new ArrayList<MineElem>();
        
        final int lifeTime;
        public final double range;
        private double safeDistSq; //How many blocks until we peform an update
        
        private double lastX, lastY, lastZ;
        
        private EntityPlayer target;
        
        private final boolean isAdvanced;

        public HandlerEntity(EntityPlayer target, int time, double range, boolean advanced) {
            super(target.worldObj);
            
            this.ignoreFrustumCheck = true;
            
            this.lifeTime = time;
            this.range = Math.min(range, 28);
            this.target = target;
            
            double tmp = range * 0.2;
            safeDistSq = tmp * tmp;
            
            isAdvanced = advanced;
            
            setPosition(target.posX, target.posY, target.posZ);
        }
        
        @Override
        public boolean shouldRenderInPass(int pass) {
            return pass == 1;
        }
        
        @Override
        public void onFirstUpdate() {
            updateBlocks();
        }
        
        @Override
        public void onUpdate() {
            super.onUpdate();
            
            setPosition(target.posX, target.posY, target.posZ);
            
            double distSq = MathUtils.distanceSq(posX, posY, posZ, lastX, lastY, lastZ);
            if(distSq > safeDistSq) {
                updateBlocks();
            }
            
            if(ticksExisted > lifeTime) {
                setDead();
            }
        }
        
        private void updateBlocks() {
            aliveSims.clear();
            
            List<BlockPos> set = WorldUtils.getBlocksWithin(this, range, 1000, blockFilter);
                
            for(BlockPos bp : set) {
                aliveSims.add(new MineElem(
                    bp.x, bp.y, bp.z, 
                    isAdvanced ? Math.min(3, (bp.getBlock().getHarvestLevel(0) + 1)) : 0));
            }
            
            lastX = posX;
            lastY = posY;
            lastZ = posZ;
        }

        @Override
        protected void readEntityFromNBT(NBTTagCompound p_70037_1_) {}

        @Override
        protected void writeEntityToNBT(NBTTagCompound p_70014_1_) {}
        
    }
    
    @SideOnly(Side.CLIENT)
    public static class HandlerRender extends Render {
        
        final ResourceLocation texture = Resources.getTexture("effects/mineview");
        final Mesh mesh = MeshUtils.createBoxWithUV(null, 0, 0, 0, .9, .9, .9);
        final Material material = new SimpleMaterial(texture).setIgnoreLight();
        
        static final Color colors[] = { //alpha will be reset each time rendering
            new Color().setColor4i(115, 200, 227, 0), //default color
            new Color().setColor4i(161, 181, 188, 0), //harvest level 0-3
            new Color().setColor4i(87, 231, 248, 0),
            new Color().setColor4i(97, 204, 94, 0),
            new Color().setColor4i(235, 109, 84, 0)
        };
        

        @Override
        public void doRender(Entity var1, double var2, double var4,
                double var6, float var8, float var9) {
            HandlerEntity he = (HandlerEntity) var1;
            for(MineElem me : he.aliveSims) {
                drawSingle(me, calcAlpha(he.posX - me.x, he.posY - me.y, he.posZ - me.z, he.range));
            }
        }
        
        private float calcAlpha(double x, double y, double z, double range) {
            double jdg = 1 - MathUtils.length(x, y, z) / range * 2.2;
            return 0.3f + (float) (jdg * 0.7);
        }
        
        private void drawSingle(MineElem me, float alpha) {
            double x = me.x - RenderManager.renderPosX, 
                y = me.y - RenderManager.renderPosY, 
                z = me.z - RenderManager.renderPosZ;
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_FOG);
            GL11.glPushMatrix(); {
                RenderUtils.loadTexture(texture);
                GL11.glTranslated(x + .05, y + .05, z + .05);
                Color color = colors[Math.min(colors.length - 1, me.level)];
                color.a = alpha;
                
                material.color.from(color);
                
                mesh.draw(material);
            } GL11.glPopMatrix();
            GL11.glEnable(GL11.GL_FOG);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_LIGHTING);
        }

        @Override
        protected ResourceLocation getEntityTexture(Entity var1) {
            return null;
        }
        
    }

}
