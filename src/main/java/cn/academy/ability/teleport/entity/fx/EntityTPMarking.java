/**
 * 
 */
package cn.academy.ability.teleport.entity.fx;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cn.academy.ability.teleport.client.model.SimpleModelBiped;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.core.proxy.ACClientProps;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.entityx.EntityX;
import cn.liutils.api.render.IDrawable;
import cn.liutils.util.GenericUtils;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.space.Motion3D;

/**
 * Spawn a position mark indicating where the player would be teleport to.
 * You should spawn this entity in both sides and it will not synchronize.
 * @author WeathFolD
 */
@RegistrationClass
@RegEntity(clientOnly = true)
@RegEntity.HasRender
public abstract class EntityTPMarking extends EntityX {
	
	@RegEntity.Render
	public static MarkRender render;
	
	final AbilityData data;
	protected final EntityPlayer player;

	public EntityTPMarking(EntityPlayer player) {
		super(player.worldObj);
		data = AbilityDataMain.getData(player);
		this.player = player;
		updatePos();
		this.ignoreFrustumCheck = true;
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		rotationPitch = player.rotationPitch;
		rotationYaw = player.rotationYaw;
		this.updatePos();
	}
	
	@Override
	public boolean shouldRenderInPass(int pass) {
		return pass == 1;
	}
	
	protected void updatePos() {
		double md = getMaxDistance();
		MovingObjectPosition mop = GenericUtils.tracePlayer(player, md);
		
		
		if(mop != null) {
			double x = mop.hitVec.xCoord,
					y= mop.hitVec.yCoord,
					z= mop.hitVec.zCoord;
			switch(mop.sideHit) {
			case 0:
				y -= 1.0; break;
			case 1:
				y += 1.8; break;
			case 2:
				z -= .6; y = mop.blockY + 1.7; break;
			case 3:
				z += .6; y = mop.blockY + 1.7;  break;
			case 4:
				x -= .6; y = mop.blockY + 1.7;  break;
			case 5: 
				x += .6; y = mop.blockY + 1.7;  break;
			}
			//check head
			if(mop.sideHit > 1) {
				int hx = (int) x, hy = (int) (y + 1), hz = (int) z;
				if(!worldObj.isAirBlock(hx, hy, hz)) {
					y -= 1.25;
				}
			}
			
			setPosition(x, y, z);
		} else {
			Motion3D mo = new Motion3D(player, true);
			mo.move(md);
			setPosition(mo.posX, mo.posY, mo.posZ);
		}
	}
	
	public double getDist() {
		return this.getDistanceToEntity(player);
	}
	
	protected abstract double getMaxDistance();
	
	@SideOnly(Side.CLIENT)
	public static class MarkRender extends Render {
		
		protected ResourceLocation[] tex = ACClientProps.ANIM_TP_MARK;
		protected IDrawable model = new SimpleModelBiped();

		@Override
		public void doRender(Entity ent, double x, double y, double z, float var8, float var9) {
			EntityTPMarking mark = (EntityTPMarking) ent;
			int texID = (mark.ticksExisted / 2) % tex.length;
			
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glDisable(GL11.GL_CULL_FACE);
			GL11.glDisable(GL11.GL_DEPTH_TEST);
			GL11.glColor4d(1, 1, 1, 1);
			GL11.glPushMatrix(); {
				GL11.glTranslated(x, y, z);
				
				GL11.glRotated(-mark.rotationYaw, 0, 1, 0);
				GL11.glScaled(-1, -1, 1);
				OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
				Tessellator.instance.setBrightness(15728880);
				RenderUtils.loadTexture(tex[texID]);
				model.draw();
			} GL11.glPopMatrix();
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glEnable(GL11.GL_LIGHTING);
			GL11.glEnable(GL11.GL_CULL_FACE);
		}

		@Override
		protected ResourceLocation getEntityTexture(Entity var1) {
			return null;
		}
		
	}

}
