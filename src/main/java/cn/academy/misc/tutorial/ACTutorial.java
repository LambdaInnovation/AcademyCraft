package cn.academy.misc.tutorial;

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
	private static HashMap<String,ACTutorial> tutorials=new HashMap<String,ACTutorial>();
	private static final ACTutorialDataPart data = new ACTutorialDataPart();
	String id;
	static List<Condition> savedConditions = new ArrayList<Condition>();
	
	List<Condition> conditions = new ArrayList<Condition>();
	
	public ACTutorial(){
		
	}
	
	public ACTutorial(String id){
		this.id=id;
	}
	
	public static void addTutorials(ACTutorial...tutorial) throws Exception{
		for(ACTutorial t : tutorial){
			if(tutorials.containsKey(t.id))throw new Exception("Already　has a tutorial with this id:"+t.id);
			tutorials.put(t.id, t);
		}
	}

	public static ACTutorial addTutorial(String string) throws Exception {
		// TODO Auto-generated method stub
		ACTutorial t=new ACTutorial(string);
		addTutorials(t);
		return t;
	}
	
	public static void addTutorials(String...string) throws Exception {
		// TODO Auto-generated method stub
		ACTutorial[] acTu=new ACTutorial[string.length];
		int i = 0;
		for(String s : string){
			acTu[i] = new ACTutorial(s);
			i++;
		}
		addTutorials(acTu);
	}
	
	public ACTutorial addConditions(Condition...conditions){
		for(Condition c : conditions){
			this.conditions.add(c);
			c.addNeedSavingToTutorial(this);
		}
		return this;
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
		List<Boolean> allSaved;
		
		void init(){
			allSaved = new ArrayList<Boolean>();
			for(Condition c : ACTutorial.savedConditions){
				allSaved.add(c.exam(this.getPlayer()));
			}
		}
		
		public void update(){
			sync();
		}
		
		@Override
		public void fromNBT(NBTTagCompound tag) {
			// TODO Auto-generated method stub
			if(allSaved==null)init();
			Set<String> set=tag.func_150296_c();
			for(String s : set){
				try {
					allSaved.set(Integer.parseInt(s), tag.getBoolean(s));
				} catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		@Override
		public NBTTagCompound toNBT() {
			// TODO Auto-generated method stub
			NBTTagCompound tag = new NBTTagCompound();
			for(int i=0;i<allSaved.size();i++){
				tag.setBoolean(String.valueOf(i), allSaved.get(i));
			}
			return tag;
		}
		
	}
	
	public boolean getIsLoad(EntityPlayer player){
		boolean b = true;
		if(b)for(Condition b0 : conditions)
			if(!b0.exam(player)){
				b = false;
				break;
			}
		
		return b;
	}

}
