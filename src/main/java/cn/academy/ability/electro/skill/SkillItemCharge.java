/**
 * 
 */
package cn.academy.ability.electro.skill;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import cn.academy.ability.electro.client.render.PieceSmallArc;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.PatternHold;
import cn.academy.api.ctrl.pattern.PatternHold.State;
import cn.academy.core.proxy.ACClientProps;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 对手上的物品充能
 * TODO 施工中 现在是实验场
 * @author WeathFolD
 */
public class SkillItemCharge extends SkillBase {

	public SkillItemCharge() {
		//don't put reg here!
		//RenderingRegistry.registerEntityRenderingHandler(EntityTest.class, new RenderTest());
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
	
	public static class RenderTest extends Render {
		
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
	
	public static class EntityTest extends Entity {
		
		public EntityTest(World world, double x, double y, double z) {
			super(world);
			setPosition(x, y, z);
		}
		
		@Override
		public void onUpdate() {
			if(++ticksExisted > 300) {
				setDead();
			}
			//N/A
		}

		@Override
		protected void entityInit() {}

		@Override
		protected void readEntityFromNBT(NBTTagCompound var1) {}

		@Override
		protected void writeEntityToNBT(NBTTagCompound var1) {}
		
	}
	
	private static class StateHold extends State {

		public StateHold(EntityPlayer player) {
			super(player);
		}

		@Override
		public void onStart() { 
			World world = player.worldObj;
			if(world.isRemote) {
				world.spawnEntityInWorld(new EntityTest(world, player.posX, player.posY, player.posZ));
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
