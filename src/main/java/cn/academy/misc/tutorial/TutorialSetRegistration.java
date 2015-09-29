package cn.academy.misc.tutorial;

import java.lang.reflect.Field;

import cn.academy.misc.tutorial.ACTutorialUtils.RegTutorialSet;
import cn.annoreg.base.RegistrationClassSimple;
import cn.annoreg.core.LoadStage;
import cn.annoreg.core.RegistryTypeDecl;

@RegistryTypeDecl
public class TutorialSetRegistration extends RegistrationClassSimple<RegTutorialSet,Object> {

	public TutorialSetRegistration(){
		super(RegTutorialSet.class, "TutorialSet");
		this.setLoadStage(LoadStage.INIT);
		// TODO Auto-generated constructor stub
	}
	/**
	 * 遍历一个类里所有声明的成员变量，若判断其为{@link ACTutorial},则尝试对其进行注册
	 * 如果要是此成员变量为未初始化的成员，则会以成员名为{@link ACTutorial#name}创建其对象并注册
	 * 
	 * 不保证现在就能正常工作QwQ
	 */
	@Override
	protected void register(Class clazz, RegTutorialSet anno) throws Exception {
		// TODO Auto-generated method stub
		Field[] fields=clazz.getDeclaredFields();
		for(Field f:fields){
			if(f.getType().isAssignableFrom(ACTutorial.class)){
				ACTutorial t=(ACTutorial)f.get(null);
				if(t==null)t=new ACTutorial(f.getName());
				t.regist();
			}
		}
	}

	

}
