/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.ability.meltdowner.entity;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import cn.academy.core.proxy.ACClientProps;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.draw.DrawObject;
import cn.liutils.api.draw.prop.AssignTexture;
import cn.liutils.api.draw.prop.DisableCullFace;
import cn.liutils.api.draw.prop.DisableLight;
import cn.liutils.api.draw.tess.Rect;
import cn.liutils.api.entityx.EntityX;
import cn.liutils.util.EntityUtils;
import cn.liutils.util.GenericUtils;
import cn.liutils.util.space.Motion3D;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegEntity
@RegEntity.HasRender
public class EntityMdShield extends EntityX {
	
	@SideOnly(Side.CLIENT)
	@RegEntity.Render
	public static ShieldRender render;
	
	final float size = 2f;
	float dmgl, dmgr; //The random range of fallback damage;

	EntityPlayer spawner;
	
	Motion3D motion = new Motion3D();
	
	public EntityMdShield(EntityPlayer player, float _dmgl, float _dmgr) {
		super(player.worldObj);
		dmgl = _dmgl;
		dmgr = _dmgr;
		spawner = player;
		setPosition(player.posX, player.posY, player.posZ);
		setSize(size, size);
		this.isAirBorne = true;
		this.onGround = false;
	}
	
	public EntityMdShield(World world) {
		super(world);
		setSize(size, size);
		this.ignoreFrustumCheck = true;
	}
	
	@Override
	public void entityInit() {
		super.entityInit();
		dataWatcher.addObject(12, Integer.valueOf(-1));
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		if(worldObj.isRemote) { 
			syncClient();
		} else { 
			syncServer();
		};
		
		//TODO: Hack, find cleaner solution later
		preRender();
		
		if(!worldObj.isRemote) {
			List<Entity> entities = EntityUtils.getEntitiesAround(
				worldObj, posX, posY, posZ, size * 1.2, GenericUtils.selectorLiving, spawner);
			for(Entity e : entities) {
				e.attackEntityFrom(DamageSource.causePlayerDamage(getSpawner()), (float) GenericUtils.randIntv(dmgl, dmgr));
			}
		}
	}
	
	public EntityPlayer getSpawner() {
		return spawner;
	}
	
    @Override
	public boolean canBeCollidedWith() {
        return false;
    }
    
    @Override
    public boolean shouldRenderInPass(int pass) {
    	return pass == 1;
    }

	@SideOnly(Side.CLIENT)
	protected void syncClient() {
		int eid = dataWatcher.getWatchableObjectInt(12);
		Entity elb = worldObj.getEntityByID(eid);
		boolean unload = spawner == null;
		if(elb instanceof EntityPlayer) {
			spawner = (EntityPlayer) elb;
		}
	}
	
	protected void syncServer() {
		dataWatcher.updateObject(12, spawner == null ? -1 : spawner.getEntityId());
	}
	
	private void preRender() {
		if(spawner != null) {
			motion.init(spawner, 0, true);
			motion.move(1);
			motion.applyToEntity(this);
		}
	}
	
	@SideOnly(Side.CLIENT)
	public static class ShieldRender extends Render {
		
		DrawObject drawer;
		Rect rect;
		
		public ShieldRender() {
			drawer = new DrawObject();
			drawer.addHandler(rect = new Rect());
			rect.setCentered();
			drawer.addHandlers(
				DisableLight.instance(),
				DisableCullFace.instance(),
				new AssignTexture(ACClientProps.TEX_EFF_MD_SHIELD));
		}

		@Override
		public void doRender(Entity ent, double x, double y,
				double z, float var8, float var9) {
			EntityMdShield shield = (EntityMdShield) ent;
			EntityPlayer clientPlayer = Minecraft.getMinecraft().thePlayer;
			shield.preRender();
			double size = shield.size;
			double realSize = Math.min(1.0, shield.ticksExisted / 20.0) * size;
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glPushMatrix();
			x = shield.posX - clientPlayer.posX;
			y = shield.posY - clientPlayer.posY;
			z = shield.posZ - clientPlayer.posZ;
			rect.setSize(realSize, realSize);
			rect.setCentered();
			GL11.glColor4d(1, 1, 1, 1);
			GL11.glTranslated(x, y, z);
			GL11.glRotated(-shield.rotationYaw + 90, 0, 1, 0);
			GL11.glRotated(shield.rotationPitch, 0, 0, 1);
			drawer.draw();
			GL11.glPopMatrix();
		}

		@Override
		protected ResourceLocation getEntityTexture(Entity var1) {
			return null;
		}
		
	}
	
}
