package drceph.petrogen.common;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerPetroleumGenerator extends Container {

	public TileEntityPetroleumGenerator tileEntity;
	private EntityPlayer invokingPlayer;
	private int lastAmount;
	private int lastFluidId;
	private boolean lastActive;
	private int lastCharge;

	public ContainerPetroleumGenerator(IInventory playerInventory, TileEntityPetroleumGenerator tileEntity) {
		this.tileEntity = tileEntity;
		this.invokingPlayer = ((InventoryPlayer) playerInventory).player;
		layoutContainer(playerInventory, tileEntity);
	}

	private void layoutContainer(IInventory playerInventory, IInventory inventory) {
		addSlotToContainer(new Slot(inventory, 0, 22, 36));
		for (int inventoryRow = 0; inventoryRow < 3; inventoryRow++) {
			for (int inventoryColumn = 0; inventoryColumn < 9; inventoryColumn++) {
				addSlotToContainer(new Slot(playerInventory, inventoryColumn + inventoryRow * 9 + 9,
						8 + inventoryColumn * 18, 84 + inventoryRow * 18));
			}
		}

		for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
			addSlotToContainer(new Slot(playerInventory, hotbarSlot, 8 + hotbarSlot * 18, 142));
		}
	}

	@Override
	public boolean canInteractWith(EntityPlayer entityplayer) {
		return tileEntity.isUseableByPlayer(entityplayer);
	}

	public EntityPlayer getPlayer() {
		return invokingPlayer;
	}

	@Override
	public void addCraftingToCrafters(ICrafting crafter) {
		super.addCraftingToCrafters(crafter);

		crafter.sendProgressBarUpdate(this, 0, this.tileEntity.getFluidAmount());
		crafter.sendProgressBarUpdate(this, 1, this.tileEntity.getCurrentFluidId());
		crafter.sendProgressBarUpdate(this, 2, this.tileEntity.getCharge());
		crafter.sendProgressBarUpdate(this, 3, this.tileEntity.isActive() ? 1 : 0);
	}
	
	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();
		
		for (int i = 0; i < this.crafters.size(); i++) {
			ICrafting crafter = (ICrafting) this.crafters.get(i);
			if (this.lastAmount != this.tileEntity.getFluidAmount()) {
				crafter.sendProgressBarUpdate(this, 0, this.tileEntity.getFluidAmount());
			}
			if (this.lastFluidId != this.tileEntity.getCurrentFluidId()) {
				crafter.sendProgressBarUpdate(this, 1, this.tileEntity.getCurrentFluidId());
			}
			if (this.lastCharge != this.tileEntity.getCharge()) {
				crafter.sendProgressBarUpdate(this, 2, this.tileEntity.getCharge());
			}
			if (this.lastActive != this.tileEntity.isActive()) {
				crafter.sendProgressBarUpdate(this, 3, this.tileEntity.isActive() ? 1 : 0);
			}
		}

		this.lastAmount = this.tileEntity.getFluidAmount();
		this.lastFluidId = this.tileEntity.getCurrentFluidId();
		this.lastCharge = this.tileEntity.getCharge();
		this.lastActive = this.tileEntity.isActive();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void updateProgressBar(int par1, int par2) {
		super.updateProgressBar(par1, par2);

		if (par1 == 0) {
			this.tileEntity.setFluidAmount(par2);
		}
		if (par1 == 1) {
			this.tileEntity.setCurrentFluid(par2);
		}
		if (par1 == 2) {
			this.tileEntity.setCharge(par2);
		}
		if (par1 == 3) {
			this.tileEntity.setActive(par2 == 1);
		}
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer par1EntityPlayer, int slot) {
		ItemStack stack = null;
		Slot slotObject = (Slot) inventorySlots.get(slot);

		// null checks and checks if the item can be stacked (maxStackSize > 1)
		if (slotObject != null && slotObject.getHasStack()) {
			ItemStack stackInSlot = slotObject.getStack();
			stack = stackInSlot.copy();

			// merges the item into player inventory since its in the tileEntity
			// this assumes only 1 slot, for inventories with > 1 slots, check
			// out the Chest Container.
			if (slot == 0) {
				if (!mergeItemStack(stackInSlot, 1, inventorySlots.size(), true)) {
					return null;
				}
				// places it into the tileEntity is possible since its in the
				// player inventory
			} else if (!mergeItemStack(stackInSlot, 0, 1, false)) {
				return null;
			}

			if (stackInSlot.stackSize == 0) {
				slotObject.putStack(null);
			} else {
				slotObject.onSlotChanged();
			}
		}

		return stack;
	}

}
