package drceph.petrogen.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;

public class PetroleumFuel {

	private static Map<Integer, PetroleumFuel> fuelRegistry = new HashMap<Integer, PetroleumFuel>();
	
	public static void registerFuel(PetroleumFuel fuel) {
		FluidStack fluid = fuel.fuel;
		
		PetroleumGenerator.logger.info("Adding fuel " + fluid.getUnlocalizedName() + " (Fluid-ID: " + fluid.getFluidID()
				+ ") => Potential: " + fuel.euPerBucket + ", Power: " + fuel.euPacketSize);
		fuelRegistry.put(fluid.getFluidID(), fuel);
	}
	
	public static PetroleumFuel getFuelByItemId(int itemId) {
		return fuelRegistry.get(itemId);
	}
	
	public static boolean isValidFuel(int itemId) {
		return fuelRegistry.containsKey(itemId);
	}
	
	public static Map<Integer, PetroleumFuel> getRegisteredFuels() {
		return Collections.unmodifiableMap(fuelRegistry);
	}
	
	private FluidStack fuel;
	private int euPerBucket;
	private int euPacketSize;
	private int guiOffset;
	
	public PetroleumFuel(FluidStack fluidStack, int euPerBucket,
			int euPacketSize, int offset) {
		this.fuel = fluidStack;
		this.euPerBucket = euPerBucket;
		this.euPacketSize = euPacketSize;
		this.guiOffset = offset;
	}

	public PetroleumFuel(Item item, int euPerBucket, int euPacketSize, int offset) {
		this(new ItemStack(item), euPerBucket, euPacketSize, offset);
	}

	@SuppressWarnings("deprecation")
	public PetroleumFuel(ItemStack itemStack, int euPerBucket,
			int euPacketSize, int offset) {
		if (itemStack.getItem().hasContainerItem()) {
			this.fuel = FluidContainerRegistry.getFluidForFilledItem(itemStack);
		} else {
			throw new IllegalArgumentException("No fluid for item exists");
		}
		
		this.euPerBucket = euPerBucket;
		this.euPacketSize = euPacketSize;
		this.guiOffset = offset;
	}
	
	public FluidStack getFuel() {
		return fuel;
	}
	
	public int getFluidId() {
		return fuel.getFluidID();
	}
	
	public int getEuPerBucket() {
		return euPerBucket;
	}
	
	public int getEuPerFluidUnit() {
		return euPerBucket / FluidContainerRegistry.BUCKET_VOLUME;
	}
	
	public int getEuPacketSize() {
		return euPacketSize;
	}
	
	public int getTicksForLiquidUnit() {
		return getEuPerFluidUnit() / euPacketSize;
	}
	
	public int getGuiOffset() {
		return guiOffset;
	}
	
}
