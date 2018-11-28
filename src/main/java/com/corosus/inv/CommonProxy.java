package com.corosus.inv;

import com.corosus.inv.block.BlockSacrifice;
import com.corosus.inv.block.TileEntitySacrifice;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod.EventBusSubscriber(modid = Invasion.modID)
public class CommonProxy implements IGuiHandler
{

	public static final String block_sacrifice_name = "block_sacrifice";

	@GameRegistry.ObjectHolder(Invasion.modID + ":" + block_sacrifice_name)
	public static Block blockSacrifice;
	
    public CommonProxy()
    {
    	
    }

	@SubscribeEvent
	public static void registerBlocks(RegistryEvent.Register<Block> event) {
		Invasion.proxy.addBlock(event, blockSacrifice = (new BlockSacrifice()), TileEntitySacrifice.class, block_sacrifice_name);
	}

	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> event) {
		Invasion.proxy.addItemBlock(event, new ItemBlock(blockSacrifice).setRegistryName(blockSacrifice.getRegistryName()));


	}

    public void init()
    {

    }

    public void preInit() {

	}

	public void postInit() {
		ResourceLocation group = new ResourceLocation(Invasion.modID, "hw_invasion");

		//TODO: RECIPES!!!

		GameRegistry.addShapedRecipe(new ResourceLocation(Invasion.modID, block_sacrifice_name), group,
				new ItemStack(blockSacrifice, 1), new Object[] {"X X", "   ", "X X", 'X', Items.GOLD_INGOT});
	}
    
	public void addBlock(RegistryEvent.Register<Block> event, Block block, Class tEnt, String unlocalizedName) {
		addBlock(event, block, tEnt, unlocalizedName, true);
	}
	
    public void addBlock(RegistryEvent.Register<Block> event, Block block, Class tEnt, String unlocalizedName, boolean creativeTab) {
		addBlock(event, block, unlocalizedName, creativeTab);
		GameRegistry.registerTileEntity(tEnt, Invasion.modID + ":" + unlocalizedName);
	}
	
    public void addBlock(RegistryEvent.Register<Block> event, Block parBlock, String unlocalizedName) {
    	addBlock(event, parBlock, unlocalizedName, true);
    }
    
	public void addBlock(RegistryEvent.Register<Block> event, Block parBlock, String unlocalizedName, boolean creativeTab) {
		//vanilla calls
		//GameRegistry.registerBlock(parBlock, unlocalizedName);
		
		parBlock.setUnlocalizedName(Invasion.modID + "." + unlocalizedName);
		parBlock.setRegistryName(/*Weather.modID + ":" + */unlocalizedName);

		parBlock.setCreativeTab(CreativeTabs.MISC);

		if (event != null) {
			event.getRegistry().register(parBlock);
		} else {
			//GameRegistry.register(parBlock);
		}

		//ForgeRegistries.BLOCKS.register(parBlock);
		//LanguageRegistry.addName(parBlock, blockNameBase);
	}

	public void addItemBlock(RegistryEvent.Register<Item> event, Item item) {
		event.getRegistry().register(item);
	}
	
	public void addItem(RegistryEvent.Register<Item> event, Item item, String name) {
		item.setUnlocalizedName(Invasion.modID + "." + name);
		//item.setRegistryName(new ResourceLocation(Weather.modID, name));
		item.setRegistryName(name);

		item.setCreativeTab(CreativeTabs.MISC);

		if (event != null) {
			event.getRegistry().register(item);
		} else {
			//GameRegistry.register(item);
		}

		//registerItemVariantModel(item, name, 0);

		//return item;
	}

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world,
            int x, int y, int z)
    {
        return null;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world,
            int x, int y, int z)
    {
        return null;
    }

}
