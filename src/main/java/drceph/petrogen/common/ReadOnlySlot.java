package drceph.petrogen.common;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ReadOnlySlot extends Slot {

	public ReadOnlySlot(IInventory inventory, int slotId, int displayX, int displayY) {
		super(inventory, slotId, displayX, displayY);
	}
	
	@Override
	public boolean isItemValid(ItemStack itemStack) {
		return false;
	}

}
