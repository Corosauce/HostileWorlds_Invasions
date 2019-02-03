package com.corosus.inv;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy
{
	
    public ClientProxy()
    {

    }

    @Override
    public void init()
    {
    	super.init();
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {

    }
    
    private static void addMapping(Class<? extends Entity> entityClass, Render render) {
		RenderingRegistry.registerEntityRenderingHandler(entityClass, render);
	}
    
    @Override
    public void addBlock(RegistryEvent.Register<Block> event, Block parBlock, String name, boolean creativeTab) {
    	super.addBlock(event, parBlock, name, creativeTab);
    }

    @Override
    public void addItemBlock(RegistryEvent.Register<Item> event, Item item) {
        super.addItemBlock(event, item);

        addItemModel(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }

    @Override
    public void addItem(RegistryEvent.Register<Item> event, Item item, String name) {
        super.addItem(event, item, name);

        addItemModel(item, 0, new ModelResourceLocation(Invasion.modID + ":" + name, "inventory"));
    }

    public void addItemModel(Item item, int meta, ModelResourceLocation location) {

        ModelLoader.setCustomModelResourceLocation(item, meta, location);

    }
}
