/**
 * 
 */
package cn.academy.misc.entity;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import cn.academy.misc.client.render.RenderBlockSimulator;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.entityx.EntityX;
import cn.liutils.api.entityx.MotionHandler;
import cn.liutils.api.entityx.motion.ApplyPos;
import cn.liutils.util.DebugUtils;
import cn.liutils.util.space.BlockPos;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
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
	
	public float alpha = 1.0f;
	public boolean hasDepth = false;
	public boolean hasLight = false;
	
	BlockPos blockPos;
	ApplyPos posHandler;

	public EntityBlockSimulator(World world, int x, int y, int z, ResourceLocation blockTex) {
		super(world);
		texture = blockTex;
		blockPos = new BlockPos(x, y, z, null);
		this.removeDaemonHandler("velocity");
		this.removeDaemonHandler("collision");
		
		posHandler = new ApplyPos(this, x, y, z);
		this.setCurMotion(posHandler);
		posHandler.onUpdate(); //set pos once
		
		this.setSize(1.0f, 1.0f);
		this.ignoreFrustumCheck = true;
	}
	
	public void resetPos(int bx, int by, int bz) {
		posHandler.setPos(bx, by, bz);
		blockPos.x = bx;
		blockPos.y = by;
		blockPos.z = bz;
	}
	
	public void resetTexture(ResourceLocation texture) {
		this.texture = texture;
	}
	
	@Override
	public boolean shouldRenderInPass(int pass) {
		return pass == 1;
	}

}
