/**
 * 
 */
package cn.academy.misc.entity;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import cn.academy.ability.electro.skill.SkillMineDetect.HandlerEntity;
import cn.academy.misc.client.render.RenderBlockSimulator;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.entityx.EntityX;
import cn.liutils.api.entityx.MotionHandler;
import cn.liutils.api.entityx.motion.ApplyPos;
import cn.liutils.util.space.BlockPos;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Currently used solely for MineView skill, so none of the hierarchy optimization is done.
 * @author WeathFolD
 */
@RegistrationClass
@RegEntity()
@RegEntity.HasRender
@SideOnly(Side.CLIENT)
public class EntityBlockSimulator extends EntityX {
	
	@SideOnly(Side.CLIENT)
	@RegEntity.Render
	public static RenderBlockSimulator renderer;
	
	public ResourceLocation texture;
	
	public boolean hasDepth = false;
	public boolean hasLight = false;
	
	BlockPos blockPos;
	ApplyPos posHandler;
	
	HandlerEntity parent;

	public EntityBlockSimulator(HandlerEntity parent, BlockPos bp, ResourceLocation blockTex) {
		super(parent.worldObj);
		texture = blockTex;
		blockPos = bp.copy();
		this.parent = parent;
		
		this.removeDaemonHandler("velocity");
		this.removeDaemonHandler("collision");
		
		posHandler = new ApplyPos(this, blockPos.x, blockPos.y, blockPos.z);
		this.setCurMotion(posHandler);
		posHandler.onUpdate(); //set pos once
		
		this.addDaemonHandler(new ExistJudge());
		
		this.setSize(1.0f, 1.0f);
		this.ignoreFrustumCheck = true;
	}
	
	public void resetPos(int bx, int by, int bz) {
		posHandler.setPos(bx, by, bz);
		blockPos.x = bx;
		blockPos.y = by;
		blockPos.z = bz;
	}
	
	private class ExistJudge extends MotionHandler {

		public ExistJudge() {
			super(EntityBlockSimulator.this);
		}

		@Override
		public void onCreated() {}

		@Override
		public void onUpdate() {
			Block block = worldObj.getBlock(blockPos.x, blockPos.y, blockPos.z);
			if(block != blockPos.block) {
				setDead();
			}
		}

		@Override
		public String getID() {
			return "ej";
		}
		
	}
	
	public void resetTexture(ResourceLocation texture) {
		this.texture = texture;
	}
	
	public HandlerEntity getParent() {
		return parent;
	}
	
	@Override
	public boolean shouldRenderInPass(int pass) {
		return pass == 1;
	}
	
	@Override
	protected void readEntityFromNBT(NBTTagCompound var1) {
		this.setDead();
	}

}
