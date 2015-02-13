package cn.academy.ability.teleport.skill;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.PatternHold;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.core.register.ACItems;
import cn.academy.misc.item.ItemNeedle;
import cn.annoreg.core.RegistrationClass;
import cn.liutils.util.GenericUtils;

@RegistrationClass
public class SkillThreateningTele extends SkillBase {

	private static SkillThreateningTele instance;

	public SkillThreateningTele() {
		instance = this;
		setLogo("teleport/threatening.png");
		setName("tp_threatening");
	}

	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternHold(1000) {

			@Override
			public State createSkill(EntityPlayer player) {
				return new ThreateningState(player);
			}

		});
	}

	private static class ThreateningState extends PatternHold.State {

		public ThreateningState(EntityPlayer player) {
			super(player);
		}

		@Override
		public void onStart() {
			ItemStack stack = player.getCurrentEquippedItem();
			if(stack == null|| stack.getItem() == ACItems.ivoid || stack.getItem() instanceof ItemBlock) {
				return;
			}
			//TODO: How to cancel the skill?
			//Wea: this.finishSkill(); is fine
		}

		@Override
		public void onFinish() {
			ItemStack stack = player.getCurrentEquippedItem();
			if(stack == null|| stack.getItem() == ACItems.ivoid || stack.getItem() instanceof ItemBlock) {
				return;
			}
			if(!player.capabilities.isCreativeMode) {
				--stack.stackSize;
			}

			AbilityData data = AbilityDataMain.getData(player);
			double dist = 10 + data.getSkillLevel(data.getSkillID(instance)) * 2 + data.getLevelID() * 3;
			float ccp = 250F - data.getSkillLevel(data.getSkillID(instance)) * 10F + data.getLevelID() * 15F;

			data.decreaseCP(ccp);

			MovingObjectPosition mop = GenericUtils.tracePlayer(player, dist);
			if(mop != null && mop.typeOfHit == MovingObjectType.ENTITY && mop.entityHit instanceof EntityLiving) {
				//hit entity
				float damage = 1F - data.getSkillLevel(data.getSkillID(instance)) * .5F + data.getLevelID() * .8F;
				if (stack.getItem() instanceof ItemNeedle) {
					damage *= 1.5F;
				}
				mop.entityHit.attackEntityFrom(DamageSource.causePlayerDamage(player), damage);
				if(isRemote()) {
					//Add some FX?
				}
			} else {
				//drop item;
				if(!isRemote()) {
					double dropDist = dist * GenericUtils.randIntv((double)0, (double)1);
					MovingObjectPosition dropMop = GenericUtils.tracePlayer(player, dropDist);
					double dropX = dropMop != null ? dropMop.blockX : player.posX;
					double dropY = dropMop != null ? dropMop.blockY : player.posY;
					double dropZ = dropMop != null ? dropMop.blockZ : player.posZ;

					ItemStack newItemStack = new ItemStack(stack.getItem(), 1);
					EntityItem entityitem = new EntityItem(player.worldObj, dropX, dropY, dropZ, newItemStack);
					entityitem.delayBeforeCanPickup = 10;
					player.worldObj.spawnEntityInWorld(entityitem);
				}
			}
		}

		@Override
		public void onHold() {

		}

	}

	public int getMaxSkillLevel() {
		return 10;
	}

}
