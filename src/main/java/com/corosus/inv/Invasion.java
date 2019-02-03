package com.corosus.inv;

import com.corosus.inv.capabilities.ExtendedPlayerStorage;
import com.corosus.inv.capabilities.PlayerDataInstance;
import modconfig.ConfigMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;

import com.corosus.inv.config.ConfigAdvancedOptions;
import com.corosus.inv.config.ConfigInvasion;
import net.minecraftforge.fml.common.network.NetworkRegistry;

@Mod(modid = "hw_inv", name="HW_Invasions", version=Invasion.VERSION, acceptableRemoteVersions="*", dependencies="required-after:coroutil@[1.12.1-1.2.23,)")
public class Invasion {
	
	@Mod.Instance( value = "hw_inv" )
	public static Invasion instance;
	public static final String modID = CoroUtil.forge.CoroUtil.modID_HWInvasions;
    public static final String VERSION = "${version}";

    @SidedProxy(clientSide = "com.corosus.inv.ClientProxy", serverSide = "com.corosus.inv.CommonProxy")
    public static CommonProxy proxy;

    @CapabilityInject(PlayerDataInstance.class)
    public static final Capability<PlayerDataInstance> PLAYER_DATA_INSTANCE = null;
    
	@Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
		ConfigMod.addConfigFile(event, new ConfigInvasion());
		if (ConfigInvasion.enableAdvancedDeveloperConfigFiles) {
            ConfigMod.addConfigFile(event, new ConfigAdvancedOptions());
        }

        CapabilityManager.INSTANCE.register(PlayerDataInstance.class, new ExtendedPlayerStorage(), PlayerDataInstance.class);

        NetworkRegistry.INSTANCE.registerGuiHandler(this, InvasionGUIHandler.getInstance());

        proxy.preInit();
    }
    
	@Mod.EventHandler
    public void load(FMLInitializationEvent event)
    {
		MinecraftForge.EVENT_BUS.register(new EventHandlerForge());

		proxy.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
        proxy.postInit();
    }
    
    @Mod.EventHandler
	public void serverStarting(FMLServerStartingEvent event) {
    	event.registerServerCommand(new CommandInvasion());
    }
    
    @Mod.EventHandler
    public void serverStart(FMLServerStartedEvent event) {
    	
    }
    
    @Mod.EventHandler
    public void serverStop(FMLServerStoppedEvent event) {
    	
    }

}
