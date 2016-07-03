package drceph.petrogen.common;

import java.util.ArrayList;
import java.util.Random;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import drceph.petrogen.client.ClientProxy;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockPetroleumGenerator extends BlockContainer {

	private static final Random RANDOM = new Random();
	private IIcon[] iconBuffer;
	
    protected BlockPetroleumGenerator() {
        super(Material.iron);

        this.setCreativeTab(CreativeTabs.tabRedstone);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerBlockIcons(IIconRegister iconRegister) {
    	iconBuffer = new IIcon[2 * 2 * 6];
    	
    	//Case 0: Generator is OFF and NORTH/SOUTH
    	iconBuffer[0] = iconRegister.registerIcon("petrogen:petroleumGeneratorSide"); //bottom
    	iconBuffer[1] = iconRegister.registerIcon("petrogen:petroleumGeneratorSide"); //top
    	iconBuffer[2] = iconRegister.registerIcon("petrogen:petroleumGeneratorSide"); //north
    	iconBuffer[3] = iconRegister.registerIcon("petrogen:petroleumGeneratorSide"); //south
    	iconBuffer[4] = iconRegister.registerIcon("petrogen:petroleumGeneratorFrontOff"); //west
    	iconBuffer[5] = iconRegister.registerIcon("petrogen:petroleumGeneratorFrontOff"); //east
    	
    	//Case 1: Generator is OFF and EAST/WEST
    	iconBuffer[6] = iconRegister.registerIcon("petrogen:petroleumGeneratorTopBottom"); //bottom
    	iconBuffer[7] = iconRegister.registerIcon("petrogen:petroleumGeneratorTopBottom"); //top
    	iconBuffer[8] = iconRegister.registerIcon("petrogen:petroleumGeneratorFrontOff"); //north
    	iconBuffer[9] = iconRegister.registerIcon("petrogen:petroleumGeneratorFrontOff"); //south
    	iconBuffer[10] = iconRegister.registerIcon("petrogen:petroleumGeneratorSide"); //west
    	iconBuffer[11] = iconRegister.registerIcon("petrogen:petroleumGeneratorSide"); //east
    	
    	//Case 2: Generator is ON and NORTH/SOUTH
    	iconBuffer[12] = iconRegister.registerIcon("petrogen:petroleumGeneratorSide"); //bottom
    	iconBuffer[13] = iconRegister.registerIcon("petrogen:petroleumGeneratorSide"); //top
    	iconBuffer[14] = iconRegister.registerIcon("petrogen:petroleumGeneratorSide"); //north
    	iconBuffer[15] = iconRegister.registerIcon("petrogen:petroleumGeneratorSide"); //south
    	iconBuffer[16] = iconRegister.registerIcon("petrogen:petroleumGeneratorFrontOn"); //west
    	iconBuffer[17] = iconRegister.registerIcon("petrogen:petroleumGeneratorFrontOn"); //east
    	
    	//Case 3: Generator is ON and EAST/WEST
    	iconBuffer[18] = iconRegister.registerIcon("petrogen:petroleumGeneratorTopBottom"); //bottom
    	iconBuffer[19] = iconRegister.registerIcon("petrogen:petroleumGeneratorTopBottom"); //top
    	iconBuffer[20] = iconRegister.registerIcon("petrogen:petroleumGeneratorFrontOn"); //north
    	iconBuffer[21] = iconRegister.registerIcon("petrogen:petroleumGeneratorFrontOn"); //south
    	iconBuffer[22] = iconRegister.registerIcon("petrogen:petroleumGeneratorSide"); //west
    	iconBuffer[23] = iconRegister.registerIcon("petrogen:petroleumGeneratorSide"); //east
    }
    
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int i, float f, float g,
			float t) {
		// Just making an instance of the TileEntity that the player clicked on
		TileEntity tileEntity = world.getTileEntity(x, y, z);

		if (tileEntity == null || player.isSneaking()) {
			return false;
		}

		if (world.isRemote) {
			return true;
		}

		if (tileEntity instanceof TileEntityPetroleumGenerator) {
			player.openGui(PetroleumGenerator.instance, ClientProxy.PETROLEUM_GENERATOR_GUI_ID, world, x, y, z);
		}
		
		return true;
	}
    
	public static void updateBlockState(boolean active, World world, int x, int y, int z) {
		int currentMetadata = world.getBlockMetadata(x, y, z);
		int newMetadata = active ? 1 : 0;
		
		if ((currentMetadata & 0x2) != 0) {
			newMetadata = newMetadata | 0x2;
		}
		
		world.setBlockMetadataWithNotify(x, y, z, newMetadata, 2);
	}
    
	@SideOnly(Side.CLIENT)
    @Override
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int blockSide) {
    	TileEntity te = world.getTileEntity(x, y, z);
    	int metadata = te.getBlockMetadata();
    	
    	int offset = 0;
    	if ((metadata & 0x2) == 0) {
    		//Facing is east or west
    		offset += 6;
    	}
    	
    	if ((metadata & 0x1) != 0) {
    		offset += 12;
    	}
    	
    	return iconBuffer[blockSide + offset];
    }
    
	@SideOnly(Side.CLIENT)
    @Override
    public IIcon getIcon(int blockSide, int blockMeta) {
    	int offset = 0;
    	if ((blockMeta & 0x2) == 0) {
    		//Facing is east or west
    		offset += 6;
    	}
    	
    	return iconBuffer[blockSide + offset];
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        TileEntity tileEntity = new TileEntityPetroleumGenerator();
        return tileEntity;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z,
    		EntityLivingBase player, ItemStack itemStack) {
    	int facing = MathHelper.floor_double((double)(player.rotationYaw * 4.0f / 360f) + 0.5d) & 3;
    	int currentMetadata = world.getBlockMetadata(x, y, z);
    	    	
    	switch (facing) {
    	case 1:
    	case 3:
    		//Second bits specifies the facing
    		currentMetadata = 0x2 | currentMetadata;
    		break;
    	default:
    		currentMetadata = 0xFD & currentMetadata;
    	}
    	    	
		world.setBlockMetadataWithNotify(x, y, z, currentMetadata, 0x3);
    }
    
    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int par6) {
    	dropItems(world, x, y, z);
    	super.breakBlock(world, x, y, z, block, par6);
    }
    
    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
    	return super.getDrops(world, x, y, z, metadata, fortune);
    }
    
	private void dropItems(World world, int x, int y, int z) {
		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (!(tileEntity instanceof IInventory)) {
			return;
		}
		IInventory inventory = (IInventory) tileEntity;

		for (int i = 0; i < inventory.getSizeInventory(); i++) {
			ItemStack item = inventory.getStackInSlot(i);

			if (item != null && item.stackSize > 0) {
				float rx = RANDOM.nextFloat() * 0.8F + 0.1F;
				float ry = RANDOM.nextFloat() * 0.8F + 0.1F;
				float rz = RANDOM.nextFloat() * 0.8F + 0.1F;

				ItemStack newItem = new ItemStack(item.getItem(), item.stackSize, item.getItemDamage());
				if (item.hasTagCompound()) {
					newItem.setTagCompound((NBTTagCompound) item.getTagCompound().copy());
				}
				EntityItem entityItem = new EntityItem(world, x + rx, y + ry, z + rz, newItem);

				float factor = 0.05F;
				entityItem.motionX = RANDOM.nextGaussian() * factor;
				entityItem.motionY = RANDOM.nextGaussian() * factor + 0.2F;
				entityItem.motionZ = RANDOM.nextGaussian() * factor;
				world.spawnEntityInWorld(entityItem);
				item.stackSize = 0;
			}
		}
	}
	
	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z,
			Block neighborBlock) {
		super.onNeighborBlockChange(world, x, y, z, neighborBlock);
		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (tileEntity != null && tileEntity instanceof TileEntityPetroleumGenerator) {
			((TileEntityPetroleumGenerator) tileEntity).updateRedstoneStatus();
		}
	}

}
