package cn.academy.misc.tutorial2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import cn.annoreg.core.Registrant;
import cn.liutils.registry.RegDataPart;
import cn.liutils.util.helper.DataPart;
import cn.liutils.util.helper.PlayerData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

@Registrant
public class ACTutorial {
	static HashMap<String,ACTutorial> tutorials=new HashMap<String,ACTutorial>();
	private static final ACTutorialDataPart data = new ACTutorialDataPart();
	String id;
	List<Boolean> andConditions = new ArrayList<Boolean>();
	List<Boolean> orConditions = new ArrayList<Boolean>();
	public static void addTutorials(ACTutorial...tutorial) throws Exception{
		for(ACTutorial t : tutorial){
			if(tutorials.containsKey(t.id))throw new Exception("Already　has a tutorial with this id:"+t.id);
			tutorials.put(t.id, t);
		}
	}

	public static void addTutorials(String...string) throws Exception {
		// TODO Auto-generated method stub
		ACTutorial[] acTu=new ACTutorial[string.length];
		int i = 0;
		for(String s : string){
			acTu[i] = new ACTutorial();
			acTu[i].id = s;
			i++;
		}
		addTutorials(acTu);
	}
	
	public static String getKey(ACTutorial t){
		return getKey(t.id);
	}
	
	public static String getKey(String id){
		return "ac.gui.tutorial."+id;
	}
	
	public static ACTutorial getTutorial(String s) throws Exception{
		ACTutorial t;
		if(!tutorials.containsKey(s))throw new Exception("No such a tutorial;");
		t=tutorials.get(s);
		return t;
	}
	
	@RegDataPart("ACTutorial")
	static class ACTutorialDataPart extends DataPart{
		public void update(){
			sync();
		}
		
		@Override
		public void fromNBT(NBTTagCompound tag) {
			// TODO Auto-generated method stub
			for(ACTutorial t : tutorials.values()){
				NBTTagCompound tag0 = (NBTTagCompound) tag.getTag(t.id);
				Set<String> set=tag0.func_150296_c();
				for(String s : set){
					try {
						if(s.startsWith("AND")){
							int i=Integer.parseInt(s.substring(3));
							t.andConditions.set(i, tag0.getBoolean(s));
						}else if(s.startsWith("OR")){
							int i=Integer.parseInt(s.substring(2));
							t.orConditions.set(i, tag0.getBoolean(s));
						}
					} catch (NumberFormatException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}

		@Override
		public NBTTagCompound toNBT() {
			// TODO Auto-generated method stub
			NBTTagCompound tag = new NBTTagCompound();
			for(ACTutorial t : tutorials.values()){
				NBTTagCompound tag0 = new NBTTagCompound();
				int i=0;
				for(Boolean b : t.andConditions){
					tag0.setBoolean("AND"+i++, b);
				}
				i=0;
				for(Boolean b : t.orConditions){
					tag0.setBoolean("OR"+i++, b);
				}
				tag.setTag(t.id, tag0);
			}
			return tag;
		}
		
	}
	
	public boolean getIsLoad(){
		boolean b = true;

		for(Boolean b0 : orConditions)
			if(b0){
				b = true;
				break;
			}else{
				b = false;
			}
		
		if(b)for(Boolean b0 : andConditions)
			if(!b0){
				b = false;
				break;
			}
		
		return b;
	}

	public void update(EntityPlayer p,int i){
		this.andConditions.set(i,Boolean.TRUE);
		PlayerData.get(p).getPart(ACTutorialDataPart.class).update();
	}
}
