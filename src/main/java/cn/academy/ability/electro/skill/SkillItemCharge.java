/**
 * 
 */
package cn.academy.ability.electro.skill;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import cn.academy.ability.electro.client.render.PieceSmallArc;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.PatternHold;
import cn.academy.api.ctrl.pattern.PatternHold.State;
import cn.academy.core.AcademyCraftMod;
import cn.academy.core.proxy.ACClientProps;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.entityx.EntityX;
import cn.liutils.util.space.Motion3D;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 对手上的物品充能
 * TODO 施工中 现在是实验场
 * @author WeathFolD
 */
@RegistrationClass
public class SkillItemCharge extends SkillBase {

	public SkillItemCharge() {
		//RenderingRegistry.registerEntityRenderingHandler(EntityTest.class, new RenderTest());
		EntityRegistry.registerModEntity(EntityTest.class, "aaa", 3, 
				AcademyCraftMod.INSTANCE, 60, 2, true);

		//don't put reg here!
		//REPLY: Just for temp testing purpose, will remove later
	}
	
	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternHold(100) {

			@Override
			public State createSkill(EntityPlayer player) {
				return new StateHold(player);
			}

		});
	}
	
	public String getInternalName() {
		return "em_itemcharge";
	}
	
	@SideOnly(Side.CLIENT)
	public ResourceLocation getLogo() {
		return ACClientProps.ELEC_CHARGE;
	}
	
	@SideOnly(Side.CLIENT)
	private static class RenderTest extends Render {
		
		private PieceSmallArc piece;
		
		public RenderTest() {
			piece = new PieceSmallArc(1.0);
		}

		@Override
		public void doRender(Entity var1, double x, double y,
				double z, float var8, float var9) {
			GL11.glPushMatrix(); {
				GL11.glTranslated(x, y, z);
				piece.draw();
			} GL11.glPopMatrix();
		}

		@Override
		protected ResourceLocation getEntityTexture(Entity var1) {
			return null;
		}
		
	}
	
	@RegEntity(clientOnly = true, renderName = "renderer")
	public static class EntityTest extends EntityX {
		
		@SideOnly(Side.CLIENT)
		public static RenderTest renderer;
		
		public EntityTest(EntityPlayer ep) {
			super(ep.worldObj);
			new Motion3D(ep, true).applyToEntity(this);
			double sc = 1.5;
			motionX *= sc;
			motionY *= sc;
			motionZ *= sc;
			//this.addDaemonHandler(new GravityApply(this, 0.05));
		}
		
		public EntityTest(World world) {
			super(world);
		}
		
		@Override
		public void onUpdate() {
			super.onUpdate();
			if(ticksExisted > 300) {
				setDead();
			}
		}
		
	}
	
	public static class StateHold extends State {

		public StateHold(EntityPlayer player) {
			super(player);
		}

		@Override
		public void onStart() { 
			World world = player.worldObj;
			if(!world.isRemote) {
				world.spawnEntityInWorld(new EntityTest(player));
			}
		}

		@Override
		public void onFinish() { }

		@Override
		public void onHold() {
			//this.player.xxxxxx
		}
		
	}

}
