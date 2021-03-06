package net.wfoas.gh.backpack.tiny;

import java.util.UUID;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.wfoas.gh.items.BackpackItem;

public class InventoryItem implements IInventory {
	private static final String DEFAULT_NAME = "container.backpack";

	private String name = null;

	protected String uuID;
	protected boolean hasOwner = false;
	protected UUID ownerUUID = null;
	protected String ownerName = null;
	protected boolean ownerPrivate = false;
	
	protected EntityPlayer ep;

	/** Provides NBT Tag Compound to reference */
	private final ItemStack invItem;

	/** Defining your inventory size this way is handy */
	public static final int INV_SIZE = 27;

	/**
	 * Inventory's size must be same as number of slots you add to the Container
	 * class
	 */
	private ItemStack[] inventory = new ItemStack[INV_SIZE];
	
	protected ItemStack itemStack;
	
	/**
	 * @param itemstack
	 *            - the ItemStack to which this inventory belongs
	 */
	public InventoryItem(ItemStack stack, EntityPlayer pe) {
		this.ep = pe;
		uuID = "";
		itemStack = stack;
		invItem = stack;
		name = stack.hasDisplayName() ? stack.getDisplayName() : null;
		// Create a new NBT Tag Compound if one doesn't already exist, or you
		// will crash
		if (!stack.hasTagCompound()) {
			stack.setTagCompound(new NBTTagCompound());
			uuID = UUID.randomUUID().toString();
			stack.getTagCompound().setLong("OwnerMSB", pe.getUniqueID().getMostSignificantBits());
			stack.getTagCompound().setLong("OwnerLSB", pe.getUniqueID().getLeastSignificantBits());
			stack.getTagCompound().setString("OwnerName", ep.getName());
			stack.getTagCompound().setString("BPuniqueID", uuID);
			ownerUUID = new UUID(stack.getTagCompound().getLong("OwnerMSB"),
					stack.getTagCompound().getLong("OwnerLSB"));
			ownerName = stack.getTagCompound().getString("OwnerName");
		} else {
			hasOwner = true;
			readFromNBT(stack.getTagCompound());
		}
		// note that it's okay to use stack instead of invItem right there
		// both reference the same memory location, so whatever you change using
		// either reference will change in the other

		// Read the inventory contents from NBT
	}

	@Override
	public int getSizeInventory() {
		return inventory.length;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return inventory[slot];
	}

	@Override
	public ItemStack decrStackSize(int slot, int amount) {
		ItemStack stack = getStackInSlot(slot);
		if (stack != null) {
			if (stack.stackSize > amount) {
				stack = stack.splitStack(amount);
				// Don't forget this line or your inventory will not be saved!
				markDirty();
			} else {
				// this method also calls onInventoryChanged, so we don't need
				// to call it again
				setInventorySlotContents(slot, null);
			}
		}
		return stack;
	}

	@Override
	public ItemStack removeStackFromSlot(int index) {
		ItemStack stack = getStackInSlot(index);
		setInventorySlotContents(index, null);
		return stack;
	}

	@Override
	public void setInventorySlotContents(int slot, ItemStack stack) {
		inventory[slot] = stack;

		if (stack != null && stack.stackSize > getInventoryStackLimit()) {
			stack.stackSize = getInventoryStackLimit();
		}

		// Don't forget this line or your inventory will not be saved!
		markDirty();
	}

	// 1.7.2+ renamed to getInventoryName
	@Override
	public String getName() {
		return name == null ? I18n.format(DEFAULT_NAME) : name;
	}

	// 1.7.2+ renamed to hasCustomInventoryName
	@Override
	public boolean hasCustomName() {
		return name != null;
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	/**
	 * This is the method that will handle saving the inventory contents, as it
	 * is called (or should be called!) anytime the inventory changes. Perfect.
	 * Much better than using onUpdate in an Item, as this will also let you
	 * change things in your inventory without ever opening a Gui, if you want.
	 */
	// 1.7.2+ renamed to markDirty
	@Override
	public void markDirty() {
		for (int i = 0; i < getSizeInventory(); ++i) {
			if (getStackInSlot(i) != null && getStackInSlot(i).stackSize == 0) {
				inventory[i] = null;
			}
		}

		// This line here does the work:
		writeToNBT(invItem.getTagCompound());
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer entityplayer) {
		return true;
	}

	// 1.7.2+ renamed to openInventory(EntityPlayer player)
	@Override
	public void openInventory(EntityPlayer player) {
//		if (!hasOwner) {
//			invItem.getTagCompound().setLong("OwnerMSB", player.getUniqueID().getMostSignificantBits());
//			invItem.getTagCompound().setLong("OwnerLSB", player.getUniqueID().getLeastSignificantBits());
//			invItem.getTagCompound().setString("OwnerName", player.getName());
//			ownerUUID = player.getUniqueID();
//			ownerName = player.getName();
//		}
//		long msb = invItem.getTagCompound().getLong("OwnerMSB");
//		long lsb = invItem.getTagCompound().getLong("OwnerLSB");
//		UUID ownerUuid = new UUID(msb, lsb);
//		if (player.getUniqueID().equals(ownerUuid)) {
//			invItem.getTagCompound().setString("OwnerName", player.getName());
//		}
		// long msb = invItem.getTagCompound().getLong("OwnerMSB");
		// long lsb = invItem.getTagCompound().getLong("OwnerLSB");
	}

	// 1.7.2+ renamed to closeInventory(EntityPlayer player)
	@Override
	public void closeInventory(EntityPlayer player) {
	}

	/**
	 * This method doesn't seem to do what it claims to do, as items can still
	 * be left-clicked and placed in the inventory even when this returns false
	 */
	@Override
	public boolean isItemValidForSlot(int slot, ItemStack itemstack) {
		// Don't want to be able to store the inventory item within itself
		// Bad things will happen, like losing your inventory
		// Actually, this needs a custom Slot to work
		return !(itemstack.getItem() instanceof BackpackItem);
	}

	/**
	 * A custom method to read our inventory from an ItemStack's NBT compound
	 */
	public void readFromNBT(NBTTagCompound compound) {
		// Gets the custom taglist we wrote to this compound, if any
		// 1.7.2+ change to compound.getTagList("ItemInventory",
		// Constants.NBT.TAG_COMPOUND);
		if ("".equals(uuID)) {
			uuID = compound.getString("BPuniqueID");
			if ("".equals(uuID)) {
				uuID = UUID.randomUUID().toString();
			}
		}
		ownerPrivate = compound.getBoolean("private");
		ownerUUID = new UUID(compound.getLong("OwnerMSB"), compound.getLong("OwnerLSB"));
		ownerName = compound.getString("OwnerName");
		NBTTagList items = (NBTTagList) compound.getTag("ItemInventory");
		if (items != null)
			for (int i = 0; i < items.tagCount(); ++i) {
				// 1.7.2+ change to items.getCompoundTagAt(i)tagAt(i);
				NBTTagCompound item = items.getCompoundTagAt(i);
				int slot = item.getInteger("Slot");

				// Just double-checking that the saved slot index is within our
				// inventory array bounds
				if (slot >= 0 && slot < getSizeInventory()) {
					inventory[slot] = ItemStack.loadItemStackFromNBT(item);
				}
			}
	}

	/**
	 * A custom method to write our inventory to an ItemStack's NBT compound
	 */
	public void writeToNBT(NBTTagCompound tagcompound) {
		// Create a new NBT Tag List to store itemstacks as NBT Tags
		ownerPrivate = tagcompound.getBoolean("private");
		NBTTagList items = new NBTTagList();

		for (int i = 0; i < getSizeInventory(); ++i) {
			// Only write stacks that contain items
			if (getStackInSlot(i) != null) {
				// Make a new NBT Tag Compound to write the itemstack and slot
				// index to
				NBTTagCompound item = new NBTTagCompound();
				item.setInteger("Slot", i);
				// Writes the itemstack in slot(i) to the Tag Compound we just
				// made
				getStackInSlot(i).writeToNBT(item);
				// add the tag compound to our tag list
				items.appendTag(item);
			}
		}
		// Add the TagList to the ItemStack's Tag Compound with the name
		// "ItemInventory"
		tagcompound.setString("BPuniqueID", uuID);
		tagcompound.setTag("ItemInventory", items);
		if (ownerUUID != null) {
			tagcompound.setLong("OwnerMSB", ownerUUID.getMostSignificantBits());
			tagcompound.setLong("OwnerLSB", ownerUUID.getLeastSignificantBits());
			tagcompound.setString("OwnerName", ownerName);
			tagcompound.setBoolean("private", ownerPrivate);
		}
	}

	@Override
	public IChatComponent getDisplayName() {
		return this.hasCustomName() ? new ChatComponentText(this.getName())
				: new ChatComponentTranslation(this.getName(), new Object[0]);
	}

	@Override
	public int getField(int id) {
		return 0;
	}

	@Override
	public void setField(int id, int value) {
	}

	@Override
	public int getFieldCount() {
		return 0;
	}

	@Override
	public void clear() {
		for (int i = 0; i < getSizeInventory(); ++i) {
			inventory[i] = null;
		}
	}
}