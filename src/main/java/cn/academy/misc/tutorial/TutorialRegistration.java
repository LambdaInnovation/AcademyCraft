package cn.academy.misc.tutorial;

import cn.academy.misc.tutorial.ACTutorialUtils.RegTutorial;
import cn.annoreg.base.RegistrationFieldSimple;
import cn.annoreg.core.LoadStage;

public class TutorialRegistration extends RegistrationFieldSimple<RegTutorial, ACTutorial> {

	public TutorialRegistration(){
		super(RegTutorial.class, "Tutorial");
		this.setLoadStage(LoadStage.INIT);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void register(ACTutorial value, RegTutorial anno, String field) throws Exception {
		// TODO Auto-generated method stub
		if(value==null)
			value=new ACTutorial(field);
		value.regist();
	}

}
