/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.core.register;

import java.lang.annotation.Annotation;

import cn.academy.api.data.AbilityData;
import cn.academy.api.data.ExtendedAbilityData;
import cn.annoreg.core.AnnotationData;
import cn.annoreg.core.LoadStage;
import cn.annoreg.core.RegistryType;
import cn.annoreg.core.RegistryTypeDecl;

/**
 * @author WeathFolD
 */
@RegistryTypeDecl
public class ExtendedDataRegistration extends RegistryType {
	
	public static final String ID = "ac_extended_data";

	public ExtendedDataRegistration() {
		super(RegExtendedData.class, ID);
		this.setLoadStage(LoadStage.INIT);
	}

	@Override
	public boolean registerClass(AnnotationData data) throws Exception {
		String id = (String) data.getTheClass().getField("IDENTIFIER").get(null);
		
		//System.out.println("ID to reg:  " + id);
		AbilityData.regData(id, (Class<? extends ExtendedAbilityData>) data.getTheClass());
		return true;
	}

	@Override
	public boolean registerField(AnnotationData data) throws Exception {
		return false;
	}

}
