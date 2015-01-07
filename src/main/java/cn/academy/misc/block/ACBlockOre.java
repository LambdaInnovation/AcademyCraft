package cn.academy.misc.block;

import java.util.Random;

import cn.academy.core.AcademyCraftMod;
import cn.annoreg.core.ctor.Constructible;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;


/**
 * 矿物方块类
 * @author Lyt99
 */
public class ACBlockOre extends Block{
	
	String BName;
	
	@Constructible
	public ACBlockOre(String Blockname,int HarvestLevel){
		super(Material.rock);
		setCreativeTab(AcademyCraftMod.cct);
		this.setHarvestLevel("pickaxe", HarvestLevel);
		setHardness(4.0F);
		setBlockName(Blockname);
		setBlockTextureName("academy:" + Blockname);
		setStepSound(Block.soundTypePiston);
		BName = Blockname;
		
	}
	
	@Override
	public Item getItemDropped(int metadata, Random random, int fortune){
		return getDrop(this, BName);
	}

	
	public Item getDrop(Block block, String blockname){
		//暂时用不到就不写了
		return Item.getItemFromBlock(block);
	}
}
