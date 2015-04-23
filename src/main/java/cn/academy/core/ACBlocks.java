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
package cn.academy.core;

import cn.academy.energy.block.BlockMatrix;
import cn.academy.energy.block.BlockMatrix.MatrixType;
import cn.academy.energy.block.BlockNode;
import cn.academy.energy.block.BlockNode.NodeType;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegBlock;

/**
 * All registration of blocks goes here.
 * @author WeathFolD
 */
@RegistrationClass
public class ACBlocks {

    @RegBlock
    public static BlockNode
        nodeBasic = new BlockNode(NodeType.BASIC),
        nodeStandard = new BlockNode(NodeType.STANDARD),
        nodeAdvanced = new BlockNode(NodeType.ADVANCED);
    
    @RegBlock
    public static BlockMatrix
    	matBasic = new BlockMatrix(MatrixType.BASIC),
    	matStandard = new BlockMatrix(MatrixType.STANDARD),
    	matAdvanced = new BlockMatrix(MatrixType.ADVANCED);

}
