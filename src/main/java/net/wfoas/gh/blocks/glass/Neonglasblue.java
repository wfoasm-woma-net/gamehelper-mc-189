package net.wfoas.gh.blocks.glass;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public class Neonglasblue extends GameHelperModGlass {
	public Neonglasblue() {
		super(Material.glass, "Neonglasblue");
		this.setHarvestLevel("pickaxe", 3);
		this.setHardness(2f);
		this.setStepSound(Block.soundTypeGlass);
	}
}
