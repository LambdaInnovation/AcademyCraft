/**
 * Copyright (C) Lambda-Innovation, 2013-2014
 * This code is open-source. Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer. 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 */
package cn.academy.misc.block.dev;

import cn.misaka.support.block.ad.IADModule;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 需要显式连接到AD上，让AD接管渲染的子模块类型。
 * @author WeAthFolD
 */
public interface IADModuleAttached extends IADModule {
	@SideOnly(Side.CLIENT)
	void renderAtOrigin();
	
	ItemStack getDrop();
}
