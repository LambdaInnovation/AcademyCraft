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
package cn.academy.knowledge;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import cn.academy.core.AcademyCraft;
import cn.academy.core.registry.RegDataPart;
import cn.academy.core.util.DataPart;
import cn.academy.core.util.PlayerData;
import cn.academy.knowledge.event.KnowledgeLearnedEvent;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.DataSerializer;
import cn.annoreg.mc.s11n.SerializationManager;
import cn.annoreg.mc.s11n.StorageOption;
import cn.annoreg.mc.s11n.StorageOption.Data;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;

import cpw.mods.fml.relauncher.Side;

/**
 * @author WeAthFolD
 *
 */
@Registrant
@RegDataPart("knowledge")
public class KnowledgeData extends DataPart {
	
	//STATIC REGISTRY PART
	private static List<Knowledge> knowledgeList = new ArrayList();
	
	private static BiMap<String, Integer> idMap = HashBiMap.create();
	
	public static KnowledgeData get(EntityPlayer player) {
		return PlayerData.get(player).getPart(KnowledgeData.class);
	}
	
	public static List<Knowledge> getKnowledgeList() {
		return ImmutableList.copyOf(knowledgeList);
	}
	
	public static void addKnowledge(Knowledge k) {
		if(idMap.containsKey(k.name)) {
			throw new RuntimeException("Duplicating knowledge" + k.name);
		}
		idMap.put(k.name, knowledgeList.size());
		knowledgeList.add(k);
	}
	
	public static void addKnowledges(Knowledge... ks) {
		for(Knowledge k : ks)
			addKnowledge(k);
	}
	
	/**
	 * Initialize the knowledges using the standard class.
	 */
	public static void addKnowledges(String ...ss) {
		for(String s : ss) {
			Knowledge k = new Knowledge(s);
			addKnowledge(k);
		}
	}
	
	public static Knowledge getKnowledge(String name) {
		Integer i = idMap.get(name);
		return i == null ? null : getKnowledge(i);
	}
	
	public static boolean hasKnowledge(String name) {
		return idMap.containsKey(name);
	}
	
	public static Knowledge getKnowledge(int id) {
		return knowledgeList.size() > id ? knowledgeList.get(id) : null;
	}
	
	//------
	
	static DataSerializer<BitSet> bitsetSer = SerializationManager.INSTANCE.getDataSerializer(BitSet.class);
	
	BitSet learned;
	
	public KnowledgeData() {
		learned = new BitSet(knowledgeList.size());
	}
	
	public int getLearnedCount() {
		return learned.cardinality();
	}
	
	/**
	 * You should NOT modify the result value, for this would cause sync loss.
	 */
	public BitSet getBitmask() {
		return learned;
	}
	
	/**
	 * See desc of learn(int id).
	 */
	public void learn(String name) {
		Integer i = idMap.get(name);
		if(i != null) {
			learn(i);
		}
	}
	
	/**
	 * Acquire the knowledge. will only be useful in SERVER.
	 * If the knowledge is not previously acquired, this is a effective call,
	 * and will trigger a KnowledgeAcquiredEvent in both CLIENT and SERVER.
	 */
	public void learn(int id) {
		if(!isLearned(id)) {
			if(!isRemote()) {
				doLearnKnowledge(id);
				learnedKnowledge(id);
			}
		}
	}
	
	public void unlearn(String name) {
		Integer i = idMap.get(name);
		if(i != null) {
			unlearn(i);
		}
	}
	
	/**
	 * Unlearn some knowledge, debug only, should only call in SERVER.
	 */
	public void unlearn(int id) {
		learned.set(id, false);
		plainSync(learned);
	}
	
	/**
	 * Learn all knowledges, debug only, should only call in SERVER.
	 */
	public void learnAll() {
		learned.set(0, knowledgeList.size(), true);
		plainSync(learned);
	}
	
	/**
	 * Unlearn all knowledges, debug only, should only call in SERVER.
	 */
	public void unlearnAll() {
		learned.set(0, knowledgeList.size(), false);
		plainSync(learned);
	}
	
	public boolean isLearned(String name) {
		Integer id = idMap.get(name);
		if(id == null) {
			AcademyCraft.log.warn("Querying invalid knowledge " + name);
		}
		return isLearned(id);
	}
	
	public boolean isLearned(int id) {
		return learned.size() > id && learned.get(id);
	}
	
	public void fromNBT(NBTTagCompound tag) {
		try {
			learned = bitsetSer.readData(tag, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public NBTTagCompound toNBT() {
		try {
			return (NBTTagCompound) bitsetSer.writeData(learned);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void tick() {
	}
	
	@RegNetworkCall(side = Side.CLIENT, thisStorage = StorageOption.Option.INSTANCE)
	private void plainSync(@Data BitSet bs) {
		if(this != null)
			this.learned = bs;
	}
	
	@RegNetworkCall(side = Side.CLIENT, thisStorage = StorageOption.Option.INSTANCE)
	private void learnedKnowledge(@Data Integer id) {
		if(this != null)
			doLearnKnowledge(id);
	}
	
	private void doLearnKnowledge(int id) {
		learned.set(id, true);
		System.out.println("DoLearnKnowledge in " + this.getPlayer().worldObj.isRemote + ", " + learned);
		MinecraftForge.EVENT_BUS.post(new KnowledgeLearnedEvent(getPlayer(), id));
	}

}
