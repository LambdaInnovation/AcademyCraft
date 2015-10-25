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
package cn.academy.crafting;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegWorldGen;
import cpw.mods.fml.common.IWorldGenerator;

/**
 * @author WeAthFolD
 */
@Registrant
public class PhaseLiquidGenerator implements IWorldGenerator {

	@RegWorldGen(1)
	public static PhaseLiquidGenerator INSTANCE;
	
	WorldGenPhaseLiq genLakes = new WorldGenPhaseLiq();
	
	public PhaseLiquidGenerator() {}
	
	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world,
			IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
		switch(world.provider.dimensionId) {
		case 0:
			if(random.nextDouble() < 0.3)
				genLakes.generate(world, random, 
						chunkX * 16 + random.nextInt(16), 
						5 + random.nextInt(30), 
						chunkZ * 16  + random.nextInt(16));
			break;
		}
	}

}
