package drceph.petrogen.client;

import drceph.petrogen.common.CommonProxy;
import drceph.petrogen.common.TileEntityPetroleumGenerator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class ClientProxy extends CommonProxy {

	public static final int PETROLEUM_GENERATOR_GUI_ID = 0;
	
	@Override
	public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
		TileEntity te = world.getTileEntity(x, y, z);
		if (te != null && te instanceof TileEntityPetroleumGenerator && id == PETROLEUM_GENERATOR_GUI_ID) {
			TileEntityPetroleumGenerator temg = (TileEntityPetroleumGenerator) te;
			return new GuiPetroleumGenerator(player.inventory, temg);
		}
		
		return null;
	}

}
