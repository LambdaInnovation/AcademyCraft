package cn.academy.energy.util;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.api.item.IElectricItemManager;
import ic2.core.IC2;
import ic2.core.item.DamageHandler;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * All credits of this class' code goes to IC2 team.
 */
public class IC2DefaultEIManager implements IElectricItemManager {
	  public double charge(ItemStack stack, double amount, int tier, boolean ignoreTransferLimit, boolean simulate)
	  {
	    IElectricItem item = (IElectricItem)stack.getItem();

	    assert (item.getMaxCharge(stack) > 0.0D);

	    if ((amount < 0.0D) || (stack.stackSize > 1) || (item.getTier(stack) > tier)) return 0.0D;

	    if ((!ignoreTransferLimit) && (amount > item.getTransferLimit(stack))) amount = item.getTransferLimit(stack);

	    NBTTagCompound tNBT = StackUtil.getOrCreateNbtData(stack);
	    double newCharge = tNBT.getDouble("charge");

	    amount = Math.min(amount, item.getMaxCharge(stack) - newCharge);

	    if (!simulate) {
	      newCharge += amount;

	      if (newCharge > 0.0D) {
	        tNBT.setDouble("charge", newCharge);
	      } else {
	        tNBT.removeTag("charge");
	        if (tNBT.hasNoTags()) stack.setTagCompound(null);
	      }

	      stack.func_150996_a(newCharge > 0.0D ? item.getChargedItem(stack) : item.getEmptyItem(stack));

	      if ((stack.getItem() instanceof IElectricItem)) {
	        item = (IElectricItem)stack.getItem();
	        int maxDamage = DamageHandler.getMaxDamage(stack);

	        if (maxDamage > 2) {
	          DamageHandler.setDamage(stack, maxDamage - 1 - (int)Util.map(newCharge, item.getMaxCharge(stack), maxDamage - 2));
	        }
	        else
	          DamageHandler.setDamage(stack, 0);
	      }
	      else {
	        DamageHandler.setDamage(stack, 0);
	      }
	    }

	    return amount;
	  }

	  public double discharge(ItemStack stack, double amount, int tier, boolean ignoreTransferLimit, boolean externally, boolean simulate)
	  {
	    IElectricItem item = (IElectricItem)stack.getItem();

	    assert (item.getMaxCharge(stack) > 0.0D);

	    if ((amount < 0.0D) || (stack.stackSize > 1) || (item.getTier(stack) > tier)) return 0.0D;
	    if ((externally) && (!item.canProvideEnergy(stack))) return 0.0D;

	    if ((!ignoreTransferLimit) && (amount > item.getTransferLimit(stack))) amount = item.getTransferLimit(stack);

	    NBTTagCompound tNBT = StackUtil.getOrCreateNbtData(stack);
	    double newCharge = tNBT.getDouble("charge");

	    amount = Math.min(amount, newCharge);

	    if (!simulate) {
	      newCharge -= amount;

	      if (newCharge > 0.0D) {
	        tNBT.setDouble("charge", newCharge);
	      } else {
	        tNBT.removeTag("charge");
	        if (tNBT.hasNoTags()) stack.setTagCompound(null);
	      }

	      stack.func_150996_a(newCharge > 0.0D ? item.getChargedItem(stack) : item.getEmptyItem(stack));

	      if ((stack.getItem() instanceof IElectricItem)) {
	        item = (IElectricItem)stack.getItem();
	        int maxDamage = DamageHandler.getMaxDamage(stack);

	        if (maxDamage > 2) {
	          DamageHandler.setDamage(stack, maxDamage - 1 - (int)Util.map(newCharge, item.getMaxCharge(stack), maxDamage - 2));
	        }
	        else
	          DamageHandler.setDamage(stack, 0);
	      }
	      else {
	        DamageHandler.setDamage(stack, 0);
	      }
	    }

	    return amount;
	  }

	  public double getCharge(ItemStack itemStack)
	  {
	    return ElectricItem.manager.discharge(itemStack, (1.0D / 0.0D), 2147483647, true, false, true);
	  }

	  public boolean canUse(ItemStack itemStack, double amount)
	  {
	    return ElectricItem.manager.getCharge(itemStack) >= amount;
	  }

	  public boolean use(ItemStack itemStack, double amount, EntityLivingBase entity)
	  {
	    ElectricItem.manager.chargeFromArmor(itemStack, entity);

	    double transfer = ElectricItem.manager.discharge(itemStack, amount, 2147483647, true, false, true);

	    if (Util.isSimilar(transfer, amount)) {
	      ElectricItem.manager.discharge(itemStack, amount, 2147483647, true, false, false);
	      ElectricItem.manager.chargeFromArmor(itemStack, entity);

	      return true;
	    }
	    return false;
	  }

	  public void chargeFromArmor(ItemStack itemStack, EntityLivingBase entity)
	  {
	    boolean transferred = false;

	    for (int i = 0; i < 4; i++) {
	      ItemStack armorItemStack = entity.getEquipmentInSlot(i + 1);
	      if (armorItemStack != null)
	      {
	        int tier;
	        if ((armorItemStack.getItem() instanceof IElectricItem))
	          tier = ((IElectricItem)armorItemStack.getItem()).getTier(itemStack);
	        else {
	          tier = 2147483647;
	        }

	        double transfer = ElectricItem.manager.discharge(armorItemStack, (1.0D / 0.0D), 2147483647, true, true, true);
	        if (transfer > 0.0D)
	        {
	          transfer = ElectricItem.manager.charge(itemStack, transfer, tier, true, false);
	          if (transfer > 0.0D)
	          {
	            ElectricItem.manager.discharge(armorItemStack, transfer, 2147483647, true, true, false);
	            transferred = true;
	          }
	        }
	      }
	    }
	    if ((transferred) && ((entity instanceof EntityPlayer)) && (IC2.platform.isSimulating()))
	      ((EntityPlayer)entity).openContainer.detectAndSendChanges();
	  }

	  public String getToolTip(ItemStack itemStack)
	  {
	    double charge = ElectricItem.manager.getCharge(itemStack);
	    double space = ElectricItem.manager.charge(itemStack, (1.0D / 0.0D), 2147483647, true, true);

	    return Util.toSiString(charge, 3) + "/" + Util.toSiString(charge + space, 3) + " EU";
	  }
}