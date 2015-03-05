/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.ability.meltdowner.entity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import cn.academy.ability.meltdowner.client.render.RenderMdRayBase;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.misc.client.render.RendererRayTiling;
import cn.academy.misc.entity.EntitySilbarn;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.util.GenericUtils;
import cn.liutils.util.misc.IntRandomSequence;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegEntity()
@RegEntity.HasRender
public class EntityMeltDowner extends EntityMdRayBase {
	
	IntRandomSequence seq = new IntRandomSequence(20, getTexData().length - 1);
	
	@SideOnly(Side.CLIENT)
	@RegEntity.Render
	public static RenderMD renderer;
	
	float dmg;
	
	{
		this.execAfter(30, new EntityCallback<EntityMeltDowner>() {
			@Override
			public void execute(EntityMeltDowner ent) {
				ent.setFadeout(10);
			}
		});
	}

	public EntityMeltDowner(EntityPlayer _spawner, float dmg) {
		super(_spawner);
		this.dmg = dmg;
	}

	@SideOnly(Side.CLIENT)
	public EntityMeltDowner(World world) {
		super(world);
	}

	@Override
	protected void handleCollision(MovingObjectPosition mop) {
		if(mop.typeOfHit == MovingObjectType.ENTITY) {
			if(mop.entityHit instanceof EntitySilbarn) {
				doScatterAt((EntitySilbarn) mop.entityHit);
				return;
			} else {
				mop.entityHit.attackEntityFrom(DamageSource.causeMobDamage(getSpawner()), dmg);
			}
		}
		//真男人从不回头看爆炸
		GenericUtils.explode(worldObj, getSpawner(), dmg * 0.4f, dmg * 0.3, 
			mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord, dmg * .5f);
	}
	
	private void doScatterAt(EntitySilbarn sb) {
		int nRays = (int) ((dmg * 0.04) * GenericUtils.randIntv(30, 38));
		for(int i = 0; i < nRays; ++i) {
			worldObj.spawnEntityInWorld(new EntityScatteredRay(this, sb));
		}
		this.removeDaemonHandler("trace");
		sb.onHitted();
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		seq.rebuild();
	}

	@Override
	public ResourceLocation[] getTexData() {
		return ACClientProps.ANIM_MD_RAY_L;
	}
	
	@SideOnly(Side.CLIENT)
	public static class RenderMD extends RendererRayTiling<EntityMeltDowner> {
		public RenderMD() {
			super(null);
			this.widthFp = 0.9;
			this.widthTp = 1.6;
		}
		
		protected ResourceLocation nextTexture(EntityMeltDowner ent, int i) {
			return ent.getTexData()[i == 0 ? 0 : ent.seq.get(i % ent.seq.size()) + 1];
		}
	}

}
