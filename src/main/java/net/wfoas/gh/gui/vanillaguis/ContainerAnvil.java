package net.wfoas.gh.gui.vanillaguis;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ContainerAnvil extends Container {
	private static final Logger logger = LogManager.getLogger();
	private IInventory outputSlot;
	private IInventory inputSlots;
	private World theWorld;
	public int maximumCost;
	public int materialCost;
	private String repairedItemName;
	private final EntityPlayer thePlayer;

	// @SideOnly(Side.CLIENT)
	// public ContainerAnvil(InventoryPlayer playerInventory, World worldIn,
	// EntityPlayer player) {
	// this(playerInventory, worldIn, BlockPos.ORIGIN, player);
	// }

	public ContainerAnvil(InventoryPlayer playerInventory, final World worldIn, EntityPlayer player) {
		this.outputSlot = new InventoryCraftResult();
		this.inputSlots = new InventoryBasic("Repair", true, 2) {

			public void markDirty() {
				super.markDirty();
				ContainerAnvil.this.onCraftMatrixChanged(this);
			}
		};
		this.theWorld = worldIn;
		this.thePlayer = player;
		this.addSlotToContainer(new Slot(this.inputSlots, 0, 27, 47));
		this.addSlotToContainer(new Slot(this.inputSlots, 1, 76, 47));
		this.addSlotToContainer(new Slot(this.outputSlot, 2, 134, 47) {
			public boolean isItemValid(ItemStack stack) {
				return false;
			}

			public boolean canTakeStack(EntityPlayer playerIn) {
				return (playerIn.capabilities.isCreativeMode
						|| playerIn.experienceLevel >= ContainerAnvil.this.maximumCost)
						&& ContainerAnvil.this.maximumCost > 0 && this.getHasStack();
			}

			public void onPickupFromSlot(EntityPlayer playerIn, ItemStack stack) {
				if (!playerIn.capabilities.isCreativeMode) {
					playerIn.addExperienceLevel(-ContainerAnvil.this.maximumCost);
				}

				float breakChance = net.minecraftforge.common.ForgeHooks.onAnvilRepair(playerIn, stack,
						ContainerAnvil.this.inputSlots.getStackInSlot(0),
						ContainerAnvil.this.inputSlots.getStackInSlot(1));

				ContainerAnvil.this.inputSlots.setInventorySlotContents(0, (ItemStack) null);

				if (ContainerAnvil.this.materialCost > 0) {
					ItemStack itemstack1 = ContainerAnvil.this.inputSlots.getStackInSlot(1);

					if (itemstack1 != null && itemstack1.stackSize > ContainerAnvil.this.materialCost) {
						itemstack1.stackSize -= ContainerAnvil.this.materialCost;
						ContainerAnvil.this.inputSlots.setInventorySlotContents(1, itemstack1);
					} else {
						ContainerAnvil.this.inputSlots.setInventorySlotContents(1, (ItemStack) null);
					}
				} else {
					ContainerAnvil.this.inputSlots.setInventorySlotContents(1, (ItemStack) null);
				}

				ContainerAnvil.this.maximumCost = 0;
			}
		});
		int i;

		for (i = 0; i < 3; ++i) {
			for (int j = 0; j < 9; ++j) {
				this.addSlotToContainer(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
			}
		}

		for (i = 0; i < 9; ++i) {
			this.addSlotToContainer(new Slot(playerInventory, i, 8 + i * 18, 142));
		}
	}

	public void onCraftMatrixChanged(IInventory inventoryIn) {
		super.onCraftMatrixChanged(inventoryIn);

		if (inventoryIn == this.inputSlots) {
			this.updateRepairOutput();
		}
	}

	public static boolean onAnvilChange(ContainerAnvil container, ItemStack left, ItemStack right,
			IInventory outputSlot, String name, int baseCost) {
		AnvilUpdateEvent e = new AnvilUpdateEvent(left, right, name, baseCost);
		if (MinecraftForge.EVENT_BUS.post(e))
			return false;
		if (e.output == null)
			return true;

		outputSlot.setInventorySlotContents(0, e.output);
		container.maximumCost = e.cost;
		container.materialCost = e.materialCost;
		return false;
	}

	public void updateRepairOutput() {
		boolean flag = false;
		boolean flag1 = true;
		boolean flag2 = true;
		boolean flag3 = true;
		boolean flag4 = true;
		boolean flag5 = true;
		boolean flag6 = true;
		ItemStack itemstack = this.inputSlots.getStackInSlot(0);
		this.maximumCost = 1;
		int i = 0;
		byte b0 = 0;
		byte b1 = 0;

		if (itemstack == null) {
			this.outputSlot.setInventorySlotContents(0, (ItemStack) null);
			this.maximumCost = 0;
		} else {
			ItemStack itemstack1 = itemstack.copy();
			ItemStack itemstack2 = this.inputSlots.getStackInSlot(1);
			Map map = EnchantmentHelper.getEnchantments(itemstack1);
			boolean flag7 = false;
			int i2 = b0 + itemstack.getRepairCost() + (itemstack2 == null ? 0 : itemstack2.getRepairCost());
			this.materialCost = 0;
			int j;

			if (itemstack2 != null) {
				if (!onAnvilChange(this, itemstack, itemstack2, outputSlot, repairedItemName, i2))
					return;
				flag7 = itemstack2.getItem() == Items.enchanted_book
						&& Items.enchanted_book.getEnchantments(itemstack2).tagCount() > 0;
				int k;
				int l;

				if (itemstack1.isItemStackDamageable() && itemstack1.getItem().getIsRepairable(itemstack, itemstack2)) {
					j = Math.min(itemstack1.getItemDamage(), itemstack1.getMaxDamage() / 4);

					if (j <= 0) {
						this.outputSlot.setInventorySlotContents(0, (ItemStack) null);
						this.maximumCost = 0;
						return;
					}

					for (k = 0; j > 0 && k < itemstack2.stackSize; ++k) {
						l = itemstack1.getItemDamage() - j;
						itemstack1.setItemDamage(l);
						++i;
						j = Math.min(itemstack1.getItemDamage(), itemstack1.getMaxDamage() / 4);
					}

					this.materialCost = k;
				} else {
					if (!flag7
							&& (itemstack1.getItem() != itemstack2.getItem() || !itemstack1.isItemStackDamageable())) {
						this.outputSlot.setInventorySlotContents(0, (ItemStack) null);
						this.maximumCost = 0;
						return;
					}

					int j1;

					if (itemstack1.isItemStackDamageable() && !flag7) {
						j = itemstack.getMaxDamage() - itemstack.getItemDamage();
						k = itemstack2.getMaxDamage() - itemstack2.getItemDamage();
						l = k + itemstack1.getMaxDamage() * 12 / 100;
						int i1 = j + l;
						j1 = itemstack1.getMaxDamage() - i1;

						if (j1 < 0) {
							j1 = 0;
						}

						if (j1 < itemstack1.getMetadata()) {
							itemstack1.setItemDamage(j1);
							i += 2;
						}
					}

					Map map1 = EnchantmentHelper.getEnchantments(itemstack2);
					Iterator iterator1 = map1.keySet().iterator();

					while (iterator1.hasNext()) {
						l = ((Integer) iterator1.next()).intValue();
						Enchantment enchantment = Enchantment.getEnchantmentById(l);

						if (enchantment != null) {
							j1 = map.containsKey(Integer.valueOf(l))
									? ((Integer) map.get(Integer.valueOf(l))).intValue() : 0;
							int k1 = ((Integer) map1.get(Integer.valueOf(l))).intValue();
							int k2;

							if (j1 == k1) {
								++k1;
								k2 = k1;
							} else {
								k2 = Math.max(k1, j1);
							}

							k1 = k2;
							boolean flag8 = enchantment.canApply(itemstack);

							if (this.thePlayer.capabilities.isCreativeMode
									|| itemstack.getItem() == Items.enchanted_book) {
								flag8 = true;
							}

							Iterator iterator = map.keySet().iterator();

							while (iterator.hasNext()) {
								int l1 = ((Integer) iterator.next()).intValue();

								Enchantment e2 = Enchantment.getEnchantmentById(l1);
								if (l1 != l && !(enchantment.canApplyTogether(e2) && e2.canApplyTogether(enchantment))) // Forge
																														// BugFix:
																														// Let
																														// Both
																														// enchantments
																														// veto
																														// being
																														// together
								{
									flag8 = false;
									++i;
								}
							}

							if (flag8) {
								if (k1 > enchantment.getMaxLevel()) {
									k1 = enchantment.getMaxLevel();
								}

								map.put(Integer.valueOf(l), Integer.valueOf(k1));
								int j2 = 0;

								switch (enchantment.getWeight()) {
								case 1:
									j2 = 8;
									break;
								case 2:
									j2 = 4;
								case 3:
								case 4:
								case 6:
								case 7:
								case 8:
								case 9:
								default:
									break;
								case 5:
									j2 = 2;
									break;
								case 10:
									j2 = 1;
								}

								if (flag7) {
									j2 = Math.max(1, j2 / 2);
								}

								i += j2 * k1;
							}
						}
					}
				}
			}

			if (flag7 && !itemstack1.getItem().isBookEnchantable(itemstack1, itemstack2))
				itemstack1 = null;

			if (StringUtils.isBlank(this.repairedItemName)) {
				if (itemstack.hasDisplayName()) {
					b1 = 1;
					i += b1;
					itemstack1.clearCustomName();
				}
			} else if (!this.repairedItemName.equals(itemstack.getDisplayName())) {
				b1 = 1;
				i += b1;
				itemstack1.setStackDisplayName(this.repairedItemName);
			}

			this.maximumCost = i2 + i;

			if (i <= 0) {
				itemstack1 = null;
			}

			if (b1 == i && b1 > 0 && this.maximumCost >= 40) {
				this.maximumCost = 39;
			}

			if (this.maximumCost >= 40 && !this.thePlayer.capabilities.isCreativeMode) {
				itemstack1 = null;
			}

			if (itemstack1 != null) {
				j = itemstack1.getRepairCost();

				if (itemstack2 != null && j < itemstack2.getRepairCost()) {
					j = itemstack2.getRepairCost();
				}

				j = j * 2 + 1;
				itemstack1.setRepairCost(j);
				EnchantmentHelper.setEnchantments(map, itemstack1);
			}

			this.outputSlot.setInventorySlotContents(0, itemstack1);
			this.detectAndSendChanges();
		}
	}
	//
	// public void addCraftingToCrafters(ICrafting listener) {
	// super.addCraftingToCrafters(listener);
	// listener.sendProgressBarUpdate(this, 0, this.maximumCost);
	// }

	@SideOnly(Side.CLIENT)
	public void updateProgressBar(int id, int data) {
		if (id == 0) {
			this.maximumCost = data;
		}
	}

	public void onContainerClosed(EntityPlayer playerIn) {
		super.onContainerClosed(playerIn);

		if (!this.theWorld.isRemote) {
			for (int i = 0; i < this.inputSlots.getSizeInventory(); ++i) {
				ItemStack itemstack = this.inputSlots.removeStackFromSlot(i);

				if (itemstack != null) {
					playerIn.dropPlayerItemWithRandomChoice(itemstack, false);
				}
			}
		}
	}

	public boolean canInteractWith(EntityPlayer playerIn) {
		return true;
	}

	public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
		ItemStack itemstack = null;
		Slot slot = (Slot) this.inventorySlots.get(index);

		if (slot != null && slot.getHasStack()) {
			ItemStack itemstack1 = slot.getStack();
			itemstack = itemstack1.copy();

			if (index == 2) {
				if (!this.mergeItemStack(itemstack1, 3, 39, true)) {
					return null;
				}

				slot.onSlotChange(itemstack1, itemstack);
			} else if (index != 0 && index != 1) {
				if (index >= 3 && index < 39 && !this.mergeItemStack(itemstack1, 0, 2, false)) {
					return null;
				}
			} else if (!this.mergeItemStack(itemstack1, 3, 39, false)) {
				return null;
			}

			if (itemstack1.stackSize == 0) {
				slot.putStack((ItemStack) null);
			} else {
				slot.onSlotChanged();
			}

			if (itemstack1.stackSize == itemstack.stackSize) {
				return null;
			}

			slot.onPickupFromSlot(playerIn, itemstack1);
		}

		return itemstack;
	}

	public void updateItemName(String newName) {
		this.repairedItemName = newName;

		if (this.getSlot(2).getHasStack()) {
			ItemStack itemstack = this.getSlot(2).getStack();

			if (StringUtils.isBlank(newName)) {
				itemstack.clearCustomName();
			} else {
				itemstack.setStackDisplayName(this.repairedItemName);
			}
		}

		this.updateRepairOutput();
	}
}
