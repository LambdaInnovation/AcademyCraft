/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.vanilla.meltdowner.skill;

import static cn.liutils.util.generic.RandUtils.ranged;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.ActionManager;
import cn.academy.ability.api.ctrl.Cooldown;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.action.SkillSyncAction;
import cn.academy.core.AcademyCraft;
import cn.academy.core.client.sound.ACSounds;
import cn.academy.core.client.sound.FollowEntitySound;
import cn.academy.vanilla.meltdowner.client.render.MdParticleFactory;
import cn.annoreg.mc.s11n.InstanceSerializer;
import cn.annoreg.mc.s11n.SerializationManager;
import cn.liutils.entityx.handlers.Rigidbody;
import cn.liutils.render.particle.Particle;
import cn.liutils.util.generic.RandUtils;
import cn.liutils.util.generic.VecUtils;
import cn.liutils.util.mc.EntitySelectors;
import cn.liutils.util.raytrace.Raytrace;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 */
public abstract class MineRaysBase extends Skill {
	
	protected ResourceLocation particleTexture;
	
	final String postfix;

	public MineRaysBase(String _postfix, int atLevel) {
		super("mine_ray_" + _postfix, atLevel);
		postfix = _postfix;
	}
	
	protected abstract void onBlockBreak(World world, int x, int y, int z, Block block);
	
	@SideOnly(Side.CLIENT)
	protected abstract Entity createRay(EntityPlayer player);
	
	@Override
	public final SkillInstance createSkillInstance(EntityPlayer player) {
		return new SkillInstance().addChild(new MRAction(this));	
	}
	
	public static class MRAction extends SkillSyncAction {

		InstanceSerializer<Skill> skillSer;
		
		MineRaysBase skill;
		
		int x = -1, y = -1, z = -1;
		float hardnessLeft = Float.MAX_VALUE;
		
		public MRAction(MineRaysBase _skill) {
			this();
			skill = _skill;
		}
		
		public MRAction() {
			super(5);
			skillSer = SerializationManager.INSTANCE.getInstanceSerializer(Skill.class);
		}
		
		@Override
		public void writeNBTStart(NBTTagCompound tag) {
			try {
				tag.setTag("s", skillSer.writeInstance(skill));
			} catch (Exception e) {
				AcademyCraft.log.error("skill serialization", e);
			}
		}
		
		@Override
		public void readNBTStart(NBTTagCompound tag) {
			try {
				skill = (MineRaysBase) skillSer.readInstance(tag.getTag("s"));
			} catch (Exception e) {
				AcademyCraft.log.error("skill deserialization", e);
			}
		}
		
		@Override
		public void onStart() {
			super.onStart();
			
			cpData.perform(skill.getOverload(aData), 0);
			if(isRemote)
				startEffects();
		}
		
		@Override
		public void onTick() {
			if(!cpData.perform(0, skill.getConsumption(aData)) && !isRemote)
				ActionManager.abortAction(this);
			
			MovingObjectPosition result = Raytrace.traceLiving(player, skill.getFloat("range"), EntitySelectors.nothing);
			if(result != null) {
				int tx = result.blockX, ty = result.blockY, tz = result.blockZ;
				if(tx != x || ty != y || tz != z) {
					Block block = world.getBlock(tx, ty, tz);
					if(block.getHarvestLevel(world.getBlockMetadata(x, y, z)) <= skill.getInt("harvest_level")) {
						x = tx; y = ty; z = tz;
						hardnessLeft = block.getBlockHardness(world, tx, ty, tz);
						
						if(hardnessLeft < 0)
							hardnessLeft = Float.MAX_VALUE;
					} else {
						x = y = z = -1;
					}
				} else {
					hardnessLeft -= skill.callFloatWithExp("speed", aData);
					if(hardnessLeft <= 0) {
						if(!isRemote) {
							skill.onBlockBreak(world, x, y, z, world.getBlock(x, y, z));
							aData.addSkillExp(skill, skill.getFloat("expincr"));
						}
						x = y = z = -1;
					}
					if(isRemote)
						spawnParticles();
				}
			} else {
				x = y = z = -1;
			}
			
			if(isRemote)
				updateEffects();
		}
		
		@Override
		public void onFinalize() {
			if(isRemote)
				endEffects();
			Cooldown.setCooldown(skill, skill.callIntWithExp("cooldown", aData));
		}
		
		// CLIENT
		@SideOnly(Side.CLIENT)
		static FollowEntitySound loopSound;
		
		Entity ray;
		
		@SideOnly(Side.CLIENT)
		public void startEffects() {
			world.spawnEntityInWorld(ray = skill.createRay(player));
			loopSound = new FollowEntitySound(player, "md.mine_loop").setLoop().setVolume(0.3f);
			ACSounds.playClient(loopSound);
			ACSounds.playClient(player, "md.mine_" + skill.postfix + "_startup", 0.4f);
		}
		
		@SideOnly(Side.CLIENT)
		public void updateEffects() {
			
		}
		
		@SideOnly(Side.CLIENT)
		public void spawnParticles() {
			for(int i = 0, max = RandUtils.rangei(2, 3); i < max; ++i) {
				double _x = x + ranged(-.2, 1.2),
						_y = y + ranged(-.2, 1.2),
						_z = z + ranged(-.2, 1.2);
				
				Particle p = MdParticleFactory.INSTANCE.next(world,
						VecUtils.vec(_x, _y, _z),
						VecUtils.vec(ranged(-.06, .06), ranged(-.06, .06), ranged(-.06, .06)));
				if(skill.particleTexture != null) {
					p.texture = skill.particleTexture;
				}
				
				p.needRigidbody = false;
				Rigidbody rb = new Rigidbody();
				rb.gravity = 0.01;
				rb.entitySel = null;
				rb.blockFil = null;
				p.addMotionHandler(rb);
				
				world.spawnEntityInWorld(p);
			}
		}
		
		@SideOnly(Side.CLIENT)
		public void endEffects() {
			ray.setDead();
			loopSound.stop();
		}
		
	}
}
