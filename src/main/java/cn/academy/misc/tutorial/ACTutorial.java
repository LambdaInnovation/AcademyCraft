package cn.academy.misc.tutorial;

import net.minecraft.util.StatCollector;

public class ACTutorial {
	int id=0;
	String name;
	boolean isLoaded;
	
	public ACTutorial(String name,boolean isPreLoaded){
		this.name=name;
		this.isLoaded=isPreLoaded;
	}
	
	public ACTutorial(String name){
		this(name,false);
	}
	
	public String getText(){
		return getTextByKey("ac.gui.tutorial."+this.name);
	}
	
	static String getTextByKey(String key){
		if(StatCollector.canTranslate(key))
			return StatCollector.translateToLocal(key);
		else
			//我随便写的
			return "Lang Missing!";
	}
	/**
	 * 将自己注册至{@link ACTutorialList#list}
	 */
	public void regist(){
		ACTutorialList.regist(this);
	}
}
