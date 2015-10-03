package cn.academy.misc.tutorial2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import cn.liutils.util.helper.DataPart;
import net.minecraft.nbt.NBTTagCompound;

public class ACTutorial {
	static HashMap<String,ACTutorial> tutorials=new HashMap<String,ACTutorial>();
	String id;
	List<Condition> conditions = new ArrayList<Condition>();
	
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
	
	public static void addConditions(String TutorialID,Condition...c) throws Exception{
		try {
			tutorials.get(TutorialID).addConditions(c);
		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			throw new Exception("No such a tutorial:"+TutorialID);
		}
	}
	
	public static String getKey(ACTutorial t){
		return getKey(t.id);
	}
	
	public static String getKey(String id){
		return "ac.gui.tutorial."+id;
	}
	
	
	public static class ACTutorialDataPart extends DataPart{
		public void update(Condition c,boolean b){
			for(ACTutorial t : tutorials.values()){
				for(Condition c0 : t.conditions){
					if(c0.equals(c))c0.update(b);
				}
			}
		}

		@Override
		public void fromNBT(NBTTagCompound tag) {
			// TODO Auto-generated method stub
			for(ACTutorial t : tutorials.values()){
				NBTTagCompound tag0 = (NBTTagCompound) tag.getTag(t.id);
				int i=0;
				for(Condition c : t.conditions){
					try {
						c.update(tag0.getBoolean(String.valueOf(i++)));
					} catch (Exception e) {
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
				for(Condition c : t.conditions){
					tag0.setBoolean(String.valueOf(i++), c.exam());
				}
				tag.setTag(t.id,tag0);;
			}
			return tag;
		}
		
	}
	
	
	
	public void addConditions(Condition...c){
		this.conditions.addAll(Arrays.asList(c));
	}
	
	public boolean getIsLoad(){
		boolean b = true;
		for(Condition c : conditions)b &= c.exam();
		return b;
	}
}
