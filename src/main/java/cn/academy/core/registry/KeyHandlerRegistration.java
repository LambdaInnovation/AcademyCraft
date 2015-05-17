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
package cn.academy.core.registry;

import cn.academy.core.util.KeyHandler;
import cn.academy.core.util.KeyManager;
import cn.annoreg.base.RegistrationFieldSimple;
import cn.annoreg.core.LoadStage;
import cn.annoreg.core.RegistryTypeDecl;

/**
 * @author WeAthFolD
 *
 */
@RegistryTypeDecl
public class KeyHandlerRegistration extends RegistrationFieldSimple<RegKeyHandler, KeyHandler> {

	public KeyHandlerRegistration() {
		super(RegKeyHandler.class, "ACKeyHandler");
		setLoadStage(LoadStage.INIT);
	}

	@Override
	protected void register(KeyHandler value, RegKeyHandler anno, String field)
			throws Exception {
		KeyManager.instance.addKeyHandler(anno.name(), anno.desc(), anno.defaultKey(), value);
	}


}
