package cn.academy.medicine.buff;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import cn.academy.ability.api.data.CPData;
import cn.lambdalib.util.deprecated.LIFMLGameEventDispatcher;
import cn.lambdalib.util.deprecated.LIHandler;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;

public class BuffAllergic extends PotionEffect {
	boolean onCombine=false;
	public int level;
	
	public BuffAllergic(int duration) {
		super(0,duration);
		Constructor c;
		try {
			c = Potion.class.getDeclaredConstructor(int.class,boolean.class,int.class);
			c.setAccessible(true);
			Potion.potionTypes[0] = (Potion) c.newInstance(0,true,0x0099FF);
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.setCurativeItems(new ArrayList<ItemStack>());
	}
	
	private static class BuffHandler extends LIHandler<PlayerTickEvent>{
		final EntityPlayer p;
		int ticks = 10*60*20;
		int ticker = 0;
		
		public BuffHandler(EntityPlayer p) {
			this.p=p;
		}
		
		@Override
		protected boolean onEvent(PlayerTickEvent event) {
			if (p.isDead || (++ticker == ticks)) {
				this.setDead();
			} else {
				CPData.get(p).perform(0, 0);
			}
			return true;
		}
		
	}
	
	@Override
	public void combine(PotionEffect efx) {
		super.combine(efx);
		this.level++;
		this.onCombine=true;
	}
	@Override
	public boolean onUpdate(EntityLivingBase p_76455_1_)
    {
		super.onUpdate(p_76455_1_);
        if (this.getDuration() > 0)
        {
            this.performEffect(p_76455_1_);
        }

        return this.getDuration() > 0;
    }
	
	@Override
	public void performEffect(EntityLivingBase entity) {
		if(!(entity instanceof EntityPlayer))
			return;

		EntityPlayer p;
		p = (EntityPlayer) entity;
		if(onCombine){
			switch(level){
				case 1:
					p.addPotionEffect(new PotionEffect(Potion.hunger.id, 3*20));
					p.attackEntityFrom(DamageSource.magic, Math.min(3, entity.getHealth()-1));
					break;
				case 2:
					p.addPotionEffect(new PotionEffect(Potion.blindness.id, 15*20));
					p.attackEntityFrom(DamageSource.magic, 5);
					break;
				case 3:
					p.attackEntityFrom(DamageSource.magic, 10);
					CPData.get(p).perform(80, 0);
					LIFMLGameEventDispatcher.INSTANCE.registerPlayerTick(new BuffHandler(p));
					break;
				case 4:
					p.setDead();
					break;
			}
			onCombine=false;
		}
	}
}
