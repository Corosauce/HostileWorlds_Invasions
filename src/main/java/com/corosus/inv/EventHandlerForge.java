package com.corosus.inv;

import java.util.*;

import CoroUtil.difficulty.UtilEntityBuffs;
import CoroUtil.difficulty.data.DataCondition;
import CoroUtil.difficulty.data.DataMobSpawnsTemplate;
import CoroUtil.difficulty.data.DifficultyDataReader;
import CoroUtil.difficulty.data.conditions.ConditionContext;
import CoroUtil.difficulty.data.conditions.ConditionDifficulty;
import CoroUtil.difficulty.data.conditions.ConditionInvasionNumber;
import CoroUtil.difficulty.data.conditions.ConditionRandom;
import CoroUtil.util.*;
import com.corosus.inv.capabilities.PlayerDataInstance;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayer.SleepResult;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import CoroUtil.difficulty.DynamicDifficulty;

import com.corosus.inv.ai.BehaviorModifier;
import com.corosus.inv.config.ConfigAdvancedOptions;
import com.corosus.inv.config.ConfigAdvancedSpawning;
import com.corosus.inv.config.ConfigInvasion;
import com.mojang.realmsclient.gui.ChatFormatting;

public class EventHandlerForge {


    public EventHandlerForge() {

	}

	@SubscribeEvent
	public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
		if (event.getObject() instanceof EntityPlayer) {
			event.addCapability(new ResourceLocation(Invasion.modID, "PlayerDataInstance"), new ICapabilitySerializable<NBTTagCompound>() {
				PlayerDataInstance instance = Invasion.PLAYER_DATA_INSTANCE.getDefaultInstance().setPlayer((EntityPlayer)event.getObject());

				@Override
				public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
					return capability == Invasion.PLAYER_DATA_INSTANCE;
				}

				@Override
				public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
					return capability == Invasion.PLAYER_DATA_INSTANCE ? Invasion.PLAYER_DATA_INSTANCE.<T>cast(this.instance) : null;
				}

				@Override
				public NBTTagCompound serializeNBT() {
					return (NBTTagCompound) Invasion.PLAYER_DATA_INSTANCE.getStorage().writeNBT(Invasion.PLAYER_DATA_INSTANCE, this.instance, null);
				}

				@Override
				public void deserializeNBT(NBTTagCompound nbt) {
					Invasion.PLAYER_DATA_INSTANCE.getStorage().readNBT(Invasion.PLAYER_DATA_INSTANCE, this.instance, null, nbt);
				}
			});
		}
	}
	
	@SubscribeEvent
	public void canSleep(PlayerSleepInBedEvent event) {
		if (event.getEntityPlayer().worldObj.isRemote) return;
		if (ConfigInvasion.preventSleepDuringInvasions) {
			if (!event.getEntityPlayer().worldObj.isDaytime() && InvasionManager.isInvasionTonight(event.getEntityPlayer().worldObj)) {
				EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
				if (CoroUtilEntity.canProcessForList(CoroUtilEntity.getName(player), ConfigAdvancedOptions.blackListPlayers, ConfigAdvancedOptions.useBlacklistAsWhitelist)) {
					player.addChatMessage(new TextComponentString("You can't sleep during invasion nights!"));
					event.setResult(SleepResult.NOT_SAFE);
				}
			} else {
				
			}
		}
	}

	@SubscribeEvent
	public void canDespawn(LivingSpawnEvent.AllowDespawn event) {

		//prevent invasion spawned entities from despawning during invasion if they are too far away

		if (!event.getEntity().worldObj.isDaytime() && InvasionManager.isInvasionTonight(event.getEntity().worldObj)) {
			if (event.getEntity().getEntityData().getBoolean(UtilEntityBuffs.dataEntityWaveSpawned)) {
				event.setResult(Event.Result.DENY);
			}
		}
	}
	
	@SubscribeEvent
	public void tickServer(ServerTickEvent event) {
		
		if (event.phase == Phase.START) {
			//System.out.println("tick ZA");
			//ZombieAwareness.instance.onTick(MinecraftServer.getServer());
			
			
			World world = DimensionManager.getWorld(0);
			if (world != null) {
				if (world.getTotalWorldTime() % 20 == 0) {
					for (EntityPlayer player : world.playerEntities) {
						if (CoroUtilEntity.canProcessForList(CoroUtilEntity.getName(player), ConfigAdvancedOptions.blackListPlayers, ConfigAdvancedOptions.useBlacklistAsWhitelist)) {
							InvasionManager.tickPlayer(player);
						}
					}
				}
			}
		}
	}
}
