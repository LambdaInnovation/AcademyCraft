/**
 * 
 */
package cn.academy.ability.electro.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import cn.academy.ability.electro.client.render.RenderElecArc;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.misc.entity.EntityRay;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.annoreg.mc.RegEntity.HasRender;
import cn.liutils.api.entityx.MotionHandler;
import cn.liutils.util.misc.RandomSequence;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegEntity
@HasRender
public class EntityElecArc extends EntityRay {
	
	@SideOnly(Side.CLIENT)
	ResourceLocation TEXS[] = ACClientProps.ANIM_ELEC_ARC;
	
	@SideOnly(Side.CLIENT)
	static final int SEQ_SIZE = 20;
	
	@SideOnly(Side.CLIENT)
	public RandomSequence indSeq = new RandomSequence(SEQ_SIZE, TEXS.length);
	
	@RegEntity.Render
	@SideOnly(Side.CLIENT)
	public static RenderElecArc renderer;
	
	public boolean isDrawing;
	int lastTick, tickWait;

	public EntityElecArc(EntityLivingBase creator) {
		super(creator);
	}
	
	@SideOnly(Side.CLIENT)
	public EntityElecArc(World world) {
		super(world);
		this.addDaemonHandler(new MotionHandler(this) {
			@Override public void onSpawnedInWorld() {}

			@Override
			public void onUpdate() {
				indSeq.rebuild();
				if(ticksExisted - lastTick > tickWait) {
					tickWait = isDrawing ? (5 + rand.nextInt(5)) : (8 + rand.nextInt(5));
					lastTick = ticksExisted;
					isDrawing = !isDrawing;
				}
			}

			@Override
			public String getID() {
				return "perm";
			}
		});
	}
	
	@SideOnly(Side.CLIENT)
	public int getIndex(int i) {
		return indSeq.get(i % SEQ_SIZE);
	}

}
