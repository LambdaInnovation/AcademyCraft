/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
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
