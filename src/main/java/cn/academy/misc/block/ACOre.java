package cn.academy.misc.block;

import java.util.Random;

import cn.academy.core.AcademyCraft;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;

/**
 * AcademyCraft Ore
 * @author KSkun
 */
public class ACOre extends Block {
	
	private Item itemDropped;
	private float lightLevel;
	private float hardness = 3.0f;
	
	/** Reconstructed: Different ore have different hardness */
	public ACOre(int harvest, float hardness) {
		super(Material.rock);
		setHarvestLevel("pickaxe", harvest);
		setCreativeTab(AcademyCraft.cct);
		setStepSound(Block.soundTypePiston);
		setHardness(hardness);
		this.hardness = hardness;
		setResistance(5.0F);
	}
	
	public ACOre(int harvest) {
		this(harvest, 3.0f);
	}
	
	@Override
	public Item getItemDropped(int par1, Random par2Rand, int par3) {
		if(itemDropped == null) return Item.getItemById(Block.getIdFromBlock(this));
		return itemDropped;
	}
	
	public ACOre getItemDropped(Item itemDropped) {
		this.itemDropped = itemDropped;
		return this;
	}
	
    public ACOre setLightLevel(float light) {
        super.setLightLevel(light);
        return this;
    }
    
    public ACOre setOreHardness(float hardness) {
    	this.hardness = hardness;
    	return this;
    }

}
