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
package cn.academy.energy;

import java.io.IOException;

import org.apache.commons.io.IOUtils;

import cn.academy.core.registry.LoaderHelper;
import cn.academy.energy.block.BlockIonicFlux;
import cn.academy.energy.block.BlockMatrix;
import cn.academy.energy.block.BlockNode;
import cn.academy.energy.block.BlockNode.NodeType;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegBlock;
import cn.annoreg.mc.RegSubmoduleInit;
import cn.liutils.loading.item.ItemLoader;
import cn.liutils.template.block.ItemBlockMulti;

/**
 * @author WeAthFolD
 */
@Registrant
@RegSubmoduleInit
public class ModuleEnergy {

	public static ItemLoader loader;
	
    @RegBlock
    public static BlockNode
        nodeBasic = new BlockNode(NodeType.BASIC),
        nodeStandard = new BlockNode(NodeType.STANDARD),
        nodeAdvanced = new BlockNode(NodeType.ADVANCED);
    
    @RegBlock(item = ItemBlockMulti.class)
    public static BlockMatrix matrix = new BlockMatrix();
    
    @RegBlock
    public static BlockIonicFlux ionicFlux;
	
	public static void init() {
		loader = LoaderHelper.createItemLoader();
		try {
			loader.feed(IOUtils.toString(ModuleEnergy.class.getResource("/assets/academy/items_energy.json")));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		loader.loadAll();
	}
	
}
