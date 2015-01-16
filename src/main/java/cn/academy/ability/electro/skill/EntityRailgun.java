/**
 * 
 */
package cn.academy.ability.electro.skill;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.misc.client.render.RendererRayTiling;
import cn.academy.misc.entity.EntityRay;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.annoreg.mc.RegEntity.HasRender;
import cn.liutils.util.space.Motion3D;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegEntity
@HasRender
public class EntityRailgun extends EntityRay {
	
	@RegEntity.Render
	@SideOnly(Side.CLIENT)
	public static Render renderer;
	
	public static class Render extends RendererRayTiling {

		public Render() {
			super(ACClientProps.TEX_EFF_RAILGUN);
		}
		
	}
	
	public EntityRailgun(EntityLivingBase creator) {
		super(creator);
	}
	
	public EntityRailgun(World world) {
		super(world);
	}

}
