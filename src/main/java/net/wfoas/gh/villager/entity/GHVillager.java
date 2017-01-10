package net.wfoas.gh.villager.entity;

import java.util.Random;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.IMerchant;
import net.minecraft.entity.INpc;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAIFollowGolem;
import net.minecraft.entity.ai.EntityAIHarvestFarmland;
import net.minecraft.entity.ai.EntityAILookAtTradePlayer;
import net.minecraft.entity.ai.EntityAIMoveIndoors;
import net.minecraft.entity.ai.EntityAIMoveTowardsRestriction;
import net.minecraft.entity.ai.EntityAIOpenDoor;
import net.minecraft.entity.ai.EntityAIPlay;
import net.minecraft.entity.ai.EntityAIRestrictOpenDoor;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAITradePlayer;
import net.minecraft.entity.ai.EntityAIVillagerInteract;
import net.minecraft.entity.ai.EntityAIVillagerMate;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.EntityAIWatchClosest2;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.stats.StatList;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Tuple;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.village.Village;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.wfoas.gh.network.gui.RemoteGuiOpener;
import net.wfoas.gh.villager.VillagerRegistrar;

public class GHVillager extends EntityVillager implements IMerchant, INpc {
	private int randomTickDivider;
	private boolean isMating;
	private boolean isPlaying;
	Village villageObj;
	/** This villager's current customer. */
	private EntityPlayer buyingPlayer;
	/** Initialises the MerchantRecipeList.java */
	private MerchantRecipeList buyingList;
	private int timeUntilReset;
	/** addDefaultEquipmentAndRecipies is called if this is true */
	private boolean needsInitilization;
	private boolean isWillingToMate;
	private int wealth;
	/** Last player to trade with this villager, used for aggressivity. */
	private String lastBuyingPlayer;
	private final int careerId = 0;
	/** This is the EntityVillager's career level value */
	private final int careerLevel = 0;
	private boolean isLookingForHome;
	private boolean areAdditionalTasksSet;
	private InventoryBasic villagerInventory;

	public GHVillager(World worldIn) {
		this(worldIn, 0);
	}

	public GHVillager(World worldIn, int professionId) {
		super(worldIn);
		this.villagerInventory = new InventoryBasic("Items", false, 8);
		this.setProfession(professionId);
		this.setSize(0.6F, 1.8F);
		((PathNavigateGround) this.getNavigator()).setBreakDoors(true);
		((PathNavigateGround) this.getNavigator()).setAvoidsWater(true);
		this.tasks.addTask(0, new EntityAISwimming(this));
		this.tasks.addTask(1, new EntityAIAvoidEntity(this, EntityZombie.class, 8.0F, 0.6D, 0.6D));
		this.tasks.addTask(1, new EntityAITradePlayer(this));
		this.tasks.addTask(1, new EntityAILookAtTradePlayer(this));
		this.tasks.addTask(2, new EntityAIMoveIndoors(this));
		this.tasks.addTask(3, new EntityAIRestrictOpenDoor(this));
		this.tasks.addTask(4, new EntityAIOpenDoor(this, true));
		this.tasks.addTask(5, new EntityAIMoveTowardsRestriction(this, 0.6D));
		this.tasks.addTask(6, new EntityAIVillagerMate(this));
		this.tasks.addTask(7, new EntityAIFollowGolem(this));
		this.tasks.addTask(9, new EntityAIWatchClosest2(this, EntityPlayer.class, 3.0F, 1.0F));
		this.tasks.addTask(9, new EntityAIVillagerInteract(this));
		this.tasks.addTask(9, new EntityAIWander(this, 0.6D));
		this.tasks.addTask(10, new EntityAIWatchClosest(this, EntityLiving.class, 8.0F));
		this.setCanPickUpLoot(true);
	}

	private void setAdditionalAItasks() {
		if (!this.areAdditionalTasksSet) {
			this.areAdditionalTasksSet = true;

			if (this.isChild()) {
				this.tasks.addTask(8, new EntityAIPlay(this, 0.32D));
			} else if (this.getProfession() == 0) {
				this.tasks.addTask(6, new EntityAIHarvestFarmland(this, 0.6D));
			}
		}
	}

	/**
	 * This is called when Entity's growing age timer reaches 0 (negative values
	 * are considered as a child, positive as an adult)
	 */
	protected void onGrowingAdult() {
		if (this.getProfession() == 0) {
			this.tasks.addTask(8, new EntityAIHarvestFarmland(this, 0.6D));
		}

		super.onGrowingAdult();
	}

	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.5D);
	}

	protected void updateAITasks() {
		if (--this.randomTickDivider <= 0) {
			BlockPos blockpos = new BlockPos(this);
			this.worldObj.getVillageCollection().addToVillagerPositionList(blockpos);
			this.randomTickDivider = 70 + this.rand.nextInt(50);
			this.villageObj = this.worldObj.getVillageCollection().getNearestVillage(blockpos, 32);

			if (this.villageObj == null) {
				this.detachHome();
			} else {
				BlockPos blockpos1 = this.villageObj.getCenter();
				this.setHomePosAndDistance(blockpos1, (int) ((float) this.villageObj.getVillageRadius() * 1.0F));

				if (this.isLookingForHome) {
					this.isLookingForHome = false;
					this.villageObj.setDefaultPlayerReputation(5);
				}
			}
		}

		if (!this.isTrading() && this.timeUntilReset > 0) {
			--this.timeUntilReset;

			if (this.timeUntilReset <= 0) {
				if (this.needsInitilization) {
					for (MerchantRecipe merchantrecipe : this.buyingList) {
						if (merchantrecipe.isRecipeDisabled()) {
							merchantrecipe.increaseMaxTradeUses(this.rand.nextInt(6) + this.rand.nextInt(6) + 2);
						}
					}

					this.populateBuyingList();
					this.needsInitilization = false;

					if (this.villageObj != null && this.lastBuyingPlayer != null) {
						this.worldObj.setEntityState(this, (byte) 14);
						this.villageObj.setReputationForPlayer(this.lastBuyingPlayer, 1);
					}
				}

				this.addPotionEffect(new PotionEffect(Potion.regeneration.id, 200, 0));
			}
		}

		super.updateAITasks();
	}

	/**
	 * Called when a player interacts with a mob. e.g. gets milk from a cow,
	 * gets into the saddle on a pig.
	 */
	public boolean interact(EntityPlayer player) {
		ItemStack itemstack = player.inventory.getCurrentItem();
		boolean flag = itemstack != null && itemstack.getItem() == Items.spawn_egg;

		if (!flag && this.isEntityAlive() && !this.isTrading() && !this.isChild() && !player.isSneaking()) {
			if (!this.worldObj.isRemote && (this.buyingList == null || this.buyingList.size() > 0)) {
				this.setCustomer(player);
				player.displayVillagerTradeGui(this);
				// if (player instanceof EntityPlayerMP)
				// RemoteGuiOpener.openTradingInventory((EntityPlayerMP) player,
				// this);
			}

			player.triggerAchievement(StatList.timesTalkedToVillagerStat);
			return true;
		} else {
			// return super.interact(player);
			return false;
		}
	}

	protected void entityInit() {
		super.entityInit();
		// this.dataWatcher.addObject(16, Integer.valueOf(0));
	}

	/**
	 * (abstract) Protected helper method to write subclass entity data to NBT.
	 */
	public void writeEntityToNBT(NBTTagCompound tagCompound) {
		super.writeEntityToNBT(tagCompound);
		tagCompound.setInteger("Profession", this.getProfession());
		tagCompound.setInteger("Riches", this.wealth);
		tagCompound.setInteger("Career", this.careerId);
		tagCompound.setInteger("CareerLevel", this.careerLevel);
		tagCompound.setBoolean("Willing", this.isWillingToMate);
		tagCompound.setInteger("ListLevel", listLevel);

		if (this.buyingList != null) {
			tagCompound.setTag("Offers", this.buyingList.getRecipiesAsTags());
		}

		NBTTagList nbttaglist = new NBTTagList();

		for (int i = 0; i < this.villagerInventory.getSizeInventory(); ++i) {
			ItemStack itemstack = this.villagerInventory.getStackInSlot(i);

			if (itemstack != null) {
				nbttaglist.appendTag(itemstack.writeToNBT(new NBTTagCompound()));
			}
		}

		tagCompound.setTag("Inventory", nbttaglist);
	}

	/**
	 * (abstract) Protected helper method to read subclass entity data from NBT.
	 */
	public void readEntityFromNBT(NBTTagCompound tagCompund) {
		super.readEntityFromNBT(tagCompund);
		this.setProfession(tagCompund.getInteger("Profession"));
		this.wealth = tagCompund.getInteger("Riches");
		this.isWillingToMate = tagCompund.getBoolean("Willing");

		if (tagCompund.hasKey("Offers", 10)) {
			NBTTagCompound nbttagcompound = tagCompund.getCompoundTag("Offers");
			this.buyingList = new MerchantRecipeList(nbttagcompound);
		}

		NBTTagList nbttaglist = tagCompund.getTagList("Inventory", 10);

		for (int i = 0; i < nbttaglist.tagCount(); ++i) {
			ItemStack itemstack = ItemStack.loadItemStackFromNBT(nbttaglist.getCompoundTagAt(i));

			if (itemstack != null) {
				this.villagerInventory.func_174894_a(itemstack);
			}
		}
		listLevel = tagCompund.getInteger("ListLevel");
		this.setCanPickUpLoot(true);
		this.setAdditionalAItasks();
	}

	protected boolean canDespawn() {
		return false;
	}

	protected String getLivingSound() {
		return this.isTrading() ? "mob.villager.haggle" : "mob.villager.idle";
	}

	protected String getHurtSound() {
		return "mob.villager.hit";
	}

	protected String getDeathSound() {
		return "mob.villager.death";
	}

	public void setProfession(int professionId) {
		this.dataWatcher.updateObject(16, Integer.valueOf(professionId));
	}

	public int getProfession() {
		return Math.max(this.dataWatcher.getWatchableObjectInt(16) % 5, 0);
	}

	public boolean isMating() {
		return this.isMating;
	}

	public void setMating(boolean mating) {
		this.isMating = mating;
	}

	public void setPlaying(boolean playing) {
		this.isPlaying = playing;
	}

	public boolean isPlaying() {
		return this.isPlaying;
	}

	public void setRevengeTarget(EntityLivingBase livingBase) {
		super.setRevengeTarget(livingBase);

		if (this.villageObj != null && livingBase != null) {
			this.villageObj.addOrRenewAgressor(livingBase);

			if (livingBase instanceof EntityPlayer) {
				int i = -1;

				if (this.isChild()) {
					i = -3;
				}

				this.villageObj.setReputationForPlayer(livingBase.getName(), i);

				if (this.isEntityAlive()) {
					this.worldObj.setEntityState(this, (byte) 13);
				}
			}
		}
	}

	public void onDeath(DamageSource cause) {
		super.onDeath(cause);
	}

	public void setCustomer(EntityPlayer p_70932_1_) {
		this.buyingPlayer = p_70932_1_;
	}

	public EntityPlayer getCustomer() {
		return this.buyingPlayer;
	}

	public boolean isTrading() {
		return this.buyingPlayer != null;
	}

	/**
	 * Returns current or updated value of {@link #isWillingToMate}
	 */
	public boolean getIsWillingToMate(boolean updateFirst) {
		if (!this.isWillingToMate && updateFirst && this.func_175553_cp()) {
			boolean flag = false;

			for (int i = 0; i < this.villagerInventory.getSizeInventory(); ++i) {
				ItemStack itemstack = this.villagerInventory.getStackInSlot(i);

				if (itemstack != null) {
					if (itemstack.getItem() == Items.bread && itemstack.stackSize >= 3) {
						flag = true;
						this.villagerInventory.decrStackSize(i, 3);
					} else if ((itemstack.getItem() == Items.potato || itemstack.getItem() == Items.carrot)
							&& itemstack.stackSize >= 12) {
						flag = true;
						this.villagerInventory.decrStackSize(i, 12);
					}
				}

				if (flag) {
					this.worldObj.setEntityState(this, (byte) 18);
					this.isWillingToMate = true;
					break;
				}
			}
		}

		return this.isWillingToMate;
	}

	public void setIsWillingToMate(boolean willingToTrade) {
		this.isWillingToMate = willingToTrade;
	}

	public void useRecipe(MerchantRecipe recipe) {
		recipe.incrementToolUses();
		this.livingSoundTime = -this.getTalkInterval();
		this.playSound("mob.villager.yes", this.getSoundVolume(), this.getSoundPitch());
		int i = 3 + this.rand.nextInt(4);

		if (recipe.getToolUses() == 1 || this.rand.nextInt(5) == 0) {
			this.timeUntilReset = 40;
			this.needsInitilization = true;
			this.isWillingToMate = true;

			if (this.buyingPlayer != null) {
				this.lastBuyingPlayer = this.buyingPlayer.getName();
			} else {
				this.lastBuyingPlayer = null;
			}

			i += 5;
		}

		if (recipe.getItemToBuy().getItem() == Items.emerald) {
			this.wealth += recipe.getItemToBuy().stackSize;
		}

		if (recipe.getRewardsExp()) {
			this.worldObj.spawnEntityInWorld(new EntityXPOrb(this.worldObj, this.posX, this.posY + 0.5D, this.posZ, i));
		}
	}

	/**
	 * Notifies the merchant of a possible merchantrecipe being fulfilled or
	 * not. Usually, this is just a sound byte being played depending if the
	 * suggested itemstack is not null.
	 */
	public void verifySellingItem(ItemStack stack) {
		if (!this.worldObj.isRemote && this.livingSoundTime > -this.getTalkInterval() + 20) {
			this.livingSoundTime = -this.getTalkInterval();

			if (stack != null) {
				this.playSound("mob.villager.yes", this.getSoundVolume(), this.getSoundPitch());
			} else {
				this.playSound("mob.villager.no", this.getSoundVolume(), this.getSoundPitch());
			}
		}
	}

	public MerchantRecipeList getRecipes(EntityPlayer p_70934_1_) {
		if (this.buyingList == null) {
			this.populateBuyingList();
		}

		return this.buyingList;
	}

	int listLevel = 0;

	private void populateBuyingList() {
		if (this.buyingList == null) {
			this.buyingList = new MerchantRecipeList();
			this.buyingList
					.addAll(VillagerRegistrar.getProfessionById(getProfession()).getMerchantRecipeListExplicitCopy());
		}
		MerchantRecipeList mr = VillagerRegistrar.getProfessionById(getProfession()).getUnlockable(listLevel);
		if (mr != null) {
			this.buyingList.addAll(mr);
			listLevel++;
		}
	}

	@SideOnly(Side.CLIENT)
	public void setRecipes(MerchantRecipeList recipeList) {
	}

	/**
	 * Get the formatted ChatComponent that will be used for the sender's
	 * username in chat
	 */
	public IChatComponent getDisplayName() {
		String s = this.getCustomNameTag();

		if (s != null && s.length() > 0) {
			ChatComponentText chatcomponenttext = new ChatComponentText(s);
			chatcomponenttext.getChatStyle().setChatHoverEvent(this.getHoverEvent());
			chatcomponenttext.getChatStyle().setInsertion(this.getUniqueID().toString());
			return chatcomponenttext;
		} else {
			if (this.buyingList == null) {
				this.populateBuyingList();
			}
			ChatComponentTranslation cc = new ChatComponentTranslation(
					VillagerRegistrar.getProfessionById(this.getProfession()).getUnlocalizedI18nKey());
			cc.getChatStyle().setChatHoverEvent(this.getHoverEvent());
			cc.getChatStyle().setInsertion(this.getUniqueID().toString());
			return cc;
		}
	}

	public float getEyeHeight() {
		float f = 1.62F;

		if (this.isChild()) {
			f = (float) ((double) f - 0.81D);
		}

		return f;
	}

	@SideOnly(Side.CLIENT)
	public void handleStatusUpdate(byte id) {
		if (id == 12) {
			this.spawnParticles(EnumParticleTypes.HEART);
		} else if (id == 13) {
			this.spawnParticles(EnumParticleTypes.VILLAGER_ANGRY);
		} else if (id == 14) {
			this.spawnParticles(EnumParticleTypes.VILLAGER_HAPPY);
		} else {
			super.handleStatusUpdate(id);
		}
	}

	@SideOnly(Side.CLIENT)
	private void spawnParticles(EnumParticleTypes particleType) {
		for (int i = 0; i < 5; ++i) {
			double d0 = this.rand.nextGaussian() * 0.02D;
			double d1 = this.rand.nextGaussian() * 0.02D;
			double d2 = this.rand.nextGaussian() * 0.02D;
			this.worldObj.spawnParticle(particleType,
					this.posX + (double) (this.rand.nextFloat() * this.width * 2.0F) - (double) this.width,
					this.posY + 1.0D + (double) (this.rand.nextFloat() * this.height),
					this.posZ + (double) (this.rand.nextFloat() * this.width * 2.0F) - (double) this.width, d0, d1, d2,
					new int[0]);
		}
	}

	/**
	 * Called only once on an entity when first time spawned, via egg, mob
	 * spawner, natural spawning etc, but not called when entity is reloaded
	 * from nbt. Mainly used for initializing attributes and inventory
	 */
	public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, IEntityLivingData livingdata) {
		livingdata = super.onInitialSpawn(difficulty, livingdata);
		net.minecraftforge.fml.common.registry.VillagerRegistry.setRandomProfession(this, this.worldObj.rand);
		this.setAdditionalAItasks();
		return livingdata;
	}

	public void setLookingForHome() {
		this.isLookingForHome = true;
	}

	public GHVillager createChild(EntityAgeable ageable) {
		GHVillager entityvillager = new GHVillager(this.worldObj);
		entityvillager.onInitialSpawn(this.worldObj.getDifficultyForLocation(new BlockPos(entityvillager)),
				(IEntityLivingData) null);
		return entityvillager;
	}

	public boolean allowLeashing() {
		return false;
	}

	/**
	 * Called when a lightning bolt hits the entity.
	 */
	public void onStruckByLightning(EntityLightningBolt lightningBolt) {
		if (!this.worldObj.isRemote && !this.isDead) {
			EntityWitch entitywitch = new EntityWitch(this.worldObj);
			entitywitch.setLocationAndAngles(this.posX, this.posY, this.posZ, this.rotationYaw, this.rotationPitch);
			entitywitch.onInitialSpawn(this.worldObj.getDifficultyForLocation(new BlockPos(entitywitch)),
					(IEntityLivingData) null);
			entitywitch.setNoAI(this.isAIDisabled());

			if (this.hasCustomName()) {
				entitywitch.setCustomNameTag(this.getCustomNameTag());
				entitywitch.setAlwaysRenderNameTag(this.getAlwaysRenderNameTag());
			}

			this.worldObj.spawnEntityInWorld(entitywitch);
			this.setDead();
		}
	}

	public InventoryBasic getVillagerInventory() {
		return this.villagerInventory;
	}

	/**
	 * Tests if this entity should pickup a weapon or an armor. Entity drops
	 * current weapon or armor if the new one is better.
	 */
	protected void updateEquipmentIfNeeded(EntityItem itemEntity) {
		ItemStack itemstack = itemEntity.getEntityItem();
		Item item = itemstack.getItem();

		if (this.canVillagerPickupItem(item)) {
			ItemStack itemstack1 = this.villagerInventory.func_174894_a(itemstack);

			if (itemstack1 == null) {
				itemEntity.setDead();
			} else {
				itemstack.stackSize = itemstack1.stackSize;
			}
		}
	}

	private boolean canVillagerPickupItem(Item itemIn) {
		return itemIn == Items.bread || itemIn == Items.potato || itemIn == Items.carrot || itemIn == Items.wheat
				|| itemIn == Items.wheat_seeds;
	}

	public boolean func_175553_cp() {
		return this.hasEnoughItems(1);
	}

	/**
	 * Used by {@link net.minecraft.entity.ai.EntityAIVillagerInteract
	 * EntityAIVillagerInteract} to check if the villager can give some items
	 * from an inventory to another villager.
	 */
	public boolean canAbondonItems() {
		return this.hasEnoughItems(2);
	}

	public boolean func_175557_cr() {
		boolean flag = this.getProfession() == 0;
		return flag ? !this.hasEnoughItems(5) : !this.hasEnoughItems(1);
	}

	/**
	 * Returns true if villager has enough items in inventory
	 */
	private boolean hasEnoughItems(int multiplier) {
		boolean flag = this.getProfession() == 0;

		for (int i = 0; i < this.villagerInventory.getSizeInventory(); ++i) {
			ItemStack itemstack = this.villagerInventory.getStackInSlot(i);

			if (itemstack != null) {
				if (itemstack.getItem() == Items.bread && itemstack.stackSize >= 3 * multiplier
						|| itemstack.getItem() == Items.potato && itemstack.stackSize >= 12 * multiplier
						|| itemstack.getItem() == Items.carrot && itemstack.stackSize >= 12 * multiplier) {
					return true;
				}

				if (flag && itemstack.getItem() == Items.wheat && itemstack.stackSize >= 9 * multiplier) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Returns true if villager has seeds, potatoes or carrots in inventory
	 */
	public boolean isFarmItemInInventory() {
		for (int i = 0; i < this.villagerInventory.getSizeInventory(); ++i) {
			ItemStack itemstack = this.villagerInventory.getStackInSlot(i);

			if (itemstack != null && (itemstack.getItem() == Items.wheat_seeds || itemstack.getItem() == Items.potato
					|| itemstack.getItem() == Items.carrot)) {
				return true;
			}
		}

		return false;
	}

	public boolean replaceItemInInventory(int inventorySlot, ItemStack itemStackIn) {
		if (super.replaceItemInInventory(inventorySlot, itemStackIn)) {
			return true;
		} else {
			int i = inventorySlot - 300;

			if (i >= 0 && i < this.villagerInventory.getSizeInventory()) {
				this.villagerInventory.setInventorySlotContents(i, itemStackIn);
				return true;
			} else {
				return false;
			}
		}
	}

	public static class EmeraldForItems implements GHVillager.ITradeList {
		public Item sellItem;
		public GHVillager.PriceInfo price;

		public EmeraldForItems(Item itemIn, GHVillager.PriceInfo priceIn) {
			this.sellItem = itemIn;
			this.price = priceIn;
		}

		/**
		 * Affects the given MerchantRecipeList to possibly add or remove
		 * MerchantRecipes.
		 */
		public void modifyMerchantRecipeList(MerchantRecipeList recipeList, Random random) {
			int i = 1;

			if (this.price != null) {
				i = this.price.getPrice(random);
			}

			recipeList.add(new MerchantRecipe(new ItemStack(this.sellItem, i, 0), Items.emerald));
		}
	}

	public interface ITradeList {
		/**
		 * Affects the given MerchantRecipeList to possibly add or remove
		 * MerchantRecipes.
		 */
		void modifyMerchantRecipeList(MerchantRecipeList recipeList, Random random);
	}

	public static class ItemAndEmeraldToItem implements GHVillager.ITradeList {
		public ItemStack field_179411_a;
		public GHVillager.PriceInfo field_179409_b;
		public ItemStack field_179410_c;
		public GHVillager.PriceInfo field_179408_d;

		public ItemAndEmeraldToItem(Item p_i45813_1_, GHVillager.PriceInfo p_i45813_2_, Item p_i45813_3_,
				GHVillager.PriceInfo p_i45813_4_) {
			this.field_179411_a = new ItemStack(p_i45813_1_);
			this.field_179409_b = p_i45813_2_;
			this.field_179410_c = new ItemStack(p_i45813_3_);
			this.field_179408_d = p_i45813_4_;
		}

		/**
		 * Affects the given MerchantRecipeList to possibly add or remove
		 * MerchantRecipes.
		 */
		public void modifyMerchantRecipeList(MerchantRecipeList recipeList, Random random) {
			int i = 1;

			if (this.field_179409_b != null) {
				i = this.field_179409_b.getPrice(random);
			}

			int j = 1;

			if (this.field_179408_d != null) {
				j = this.field_179408_d.getPrice(random);
			}

			recipeList.add(new MerchantRecipe(
					new ItemStack(this.field_179411_a.getItem(), i, this.field_179411_a.getMetadata()),
					new ItemStack(Items.emerald),
					new ItemStack(this.field_179410_c.getItem(), j, this.field_179410_c.getMetadata())));
		}
	}

	public static class ListEnchantedBookForEmeralds implements GHVillager.ITradeList {
		/**
		 * Affects the given MerchantRecipeList to possibly add or remove
		 * MerchantRecipes.
		 */
		public void modifyMerchantRecipeList(MerchantRecipeList recipeList, Random random) {
			Enchantment enchantment = Enchantment.enchantmentsBookList[random
					.nextInt(Enchantment.enchantmentsBookList.length)];
			int i = MathHelper.getRandomIntegerInRange(random, enchantment.getMinLevel(), enchantment.getMaxLevel());
			ItemStack itemstack = Items.enchanted_book.getEnchantedItemStack(new EnchantmentData(enchantment, i));
			int j = 2 + random.nextInt(5 + i * 10) + 3 * i;

			if (j > 64) {
				j = 64;
			}

			recipeList.add(new MerchantRecipe(new ItemStack(Items.book), new ItemStack(Items.emerald, j), itemstack));
		}
	}

	public static class ListEnchantedItemForEmeralds implements GHVillager.ITradeList {
		public ItemStack field_179407_a;
		public GHVillager.PriceInfo field_179406_b;

		public ListEnchantedItemForEmeralds(Item p_i45814_1_, GHVillager.PriceInfo p_i45814_2_) {
			this.field_179407_a = new ItemStack(p_i45814_1_);
			this.field_179406_b = p_i45814_2_;
		}

		/**
		 * Affects the given MerchantRecipeList to possibly add or remove
		 * MerchantRecipes.
		 */
		public void modifyMerchantRecipeList(MerchantRecipeList recipeList, Random random) {
			int i = 1;

			if (this.field_179406_b != null) {
				i = this.field_179406_b.getPrice(random);
			}

			ItemStack itemstack = new ItemStack(Items.emerald, i, 0);
			ItemStack itemstack1 = new ItemStack(this.field_179407_a.getItem(), 1, this.field_179407_a.getMetadata());
			itemstack1 = EnchantmentHelper.addRandomEnchantment(random, itemstack1, 5 + random.nextInt(15));
			recipeList.add(new MerchantRecipe(itemstack, itemstack1));
		}
	}

	public static class ListItemForEmeralds implements GHVillager.ITradeList {
		public ItemStack field_179403_a;
		public GHVillager.PriceInfo field_179402_b;

		public ListItemForEmeralds(Item par1Item, GHVillager.PriceInfo priceInfo) {
			this.field_179403_a = new ItemStack(par1Item);
			this.field_179402_b = priceInfo;
		}

		public ListItemForEmeralds(ItemStack stack, GHVillager.PriceInfo priceInfo) {
			this.field_179403_a = stack;
			this.field_179402_b = priceInfo;
		}

		/**
		 * Affects the given MerchantRecipeList to possibly add or remove
		 * MerchantRecipes.
		 */
		public void modifyMerchantRecipeList(MerchantRecipeList recipeList, Random random) {
			int i = 1;

			if (this.field_179402_b != null) {
				i = this.field_179402_b.getPrice(random);
			}

			ItemStack itemstack;
			ItemStack itemstack1;

			if (i < 0) {
				itemstack = new ItemStack(Items.emerald, 1, 0);
				itemstack1 = new ItemStack(this.field_179403_a.getItem(), -i, this.field_179403_a.getMetadata());
			} else {
				itemstack = new ItemStack(Items.emerald, i, 0);
				itemstack1 = new ItemStack(this.field_179403_a.getItem(), 1, this.field_179403_a.getMetadata());
			}

			recipeList.add(new MerchantRecipe(itemstack, itemstack1));
		}
	}

	public static class PriceInfo extends Tuple<Integer, Integer> {
		public PriceInfo(int p_i45810_1_, int p_i45810_2_) {
			super(Integer.valueOf(p_i45810_1_), Integer.valueOf(p_i45810_2_));
		}

		public int getPrice(Random rand) {
			return ((Integer) this.getFirst()).intValue() >= ((Integer) this.getSecond()).intValue()
					? ((Integer) this.getFirst()).intValue()
					: ((Integer) this.getFirst()).intValue() + rand.nextInt(
							((Integer) this.getSecond()).intValue() - ((Integer) this.getFirst()).intValue() + 1);
		}
	}
}