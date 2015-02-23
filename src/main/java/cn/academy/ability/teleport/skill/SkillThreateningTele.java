package cn.academy.ability.teleport.skill;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import cn.academy.ability.electro.CatElectro;
import cn.academy.ability.teleport.CatTeleport;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.SkillState;
import cn.academy.api.ctrl.pattern.PatternDown;
import cn.academy.api.ctrl.pattern.PatternHold;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.core.register.ACItems;
import cn.academy.misc.item.ItemNeedle;
import cn.annoreg.core.RegistrationClass;
import cn.liutils.util.GenericUtils;
import cn.liutils.util.space.Motion3D;

@RegistrationClass
public class SkillThreateningTele extends SkillBase {

	private static SkillThreateningTele instance;

	public SkillThreateningTele() {
		instance = this;
		setLogo("tp/threatening.png");
		setName("tp_threatening");
		setMaxLevel(10);
	}

	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternDown() {

			@Override
			public SkillState createSkill(EntityPlayer player) {
				return new ThreateningState(player);
			}

		});
	}

	private static class ThreateningState extends SkillState {

		public ThreateningState(EntityPlayer player) {
			super(player);
		}

		@Override
		public void onStart() {
			ItemStack stack = player.getCurrentEquippedItem();
			if(stack == null|| stack.getItem() == ACItems.ivoid || stack.getItem() instanceof ItemBlock) {
				return;
			}

			AbilityData data = AbilityDataMain.getData(player);
			int slv = data.getSkillLevel(data.getSkillID(instance)), lv = data.getLevelID() + 1;
			double dist = 5 + slv * .5 + lv * 1;
			float ccp = 350F - slv * 10F + lv * 125F;
			if(!data.decreaseCP(ccp, instance)) {
				return;
			}
			player.worldObj.playSoundAtEntity(player, "academy:tp.tp", .5f, 1f);

			MovingObjectPosition mop = GenericUtils.tracePlayerWithEntities(player, dist, null);
			if(mop != null && mop.typeOfHit == MovingObjectType.ENTITY) {
				//hit entity
				float damage = 1F + slv * .5F + lv * .8F;
				if (stack.getItem() instanceof ItemNeedle) {
					damage *= 2F;
				}
				mop.entityHit.attackEntityFrom(DamageSource.causePlayerDamage(player), damage);
				if(!isRemote() && rand.nextDouble() < 0.2) {
					double len = mop.hitVec.distanceTo(Vec3.createVectorHelper(player.posX, player.posY, player.posZ));
					dropItem(stack, len);
				}
			} else {
				//drop item at the far end
				if(!isRemote()) {
					dropItem(stack, dist);
				}
			}
			
			if(!player.capabilities.isCreativeMode) {
				--stack.stackSize;
			}
		}
		
		private void dropItem(ItemStack stack, double dist) {
			Motion3D mo = new Motion3D(player, true).move(dist);
			
			ItemStack newItemStack = stack.copy();
			newItemStack.stackSize = 1;
			EntityItem entityitem = new EntityItem(player.worldObj, mo.posX, mo.posY, mo.posZ, newItemStack);
			entityitem.delayBeforeCanPickup = 10;
			player.worldObj.spawnEntityInWorld(entityitem);
		}

	}
	
}
