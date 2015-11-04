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

import cn.academy.core.ModuleCoreClient;
import cn.lambdalib.annoreg.base.RegistrationFieldSimple;
import cn.lambdalib.annoreg.core.LoadStage;
import cn.lambdalib.annoreg.core.RegistryTypeDecl;
import cn.lambdalib.util.key.KeyHandler;
import cn.lambdalib.util.key.KeyManager;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 *
 */
@RegistryTypeDecl
@SideOnly(Side.CLIENT)
public class KeyHandlerRegistration extends RegistrationFieldSimple<RegACKeyHandler, KeyHandler> {

	public KeyHandlerRegistration() {
		super(RegACKeyHandler.class, "ACKeyHandler");
		setLoadStage(LoadStage.INIT);
	}

	@Override
	protected void register(KeyHandler value, RegACKeyHandler anno, String field)
			throws Exception {
		KeyManager target = anno.dynamic() ? ModuleCoreClient.dynKeyManager : ModuleCoreClient.keyManager;
		target.addKeyHandler(anno.name(), anno.desc(), anno.defaultKey(), value);
	}

}
