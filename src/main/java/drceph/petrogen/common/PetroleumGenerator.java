package drceph.petrogen.common;

import org.apache.logging.log4j.Logger;

import buildcraft.BuildCraftEnergy;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import ic2.api.item.IC2Items;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.config.Configuration;

@Mod(modid = "petrogen", name = "Petroleum Generator", version = "1.2.1")
public class PetroleumGenerator {

    @Mod.Instance("petrogen")
    public static PetroleumGenerator instance;
    
    @SidedProxy(clientSide = "drceph.petrogen.client.ClientProxy", serverSide = "drceph.petrogen.common.CommonProxy")
    public static CommonProxy proxy;

    private static final int DEFAULT_OIL_MULTIPLIER = 3;
    private static final int DEFAULT_FUEL_MULTIPLIER = 12;
    private static final int OIL_POWER = 10;
    private static final int FUEL_POWER = 25;
    private static final int OIL_STEP = 10000;
    private static final int FUEL_STEP = 25000;
    private static final int OIL_GUI_OFFSET = 0;
    private static final int FUEL_GUI_OFFSET = 1;

    public static Logger logger;

    private int fuelMultiplier;
    private int oilMultiplier;
    private int bituminousBurnTime;

    private Block petroleumGeneratorBlock;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();

        Configuration configuration = new Configuration(event.getSuggestedConfigurationFile());

        configuration.addCustomCategoryComment(Configuration.CATEGORY_GENERAL, "Oil is multiplied by 10,000 for"
        		+ " the total EU/bucket (Default: 10,000 x 3 : 30,000 EU) \nFuel is multiplied by 25,000 for the"
        		+ " total EU/bucket (Default: 25,000 x 12 : 300,000 EU)");
        oilMultiplier = configuration.get(Configuration.CATEGORY_GENERAL, "oil_multiplier", DEFAULT_OIL_MULTIPLIER).getInt();
        oilMultiplier = Math.max(oilMultiplier, 1);
        fuelMultiplier = configuration.get(Configuration.CATEGORY_GENERAL, "fuel_multiplier", DEFAULT_FUEL_MULTIPLIER).getInt();
        fuelMultiplier = Math.max(fuelMultiplier, 1);

        bituminousBurnTime = configuration.get(Configuration.CATEGORY_GENERAL, "bituminous_sludge_burntime", 1200, "Default is 75%"
        		+ " of coal burntime - set to zero to disable use of sludge as a fuel\nThis feature is currently not available").getInt();
        bituminousBurnTime = Math.max(bituminousBurnTime, 0);

        configuration.save();
    }

	@Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        petroleumGeneratorBlock = new BlockPetroleumGenerator();
        petroleumGeneratorBlock.setBlockName("blockPetroleumGenerator");
        petroleumGeneratorBlock.setHardness(2.0f);
        petroleumGeneratorBlock.setStepSound(Block.soundTypeMetal);
        petroleumGeneratorBlock.setHarvestLevel("pickaxe", 2);
        
        GameRegistry.registerBlock(petroleumGeneratorBlock, petroleumGeneratorBlock.getUnlocalizedName());
        ItemStack generatorStack = IC2Items.getItem("generator");
        ItemStack waterCellStack = IC2Items.getItem("waterCell");
        ItemStack pistonStack = new ItemStack(Block.getBlockFromName("piston"));
        ItemStack flintAndSteelStack = new ItemStack(Item.getItemById(259));
        
        GameRegistry.addRecipe(new ItemStack(petroleumGeneratorBlock), " G ", "CPC", "CFC", 'G',
        		generatorStack, 'P', pistonStack, 'F', flintAndSteelStack, 'C', waterCellStack);
        GameRegistry.registerTileEntity(TileEntityPetroleumGenerator.class, "TileEntityPetroleumGenerator");
        
		PetroleumFuel oilFuel = new PetroleumFuel(new ItemStack(BuildCraftEnergy.bucketOil), OIL_STEP * oilMultiplier,
				OIL_POWER, OIL_GUI_OFFSET);
		PetroleumFuel fuelFuel = new PetroleumFuel(new ItemStack(BuildCraftEnergy.bucketFuel),
				FUEL_STEP * fuelMultiplier, FUEL_POWER, FUEL_GUI_OFFSET);
		PetroleumFuel.registerFuel(oilFuel);
		PetroleumFuel.registerFuel(fuelFuel);
        
        NetworkRegistry.INSTANCE.registerGuiHandler(this, proxy);
    }

}