package cn.academy.misc.tutorial;

import java.util.ArrayList;

public class ACTutorialList {
	static ArrayList<ACTutorial> list=new ArrayList<ACTutorial>();
	
	static void regist(ACTutorial t){
		t.id=list.size();
		list.add(t);
	}
	static void regist(String tutorial,boolean isPreLoaded){
		ACTutorial t=new ACTutorial(tutorial,isPreLoaded);
		regist(t);
	}
	static void regist(String tutorial){
		ACTutorial t=new ACTutorial(tutorial);
		regist(t);
	}
}
