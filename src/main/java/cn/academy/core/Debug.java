package cn.academy.core;

public class Debug {
	
	public static void print(Object obj) {
		if(AcademyCraft.DEBUG_MODE)
			AcademyCraft.log.debug(obj);
	}
	
}
