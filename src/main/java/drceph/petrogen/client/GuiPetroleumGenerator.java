package drceph.petrogen.client;

import org.lwjgl.opengl.GL11;

import drceph.petrogen.common.ContainerPetroleumGenerator;
import drceph.petrogen.common.PetroleumFuel;
import drceph.petrogen.common.TileEntityPetroleumGenerator;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.ResourceLocation;

public class GuiPetroleumGenerator extends GuiContainer {

	private TileEntityPetroleumGenerator tileEntity;

	public GuiPetroleumGenerator(IInventory player, TileEntityPetroleumGenerator tileEntity) {
		super(new ContainerPetroleumGenerator(player, tileEntity));
		this.tileEntity = tileEntity;
		this.allowUserInput = false;
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int p1, int p2) {
		this.drawCenteredString(fontRendererObj, "Petroleum Generator", this.xSize / 2, 4, 0x404040);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int i, int j) {
		ResourceLocation guiResource = new ResourceLocation("petrogen:textures/gui/guiPetroleumGenerator.png");

		GL11.glColor4f(1f, 1f, 1f, 1f);
		mc.renderEngine.bindTexture(guiResource);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		// fluid
		int fuelScaled = tileEntity.getScaledVolume();
		if (PetroleumFuel.isValidFuel(tileEntity.getCurrentFluidId())) {
			int fuelOffset = PetroleumFuel.getFuelByItemId(tileEntity.getCurrentFluidId()).getGuiOffset();
			if (fuelScaled > TileEntityPetroleumGenerator.FUEL_GAUGE_SCALE - 1) {
				fuelScaled = TileEntityPetroleumGenerator.FUEL_GAUGE_SCALE - 1;
			}

			drawTexturedModalRect(guiLeft + 50,
					guiTop + 14 + TileEntityPetroleumGenerator.FUEL_GAUGE_SCALE - 1 - fuelScaled,
					192 + (16 * fuelOffset), 0, 16, fuelScaled);
			drawTexturedModalRect(guiLeft + 50, guiTop + 14, 176, 0, 16,
					TileEntityPetroleumGenerator.FUEL_GAUGE_SCALE - 1); // fluid_scale_marks
		}

		// energy
		int energyScaled = tileEntity.getScaledEnergy();
		drawTexturedModalRect(guiLeft + 113, guiTop + 35, 176, 59, energyScaled, 17);

		// active
		if (tileEntity.isActive()) {
			drawTexturedModalRect(guiLeft + 83, guiTop + 38, 176, 76, 11, 11);
		}
	}

	@Override
	public void drawCenteredString(FontRenderer par1FontRenderer, String par2Str, int par3, int par4, int par5) {
		par1FontRenderer.drawString(par2Str, par3 - par1FontRenderer.getStringWidth(par2Str) / 2, par4, par5);
	}

}
