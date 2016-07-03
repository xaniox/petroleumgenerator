package drceph.petrogen.common;

import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergySource;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidContainerItem;
import net.minecraftforge.fluids.IFluidHandler;
import net.minecraftforge.fluids.IFluidTank;

public class TileEntityPetroleumGenerator extends TileEntity
		implements IInventory, IFluidTank, IFluidHandler, IEnergySource {

	private static final int SOURCE_TIER_LV = 1;
	private static final int MAX_VOLUME = FluidContainerRegistry.BUCKET_VOLUME * 10;
	private static final int MAX_CHARGE = 30000;
	public static final int FUEL_GAUGE_SCALE = 60;
	public static final int ENERGY_GAUGE_SCALE = 24;
	public static final int SLOT_COUNT = 2;

	private int amount;
	private int charge;
	private int active;
	private int currentEu;

	private boolean isDrainingEnergy;
	private ItemStack[] inventory;
	private boolean initialized;
	private PetroleumFuel fuel;
	private int buffer;
	private boolean hasRedstonePower;

	public TileEntityPetroleumGenerator() {
		this.inventory = new ItemStack[SLOT_COUNT];
	}
	
	@Override
	public int getFluidAmount() {
		return amount;
	}
	
	public void setFluidAmount(int amount) {
		this.amount = amount;
	}
	
	public int getCharge() {
		return charge;
	}
	
	public void setCharge(int charge) {
		this.charge = charge;
	}

	public boolean isActive() {
		return active == 1;
	}
	
	public void setActive(boolean active) {
		this.active = active ? 1 : 0;
	}
	
	public int getCurrentEu() {
		return currentEu;
	}
	
	public void setCurrentEu(int currentEu) {
		this.currentEu = currentEu;
	}

	@Override
	public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
		return this.fill(resource, doFill);
	}

	@Override
	public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
		return null;
	}

	@Override
	public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
		return null;
	}

	@Override
	public boolean canFill(ForgeDirection from, Fluid fluid) {
		return PetroleumFuel.isValidFuel(fluid.getID());
	}

	@Override
	public boolean canDrain(ForgeDirection from, Fluid fluid) {
		return false;
	}

	@Override
	public FluidTankInfo[] getTankInfo(ForgeDirection from) {
		return new FluidTankInfo[] { new FluidTankInfo(getFluid(), MAX_VOLUME) };
	}

	@Override
	public FluidStack getFluid() {
		if (fuel != null) {
			FluidStack newStack = fuel.getFuel().copy();
			newStack.amount = amount;
			return newStack;
		}

		return null;
	}

	@Override
	public int getCapacity() {
		return MAX_VOLUME;
	}

	@Override
	public FluidTankInfo getInfo() {
		return new FluidTankInfo(getFluid(), getCapacity());
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		if (fuel == null && PetroleumFuel.isValidFuel(resource.getFluidID())) {
			setCurrentFluid(resource.getFluidID());
		}

		if (!isCurrentFuel(resource)) {
			return 0;
		}

		int payload = resource.amount;

		if (amount + payload <= MAX_VOLUME) {
			if (doFill) {
				amount += payload;
			}
			return payload;
		} else {
			int difference = MAX_VOLUME - amount;
			if (doFill) {
				amount = MAX_VOLUME;
			}
			return difference;
		}
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain) {
		return null;
	}

	@Override
	public void updateEntity() {
		boolean didBurn = false;
		boolean changed = false;

		// Add to energy net on first update, and initialize network
		if (!initialized && worldObj != null) {
			if (!worldObj.isRemote) {
				EnergyTileLoadEvent event = new EnergyTileLoadEvent(this);
				MinecraftForge.EVENT_BUS.post(event);

				updateRedstoneStatus();
			}
			initialized = true;
			changed = true;
		}

		// only runs it on the server side
		// remember to notify of change
		if (!worldObj.isRemote) {
			// STEP 1: Try to fill tank
			if (amount <= MAX_VOLUME - FluidContainerRegistry.BUCKET_VOLUME) {
				changed = fillTankFromInventory(this.inventory[0]);
			}

			// STEP 2: Try to charge battery
			if (amount > 0) {
				if (buffer <= 0 && charge < MAX_CHARGE - fuel.getEuPacketSize()) {
					amount--;
					buffer = fuel.getEuPerFluidUnit();
				}
			}

			if (buffer > 0) {
				buffer -= currentEu;
				charge += currentEu;
				charge = Math.min(charge, MAX_CHARGE);
				didBurn = true;
				changed = true;
			}
			
			// STEP 3: cleanup
			if (buffer <= 0 && amount <= 0) {
				fuel = null;
				changed = true;
			}

			if (isDrainingEnergy || didBurn) {
				if (active == 0) {
					changed = true;
				}

				active = 1;
			} else {
				if (active == 1) {
					changed = true;
				}

				active = 0;
			}
			
			isDrainingEnergy = false;

			if (changed) {
				BlockPetroleumGenerator.updateBlockState(active == 1, worldObj, xCoord, yCoord, zCoord);
			}
		}

		if (changed) {
			markDirty();
		}
	}

	@Override
	public void onChunkUnload() {
		if (worldObj != null && !worldObj.isRemote && initialized) {
			EnergyTileUnloadEvent event = new EnergyTileUnloadEvent(this);
			MinecraftForge.EVENT_BUS.post(event);
			initialized = false;
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);

		NBTTagList tagList = tagCompound.getTagList("Inventory", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < tagList.tagCount(); i++) {
			NBTTagCompound tag = (NBTTagCompound) tagList.getCompoundTagAt(i);
			byte slot = tag.getByte("Slot");

			if (slot >= 0 && slot < inventory.length) {
				inventory[slot] = ItemStack.loadItemStackFromNBT(tag);
			}
		}

		int fluidId = tagCompound.getInteger("fluidId");
		setCurrentFluid(fluidId);
		amount = tagCompound.getInteger("amount");
		charge = tagCompound.getInteger("charge");
		buffer = tagCompound.getInteger("buffer");
		currentEu = tagCompound.getInteger("currentEu");
	}

	@Override
	public void writeToNBT(NBTTagCompound tagCompound) {
		super.writeToNBT(tagCompound);

		NBTTagList itemList = new NBTTagList();
		for (int i = 0; i < inventory.length; i++) {
			ItemStack stack = inventory[i];

			if (stack != null) {
				NBTTagCompound tag = new NBTTagCompound();

				tag.setByte("Slot", (byte) i);
				stack.writeToNBT(tag);
				itemList.appendTag(tag);
			}
		}

		int fluidId = fuel == null ? 0 : fuel.getFluidId();

		tagCompound.setTag("Inventory", itemList);
		tagCompound.setInteger("fluidId", fluidId);
		tagCompound.setInteger("amount", amount);
		tagCompound.setInteger("charge", charge);
		tagCompound.setInteger("buffer", buffer);
		tagCompound.setInteger("currentEu", currentEu);
	}

	private boolean fillTankFromInventory(ItemStack itemStack) {
		boolean changed = false;
		
		if (itemStack == null) {
			return changed;
		}
		
		FluidStack fluid = FluidContainerRegistry.getFluidForFilledItem(itemStack);
		Item item = itemStack.getItem();
		
		if (fluid == null) {
			if (!(item instanceof IFluidContainerItem)) {
				return changed;
			}
			
			IFluidContainerItem containerItem = (IFluidContainerItem) item;
			fluid = containerItem.getFluid(itemStack);
			
			if (fluid == null) {
				return changed;
			}
		}

		if (this.fuel == null && PetroleumFuel.isValidFuel(fluid.getFluidID())) {
			setCurrentFluid(fluid.getFluidID());
		}

		if (isCurrentFuel(fluid)) {
			boolean doFill = false;
			
			if (FluidContainerRegistry.isBucket(itemStack)) {
				ItemStack emptyBucket = itemStack.getItem().getContainerItem(itemStack);
				ItemStack otherBuckets = inventory[1];
				
				if (otherBuckets == null) {
					inventory[0] = null;
					inventory[1] = emptyBucket;
					doFill = true;
				} else if (item.getContainerItem() == otherBuckets.getItem()) {
					int stackLimit = otherBuckets.getItem().getItemStackLimit(otherBuckets);
					
					if (otherBuckets.stackSize < stackLimit) {
						otherBuckets.stackSize++;
						inventory[0] = null;
						inventory[1] = otherBuckets;
						doFill = true;
					}
				}
			} else if (item instanceof IFluidContainerItem) {		
				ItemStack emptyContainer = new ItemStack(item);
				ItemStack otherEmptyContainers = inventory[1];
				
				if (otherEmptyContainers == null) {
					decrStackSize(0, 1);
					inventory[1] = emptyContainer;
					doFill = true;
				} else if (item == otherEmptyContainers.getItem()) {
					int stackLimit = otherEmptyContainers.getItem().getItemStackLimit(otherEmptyContainers);
					
					if (otherEmptyContainers.stackSize < stackLimit) {
						decrStackSize(0, 1);
						otherEmptyContainers.stackSize++;
						inventory[1] = otherEmptyContainers;
						doFill = true;
					}
				}
			}
			
			if (doFill) {
				fill(fluid, true);
				markDirty();
				changed = true;
			}
		}

		return changed;
	}

	private boolean isCurrentFuel(FluidStack resource) {
		if (fuel == null || resource == null || fuel.getFluidId() != resource.getFluidID()) {
			return false;
		}
		return true;
	}

	public void setCurrentFluid(int fluidId) {
		this.fuel = PetroleumFuel.getFuelByItemId(fluidId);
		if (fuel != null) {
			this.currentEu = fuel.getEuPacketSize();
		}
	}

	public int getCurrentFluidId() {
		if (fuel != null) {
			return fuel.getFluidId();
		} else {
			return 0;
		}
	}

	public boolean isAddedToEnergyNet() {
		return initialized;
	}

	@Override
	public void invalidate() {
		// remove from EnergyNet when invalidating the TE
		if (worldObj != null && !worldObj.isRemote && initialized) {
			EnergyTileUnloadEvent event = new EnergyTileUnloadEvent(this);
			MinecraftForge.EVENT_BUS.post(event);
			initialized = false;
		}

		super.invalidate();
	}

	public void updateRedstoneStatus() {
		hasRedstonePower = worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord);
	}

	@Override
	public int getSourceTier() {		
		return SOURCE_TIER_LV;
	}

	@Override
	public boolean emitsEnergyTo(TileEntity receiver, ForgeDirection direction) {
		return true;
	}

	@Override
	public double getOfferedEnergy() {
		int offered = 0;

		if (!hasRedstonePower && charge > 0) {
			offered = Math.min(charge, currentEu);
		}

		if (offered == 0) {
			isDrainingEnergy = false;
		}

		return offered;
	}

	@Override
	public void drawEnergy(double amount) {
		charge -= amount;

		if (amount != 0) {
			isDrainingEnergy = true;
			markDirty();
		}
	}

	@Override
	public int getSizeInventory() {
		return inventory.length;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		if (slot >= inventory.length) {
			throw new ArrayIndexOutOfBoundsException("slot requested is >= inventory.length");
		}

		return inventory[slot];
	}

	@Override
	public ItemStack decrStackSize(int i, int j) {
		if (inventory[i] != null) {
			if (inventory[i].stackSize <= j) {
				ItemStack itemstack = inventory[i];
				inventory[i] = null;
				markDirty();
				return itemstack;
			}

			ItemStack itemstack1 = inventory[i].splitStack(j);
			if (inventory[i].stackSize == 0) {
				inventory[i] = null;
			}

			markDirty();
			return itemstack1;
		} else {
			return null;
		}
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int var1) {
		if (this.inventory[var1] != null) {
			ItemStack var2 = this.inventory[var1];
			this.inventory[var1] = null;
			return var2;
		} else {
			return null;
		}
	}

	@Override
	public void setInventorySlotContents(int i, ItemStack itemStack) {
		inventory[i] = itemStack;
		if (itemStack != null && itemStack.stackSize > getInventoryStackLimit()) {
			itemStack.stackSize = getInventoryStackLimit();
		}

		markDirty();
	}

	@Override
	public String getInventoryName() {
		return "TileEntityPetroleumGenerator";
	}

	@Override
	public boolean hasCustomInventoryName() {
		return false;
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer entityPlayer) {
		if (worldObj == null) {
			return true;
		}
		if (worldObj.getTileEntity(xCoord, yCoord, zCoord) != this) {
			return false;
		}

		return entityPlayer.getDistanceSq((double) xCoord + 0.5D, (double) yCoord + 0.5D,
				(double) zCoord + 0.5D) <= 64D;
	}

	@Override
	public void openInventory() {
	}

	@Override
	public void closeInventory() {
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack itemStack) {
		if (slot == 0) {
			FluidStack fluidStack = FluidContainerRegistry.getFluidForFilledItem(itemStack);
			if (fluidStack == null || !PetroleumFuel.isValidFuel(fluidStack.getFluidID())) {
				return false;
			}

			ItemStack stack = inventory[slot];
			if (stack == null) {
				return true;
			}

			return stack.isItemEqual(itemStack);
		}

		return false;
	}

	public int getScaledVolume() {
		double ratio = MAX_VOLUME / (double)FUEL_GAUGE_SCALE;
		double scaled_volume = amount / ratio;
		return ((int) Math.round(scaled_volume));
	}

	public int getScaledEnergy() {
		double ratio = MAX_CHARGE / (double)ENERGY_GAUGE_SCALE;
		double scaled_charge = charge / ratio;
		return ((int) Math.round(scaled_charge));
	}

}
