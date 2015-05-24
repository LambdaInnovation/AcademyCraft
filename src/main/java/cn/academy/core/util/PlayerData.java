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
package cn.academy.core.util;

import java.util.Map.Entry;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import cn.academy.core.AcademyCraft;
import cn.academy.core.proxy.ProxyHelper;
import cn.academy.core.proxy.ThreadProxy;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.SideHelper;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.InstanceSerializer;
import cn.annoreg.mc.s11n.RegSerializable;
import cn.annoreg.mc.s11n.StorageOption;
import cn.annoreg.mc.s11n.StorageOption.Data;
import cn.liutils.util.client.ClientUtils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The environment provider and handler of DataPart. <br/>
 * It is recommended not to access this class explicitly, but create
 * some static get method in each DataPart class.
 * @author WeAthFolD
 */
@Registrant
@RegSerializable(instance = PlayerData.Serializer.class)
public abstract class PlayerData implements IExtendedEntityProperties {
	
	private static String IDENTIFIER = "ac_playerData";
	
	static BiMap<String, Class<? extends DataPart> > staticParts = HashBiMap.create();
	
	BiMap<String, DataPart> constructed = HashBiMap.create();
	
	public static void register(String name, Class<? extends DataPart> clazz) {
		staticParts.put(name, clazz);
	}
	
	/**
	 * Do NOT modify this field!
	 */
	public EntityPlayer player;
	
	PlayerData(EntityPlayer player) {
		this.player = player;
		
		try {
			constructData();
		} catch(Exception e) {
			AcademyCraft.log.error("Error constructing DataPart");
			e.printStackTrace();
		}
	}
	
	private void constructData() throws InstantiationException, IllegalAccessException {
		for(Entry< String, Class<? extends DataPart> > s : staticParts.entrySet()) {
			String name = s.getKey();
			Class<? extends DataPart> clazz = s.getValue();
			
			DataPart dp = clazz.newInstance();
			dp.data = this;
			constructed.put(name, dp);
		}
	}
	
	protected void tick() {
		for(DataPart p : constructed.values()) {
			p.tick();
		}
	}

	@Override
	public void init(Entity entity, World world) {
		player = (EntityPlayer) entity;
	}
	
	public String getName(DataPart part) {
		return constructed.inverse().get(part);
	}
	
	public <T extends DataPart> T getPart(String name) {
		return (T) constructed.get(name);
	}
	
	public <T extends DataPart> T getPart(Class<T> clazz) {
		String id = staticParts.inverse().get(clazz);
		return (T) constructed.get(id);
	}
	
	public static PlayerData get(EntityPlayer player) {
		return ProxyHelper.get().getPlayerData(player);
	}
	
	@Override
	public void loadNBTData(NBTTagCompound tag) {
		for(DataPart p : constructed.values()) {
			String name = getName(p);
			NBTTagCompound t = (NBTTagCompound) tag.getTag(name);
			if(t != null) {
				p.fromNBT(t);
				p.dirty = false;
			}
		}
	}
	
	@RegNetworkCall(side = Side.SERVER, thisStorage = StorageOption.Option.INSTANCE)
	protected void receivedSyncQuery() {
		if(this != null) {
			System.out.println("ReceivedSyncQuery");
			NBTTagCompound tag = new NBTTagCompound();
			this.saveNBTData(tag);
			this.receiveSync(tag);
		}
	}
	
	@RegNetworkCall(side = Side.CLIENT, thisStorage = StorageOption.Option.INSTANCE)
	protected void receiveSync(@Data NBTTagCompound tagAll) {
		if(this != null) {
			System.out.println("ReceivedSync");
			this.loadNBTData(tagAll);
		}
	}
	
	public static class Client extends PlayerData {

		int syncTicker;
		
		public Client(EntityPlayer player) {
			super(player);
			System.out.println("Constructed in client");
		}

		@Override
		protected void tick() {
			//System.out.println(syncTicker);
			if(syncTicker == 0) {
				syncTicker = 20;
				this.receivedSyncQuery();
			} else if(syncTicker > 0) {
				syncTicker--;
			}
			
			super.tick();
		}
		
		@Override
		public void saveNBTData(NBTTagCompound tag) {
			//Do nothing in client.
			System.out.println("SaveNBTData client");
		}

		@Override
		public void loadNBTData(NBTTagCompound tag) {
			super.loadNBTData(tag);
			System.out.println("LoadNBTData client");
			syncTicker = -1; //Received init, do not automatically sync any more.
		}
		
	}
	
	public static class Server extends PlayerData {

		public Server(EntityPlayer player) {
			super(player);
			System.out.println("Constructed in server");
		}

		@Override
		public void saveNBTData(NBTTagCompound tag) {
			for(DataPart p : constructed.values()) {
				NBTTagCompound ret = p.toNBT();
				tag.setTag(getName(p), ret);
			}
			
			System.out.println("SaveNBTData server");
		}
		
		@Override
		public void loadNBTData(NBTTagCompound tag) {
			System.out.println("LoadNBTData server");
			super.loadNBTData(tag);
		}
		
	}
	
	@RegEventHandler
	public static class Events {
		
		@SubscribeEvent
		@SideOnly(Side.CLIENT)
		public void onClientTick(ClientTickEvent event) {
			if(event.phase == Phase.END && ClientUtils.isPlayerInGame()) {
				EntityPlayer thePlayer = Minecraft.getMinecraft().thePlayer;
				ProxyHelper.get().getPlayerData(thePlayer).tick();
			}
		}
		
		@SubscribeEvent
		public void onServerTick(ServerTickEvent event) {
			if(event.phase == Phase.END) {
				ThreadProxy proxy = ProxyHelper.get();
				for(Object p : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
					EntityPlayer player = (EntityPlayer) p;
					proxy.getPlayerData(player).tick();
				}
			}
		}
		
		@SubscribeEvent
		public void onEntityConstructed(EntityConstructing event) {
			if(FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER && event.entity instanceof EntityPlayer) {
				//A hack that forces the data to be loaded once the entity is constructed.
				//loadNBT will only be called if we do so.
				ProxyHelper.get().getPlayerData((EntityPlayer) event.entity);
			}
		}
		
	}
	
	public static class Serializer implements InstanceSerializer<PlayerData> {

		@Override
		public PlayerData readInstance(NBTBase nbt) throws Exception {
			int[] ids = ((NBTTagIntArray) nbt).func_150302_c();
			World world = SideHelper.getWorld(ids[0]);
			if (world != null) {
				Entity ent = world.getEntityByID(ids[1]);
				if(ent instanceof EntityPlayer) {
					return ProxyHelper.get().getPlayerData((EntityPlayer) ent);
				}
			}
			return null;
		}

		@Override
		public NBTBase writeInstance(PlayerData obj) throws Exception {
			EntityPlayer ent = obj.player;
			return new NBTTagIntArray(new int[] { ent.dimension, ent.getEntityId() });
		}
		
	}

}
