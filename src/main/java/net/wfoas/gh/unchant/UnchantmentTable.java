package net.wfoas.gh.unchant;

import java.util.Random;

import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityEnchantmentTable;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.wfoas.gh.blocks.GameHelperModBlock;

public class UnchantmentTable extends GameHelperModBlock implements ITileEntityProvider{
	public UnchantmentTable() {
		super(Material.rock, "unchantment_table");
		this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.75F, 1.0F);
		this.setLightOpacity(0);
		this.setHardness(4.0f);
		// this.setCreativeTab(CreativeTabs.tabDecorations);
	}

	@Override
	public boolean isFullCube() {
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void randomDisplayTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
		super.randomDisplayTick(worldIn, pos, state, rand);
		//
		for (int i = -2; i <= 2; ++i) {
			for (int j = -2; j <= 2; ++j) {
				if (i > -2 && i < 2 && j == -1) {
					j = 2;
				}

				if (rand.nextInt(24) == 0) {
					for (int k = 0; k <= 1; ++k) {
						// BlockPos blockpos1 = pos.add(i, k, j);
						//
						// if (worldIn.getBlockState(blockpos1).getBlock() ==
						// Blocks.bookshelf)
						// {
						// if (!worldIn.isAirBlock(pos.add(i / 2, 0, j / 2)))
						// {
						// break;
						// }
						//
						worldIn.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE, pos.getX() + 0.5D,
								pos.getY() + 2.0D, pos.getZ() + 0.5D,
								i + rand.nextFloat() - 0.5D,
								k - rand.nextFloat() - 1.0F,
								j + rand.nextFloat() - 0.5D, new int[0]);
					}
					// }
				}
			}
		}
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public int getRenderType() {
		return 3;
	}

	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		return new TileEntityUnchantmentTable();
	}

	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
			EnumFacing side, float hitX, float hitY, float hitZ) {
		if (worldIn.isRemote) {
			return true;
		} else {
			// TileEntity tileentity = worldIn.getTileEntity(pos);

			// if (tileentity instanceof TileEntityEnchantmentTable)
			// {
			// playerIn.displayGui((TileEntityEnchantmentTable)tileentity);
			// }

			return true;
		}
	}

	@Override
	public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer,
			ItemStack stack) {
		super.onBlockPlacedBy(worldIn, pos, state, placer, stack);

		if (stack.hasDisplayName()) {
			TileEntity tileentity = worldIn.getTileEntity(pos);

			if (tileentity instanceof TileEntityEnchantmentTable) {
				((TileEntityEnchantmentTable) tileentity).setCustomName(stack.getDisplayName());
			}
		}
	}
}

// }
