/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.api.player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import cpw.mods.fml.relauncher.Side;
import cn.academy.api.player.lock.LockBase;
import cn.academy.api.player.lock.LockBase.LockType;
import cn.academy.api.player.lock.LockPosition;
import cn.academy.core.AcademyCraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

public class ControlData implements IExtendedEntityProperties {
	
	public static final String IDENTIFIER = "ac_control";
	
	public static LockType[] CONTROLS = new LockType[] {LockType.CONTROL_MOVE, LockType.CONTROL_JUMP};
	
	public static ControlData get(Entity entity) {
		return (ControlData) entity.getExtendedProperties(IDENTIFIER);
	}
	
	private EntityPlayer player = null;
	
	private Map<LockType, LockBase> lock = new HashMap<LockType, LockBase>();
	
	/*
	 * Set lock state(s)
	 * @param ticks Ticks to unlock the state and negative for infinite
	 */
	public void lockSet(LockType type, int ticks) {
		switch(type) {
		case ALL:
			for (LockType lt : LockType.values())
				doLockSet(lt, ticks);
			break;
		case CONTROL_ALL:
			for (LockType lt : CONTROLS)
				doLockSet(type, ticks);
			break;
		default:
			doLockSet(type, ticks);
			break;
		}
	}

	/*
	 * Modify lock state(s)
	 * @param ticks Positive for increase while negative for decrease
	 */
	public void lockModify(LockType type, int ticks) {
		switch(type) {
		case ALL:
			for (Entry<LockType, LockBase> e : lock.entrySet())
				doLockModify(e.getValue(), ticks);
			break;
		case CONTROL_ALL:
			for (LockType lt : CONTROLS)
				doLockModify(lock.get(lt), ticks);
			break;
		default:
			doLockModify(lock.get(type), ticks);
			break;
		}
	}
	
	/*
	 * Unlock something
	 */
	public void lockCancel(LockType type) {
		switch(type) {
		case ALL:
			lock.clear();
			break;
		case CONTROL_ALL:
			for (LockType lt : CONTROLS)
				lock.remove(lt);
			break;
		default:
			lock.remove(type);
			break;
		}
	}
	
	public void onTick(EntityPlayer player, Side side, boolean needSync) {
		for (Iterator<Entry<LockType, LockBase>> i = lock.entrySet().iterator(); i.hasNext();) {
			LockBase ls = i.next().getValue();
			if (ls.tick())
				i.remove();
			else
				ls.onTick(player);
		}
		if (needSync)
			sync();
	}
	
	private void doLockSet(LockType lt, int ticks) {
		LockBase ls = lock.get(lt);
		if (ls == null) {
			switch(lt) {
			case POSITION:
				break;
			case CONTROL_MOVE:
				break;
			case CONTROL_JUMP:
				break;
			default:
				AcademyCraft.log.warn("Not supported yet: " + lt);
			}
		}
		else
			ls.setTick(ticks);
			
	}
	
	private void doLockModify(LockBase ls, int ticks) {
		if (ls != null)
			ls.modifyTick(ticks);
	}
	
	private void sync() {
	}
	
	@Override
	public void saveNBTData(NBTTagCompound tag) {
		for (Entry<LockType, LockBase> e : lock.entrySet())
			e.getValue().saveNBTData(tag);
		AcademyCraft.log.info("nbtsave");
	}

	@Override
	public void loadNBTData(NBTTagCompound tag) {
		LockPosition lpos = new LockPosition(tag);
	}

	@Override
	public void init(Entity entity, World world) {
		if (!(entity instanceof EntityPlayer)) {
			AcademyCraft.log.warn("Registering ControlData for a(n)" + entity.getClass().getName());
			return;
		}
		player = (EntityPlayer) entity;
	}

}
