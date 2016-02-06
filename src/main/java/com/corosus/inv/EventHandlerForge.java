package com.corosus.inv;

import io.netty.util.internal.ThreadLocalRandom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayer.EnumStatus;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import CoroUtil.util.CoroUtilBlock;
import CoroUtil.util.CoroUtilPath;
import CoroUtil.util.Vec3;

import com.corosus.inv.ai.BehaviorModifier;
import com.corosus.inv.ai.tasks.TaskCallForHelp;
import com.corosus.inv.ai.tasks.TaskDigTowardsTarget;
import com.corosus.inv.config.ConfigAdvancedSpawning;
import com.corosus.inv.config.ConfigInvasion;

public class EventHandlerForge {
	
	public static String dataPlayerInvasionActive = "HW_dataPlayerInvasionActive";
	public static String dataPlayerServerTicks = "HW_dataPlayerServerTicks";
	public static String dataPlayerLastCacheEquipmentRating = "HW_dataPlayerLastCacheEquipmentRating";
	public static String dataPlayerInvasionWaveCountCur = "HW_dataPlayerInvasionWaveCountCur";
	public static String dataPlayerInvasionWaveCountMax = "HW_dataPlayerInvasionWaveCountMax";
	
	public float inventoryStages = 5;
	
	public HashMap<Integer, EquipmentForDifficulty> lookupDifficultyToEquipment = new HashMap<Integer, EquipmentForDifficulty>();
	
	public Class[] tasksToInject = new Class[] { TaskDigTowardsTarget.class, TaskCallForHelp.class };
	public int[] taskPriorities = {5, 5};
	
	public static class EquipmentForDifficulty {
		
		//ordered head to toe
		private List<ItemStack> listArmor;
		private ItemStack weapon;
		//unused for now, worth considering in future
		private List<Potion> listPotions;

		public EquipmentForDifficulty() {
			
		}
		
		public List<ItemStack> getListArmor() {
			return listArmor;
		}

		public void setListArmor(List<ItemStack> listArmor) {
			this.listArmor = listArmor;
		}

		public ItemStack getWeapon() {
			return weapon;
		}

		public void setWeapon(ItemStack weapon) {
			this.weapon = weapon;
		}

		public List<Potion> getListPotions() {
			return listPotions;
		}

		public void setListPotions(List<Potion> listPotions) {
			this.listPotions = listPotions;
		}
		
	}
	
	public EventHandlerForge() {
		
		//init inventories for difficulties
		
		
		EquipmentForDifficulty obj = new EquipmentForDifficulty();
		List<ItemStack> listItems = new ArrayList<ItemStack>();
		obj.setListArmor(listItems);
		lookupDifficultyToEquipment.put(0, obj);
		
		obj = new EquipmentForDifficulty();
		listItems = new ArrayList<ItemStack>();
		listItems.add(new ItemStack(Items.leather_helmet));
		listItems.add(new ItemStack(Items.leather_chestplate));
		listItems.add(new ItemStack(Items.leather_leggings));
		listItems.add(new ItemStack(Items.leather_boots));
		obj.setListArmor(listItems);
		obj.setWeapon(new ItemStack(Items.wooden_sword));
		lookupDifficultyToEquipment.put(1, obj);
		
		obj = new EquipmentForDifficulty();
		listItems = new ArrayList<ItemStack>();
		listItems.add(new ItemStack(Items.chainmail_helmet));
		listItems.add(new ItemStack(Items.chainmail_chestplate));
		listItems.add(new ItemStack(Items.chainmail_leggings));
		listItems.add(new ItemStack(Items.chainmail_boots));
		obj.setListArmor(listItems);
		obj.setWeapon(new ItemStack(Items.stone_sword));
		lookupDifficultyToEquipment.put(2, obj);
		
		obj = new EquipmentForDifficulty();
		listItems = new ArrayList<ItemStack>();
		listItems.add(new ItemStack(Items.iron_helmet));
		listItems.add(new ItemStack(Items.iron_chestplate));
		listItems.add(new ItemStack(Items.iron_leggings));
		listItems.add(new ItemStack(Items.iron_boots));
		obj.setListArmor(listItems);
		obj.setWeapon(new ItemStack(Items.iron_sword));
		lookupDifficultyToEquipment.put(3, obj);
		
		obj = new EquipmentForDifficulty();
		listItems = new ArrayList<ItemStack>();
		listItems.add(new ItemStack(Items.diamond_helmet));
		listItems.add(new ItemStack(Items.diamond_chestplate));
		listItems.add(new ItemStack(Items.diamond_leggings));
		listItems.add(new ItemStack(Items.diamond_boots));
		obj.setListArmor(listItems);
		obj.setWeapon(new ItemStack(Items.diamond_sword));
		lookupDifficultyToEquipment.put(4, obj);
	}
	
	@SubscribeEvent
	public void canSleep(PlayerSleepInBedEvent event) {
		if (ConfigInvasion.preventSleepDuringInvasions) {
			if (isInvasionTonight(event.entityPlayer.worldObj)) {
				EntityPlayerMP player = (EntityPlayerMP) event.entityPlayer;
				player.addChatMessage(new ChatComponentText("You can't sleep during invasion nights!"));
				event.result = EnumStatus.NOT_SAFE;
			} else {
				
			}
		}
	}
	
	@SubscribeEvent
	public void entityCreated(EntityJoinWorldEvent event) {
		if (event.entity instanceof EntityCreature) {
			EntityCreature ent = (EntityCreature) event.entity;
			if (ent.getEntityData().getBoolean(BehaviorModifier.dataEntityEnhanced)) {
				BehaviorModifier.addTaskIfMissing(ent, TaskDigTowardsTarget.class, tasksToInject, taskPriorities[0]);
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
						tickPlayer(player);
					}
				}
			}
		}
	}
	
	/**
	 * Ticked every 20 ticks
	 * 
	 * @param player
	 */
	public void tickPlayer(EntityPlayer player) {
		World world = player.worldObj;
		BlockPos pos = player.getPosition();
		
		long ticksPlayed = player.getEntityData().getLong(dataPlayerServerTicks);
		ticksPlayed += 20;
		//3 hour start debug
		//ticksPlayed = 20*60*60*3;
		player.getEntityData().setLong(dataPlayerServerTicks, ticksPlayed);
		
		float difficultyScale = getDifficultyScaleAverage(world, player, pos);
		///Chunk chunk = world.getChunkFromBlockCoords(pos);
		//long inhabTime = chunk.getInhabitedTime();
		//System.out.println("difficultyScale: " + difficultyScale);
		
		//start at "1"
		long dayNumber = (world.getWorldTime() / 24000) + 1;
		//System.out.println("daynumber: " + dayNumber + " - " + world.getWorldTime() + " - " + world.isDaytime());
		
		boolean invasionActive = false;
		
		//debug
		//difficultyScale = 1F;
		
		boolean activeBool = player.getEntityData().getBoolean(dataPlayerInvasionActive);
		
		//TODO: add a visual cue for invasion coming tonight + active invasion
		
		//track state of invasion for proper init and reset for wave counts, etc
		//new day starts just as sun is rising, so invasion stops just at the right time when sun is imminent, they burn 300 ticks before invasion ends, thats ok
		//FYI night val is based on sunlight level, so its not night ends @ 24000 cycle, its a bit before, 400ish ticks before, thats ok
		boolean invasionOnThisNight = isInvasionTonight(world);
		if (invasionOnThisNight && !world.isDaytime()) {
			
			invasionActive = true;
			//TODO: bug, on second day, invasion start method didnt trigger, but invasion IS active
			if (!activeBool) {
				//System.out.println("triggering invasion start");
				invasionStart(player, difficultyScale);
			}
		} else {
			invasionActive = false;
			if (activeBool) {
				//System.out.println("triggering invasion stop");
				invasionStopReset(player);
			}
		}
		
		//int playerRating = UtilPlayer.getPlayerRating(player);

		//System.out.println("invasion?: " + invasionActive + " - day# " + dayNumber + " - time: " + world.getWorldTime() + " - invasion tonight: " + invasionOnThisNight);
		//System.out.println("inv info: " + getInvasionDebug(difficultyScale));
		//System.out.println("player rating: " + playerRating);
		
		//debug
		//invasionActive = true;
		//world.getDifficultyForLocation(player.playerLocation);
		
		if (invasionActive) {
			if (player.onGround && world.getTotalWorldTime() % 50 == 0) {
			
				int range = getTargettingRangeBuff(difficultyScale);
				double moveSpeedAmp = 1.2D;
				
				//TODO: instead of this expensive method and entity iteration, we could make distant targetting a targetTask! 
				List<EntityCreature> listEnts = world.getEntitiesWithinAABB(EntityCreature.class, new AxisAlignedBB(pos, pos).expand(range, range, range));
				
				//System.out.println("ents: " + listEnts.size());
				
				TaskDigTowardsTarget task = new TaskDigTowardsTarget();
				
				int modifyRange = 100;
				float chanceToEnhance = getDigChanceBuff(difficultyScale);
				//TODO: consider making the digging tasks disable after invasions "ends" so that player wont get surprised later on in day if a zombie survives and takes a while to get to him
				BehaviorModifier.enhanceZombiesToDig(world, new Vec3(player.posX, player.posY, player.posZ), 
						tasksToInject, taskPriorities[0], 
						modifyRange, chanceToEnhance);
				
				for (EntityCreature ent : listEnts) {
					if (ent instanceof IMob && ent instanceof EntityCreature && !(ent instanceof EntityCreeper) && !(ent instanceof EntityEnderman)) {
						
						ent.setAttackTarget(player);
						CoroUtilPath.tryMoveToEntityLivingLongDist(ent, player, moveSpeedAmp);
						
					}
				}
			}
			
			if (world.getTotalWorldTime() % 10 == 0) {
				int spawnCountCur = player.getEntityData().getInteger(dataPlayerInvasionWaveCountCur);
				int spawnCountMax = player.getEntityData().getInteger(dataPlayerInvasionWaveCountMax);
				if (spawnCountCur < spawnCountMax) {
					boolean spawned = spawnNewMobSurface(player, difficultyScale);
					if (spawned) {
						spawnCountCur++;
						player.getEntityData().setInteger(dataPlayerInvasionWaveCountCur, spawnCountCur);
						//System.out.println("spawned mob, wave count: " + spawnCountCur + " of " + spawnCountMax);
					}
				}
			}
		}
	}
	
	public void invasionStart(EntityPlayer player, float difficultyScale) {
		//System.out.println("invasion started");
		player.addChatMessage(new ChatComponentText("An invasion has started! Be prepared!"));
		player.getEntityData().setBoolean(dataPlayerInvasionActive, true);
		
		player.getEntityData().setInteger(dataPlayerInvasionWaveCountMax, getSpawnCountBuff(difficultyScale));
		player.getEntityData().setInteger(dataPlayerInvasionWaveCountCur, 0);
	}
	
	public void invasionStopReset(EntityPlayer player) {
		//System.out.println("invasion ended");
		player.addChatMessage(new ChatComponentText("The invasion has ended! Next invasion in " + ConfigInvasion.daysBetweenInvasions + " days!"));
		player.getEntityData().setBoolean(dataPlayerInvasionActive, false);
	}
	
	public boolean spawnNewMobSurface(EntityLivingBase player, float difficultyScale) {
        
        //adjusted to work best with new targetting range base value of 30
        int minDist = 20;//ZAConfigSpawning.extraSpawningDistMin;
        int maxDist = 40;//ZAConfigSpawning.extraSpawningDistMax;
        int range = maxDist*2;
        
        Random rand = player.worldObj.rand;
        
        List<Class> spawnables = getSpawnableEntitiesForDifficulty(difficultyScale);
        
        if (spawnables.size() == 0) return false;
        
        for (int tries = 0; tries < 5; tries++) {
	        int tryX = MathHelper.floor_double(player.posX) - (range/2) + (rand.nextInt(range));
	        int tryZ = MathHelper.floor_double(player.posZ) - (range/2) + (rand.nextInt(range));
	        int tryY = player.worldObj.getHeight(new BlockPos(tryX, 0, tryZ)).getY();
	
	        if (player.getDistance(tryX, tryY, tryZ) < minDist || player.getDistance(tryX, tryY, tryZ) > maxDist || !canSpawnMob(player.worldObj, tryX, tryY, tryZ) || player.worldObj.getLightFromNeighbors(new BlockPos(tryX, tryY, tryZ)) >= 6) {
	        	//System.out.println("light: " + player.worldObj.getLightFromNeighbors(new BlockPos(tryX, tryY, tryZ)));
	            continue;
	        }
	
	        
	        
	        
	        try {
	        	int randSpawn = rand.nextInt(spawnables.size());
		        Class classToSpawn = spawnables.get(randSpawn);
	        	
	        	EntityCreature ent = (EntityCreature)classToSpawn.getConstructor(new Class[] {World.class}).newInstance(new Object[] {player.worldObj});
	        	
	        	ent.setPosition(tryX, tryY, tryZ);
				ent.onInitialSpawn(player.worldObj.getDifficultyForLocation(new BlockPos(ent)), (IEntityLivingData)null);
				enhanceMobForDifficulty(ent, difficultyScale);
				player.worldObj.spawnEntityInWorld(ent);
				ent.setAttackTarget(player);
			} catch (Exception e) {
				System.out.println("HW_Invasions: error spawning invasion entity: ");
				e.printStackTrace();
			}
	        
	        
	        /*EntityZombie entZ = new EntityZombie(player.worldObj);
			entZ.setPosition(tryX, tryY, tryZ);
			entZ.onInitialSpawn(player.worldObj.getDifficultyForLocation(new BlockPos(entZ)), (IEntityLivingData)null);
			enhanceMobForDifficulty(entZ, difficultyScale);
			player.worldObj.spawnEntityInWorld(entZ);
			
			entZ.setAttackTarget(player);*/
			
	        //if (ZAConfig.debugConsoleSpawns) ZombieAwareness.dbg("spawnNewMobSurface: " + tryX + ", " + tryY + ", " + tryZ);
			//System.out.println("spawnNewMobSurface: " + tryX + ", " + tryY + ", " + tryZ);
	        
	        return true;
        }
        
        return false;
    }
	
	public boolean canSpawnMob(World world, int x, int y, int z) {
        //Block id = world.getBlockState(new BlockPos(x-1,y,z)).getBlock();//Block.pressurePlatePlanks.blockID;
		Block id = world.getBlockState(new BlockPos(x,y,z)).getBlock();//Block.pressurePlatePlanks.blockID;

        /*if (id == Block.grass.blockID || id == Block.stone.blockID || id == Block.tallGrass.blockID || id == Block.grass.blockID || id == Block.sand.blockID) {
            return true;
        }*/
        if (CoroUtilBlock.isAir(id) || id.getMaterial() == Material.leaves) {
        	return false;
        }
        return true;
    }
	
	public void enhanceMobForDifficulty(EntityCreature ent, float difficultyScale) {
		/*settings to consider:
		 *- health
		 *- speed
		 *- inventory 
		 *- potions
		 */
		
		//determines what integer stage of inventory we should be at based on the difficulty scale
		//code adapts for allowing for easily adding in more inventory stages if needed
		
		//prevent enhanced children zombies
		if (ent instanceof EntityZombie) {
			EntityZombie zombie = (EntityZombie) ent;
			zombie.setChild(false);
		}
		
		//extra xp
		try {
			int xp = ObfuscationReflectionHelper.getPrivateValue(EntityLiving.class, ent, "field_70728_aV", "experienceValue");
			xp += difficultyScale * 10F;
			ObfuscationReflectionHelper.setPrivateValue(EntityLiving.class, ent, xp, "field_70728_aV", "experienceValue");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//movement speed buff
		double randBoost = ent.worldObj.rand.nextDouble() * 0.8D * difficultyScale;
		AttributeModifier speedBoostModifier = new AttributeModifier(MathHelper.getRandomUuid(ThreadLocalRandom.current()), "Invasion speed boost", randBoost, 1);
		ent.getEntityAttribute(SharedMonsterAttributes.movementSpeed).applyModifier(speedBoostModifier);
		
		int inventoryStage = getInventoryStageBuff(difficultyScale);
		
		EquipmentForDifficulty equipment = lookupDifficultyToEquipment.get(inventoryStage);
		if (equipment != null) {
			//allow for original weapon to remain if there was one and we are trying to remove it
			if (equipment.getWeapon() != null) setEquipment(ent, 0, equipment.getWeapon());
			//ent.setCurrentItemOrArmor(0, equipment.getWeapon());
			for (int i = 0; i < 4; i++) {
				if (equipment.getListArmor().size() >= i+1) {
					setEquipment(ent, i+1, equipment.getListArmor().get(i));
					//ent.setCurrentItemOrArmor(i+1, equipment.getListArmor().get(i));
				} else {
					setEquipment(ent, i+1, null);
					//ent.setCurrentItemOrArmor(i+1, null);
					
				}
			}
			//remove any chance of equipment dropping
			/*for (int i = 0; i < 5; i++) {
				ent.setEquipmentDropChance(i, 0);
			}*/
			
		} else {
			System.out.println("error, couldnt find equipment for difficulty value: " + inventoryStage);
		}
		
		
	}
	
	public static void setEquipment(EntityCreature ent, int slot, ItemStack stack) {
		if (slot == 0 && ent instanceof EntitySkeleton) {
			return;
		}
		ent.setCurrentItemOrArmor(slot, stack);
		ent.setEquipmentDropChance(slot, 0);
	}
	
	public static float getDifficultyScaleAverage(World world, EntityPlayer player, BlockPos pos) {
		float difficultyPos = getDifficultyScaleForPos(world, pos);
		float difficultyPlayerEquipment = getDifficultyScaleForPlayerEquipment(player);
		float difficultyPlayerServerTime = getDifficultyScaleForPlayerServerTime(player);
		float val = (difficultyPos + difficultyPlayerEquipment + difficultyPlayerServerTime) / 3F;
		val = Math.round(val * 1000F) / 1000F;
		return val;
	}
	
	public static float getDifficultyScaleForPlayerServerTime(EntityPlayer player) {
		long maxServerTime = ConfigInvasion.difficulty_MaxTicksOnServer;
		long curServerTime = player.getEntityData().getLong(dataPlayerServerTicks);
		return MathHelper.clamp_float((float)curServerTime / (float)maxServerTime, 0F, 1F);
	}
	
	public static float getDifficultyScaleForPlayerEquipment(EntityPlayer player) {
		int curRating = 0;
		if (player.getEntityData().hasKey(dataPlayerLastCacheEquipmentRating)) {
			if (player.worldObj.getTotalWorldTime() % 200 == 0) {
				curRating = UtilPlayer.getPlayerRating(player);
				player.getEntityData().setInteger(dataPlayerLastCacheEquipmentRating, curRating);
			} else {
				curRating = player.getEntityData().getInteger(dataPlayerLastCacheEquipmentRating);
			}
		} else {
			curRating = UtilPlayer.getPlayerRating(player);
			player.getEntityData().setInteger(dataPlayerLastCacheEquipmentRating, curRating);
		}
		
		int bestRating = UtilPlayer.getBestPlayerRatingPossible();
		
		//allow a scale value over 1F, means theres equipment in play beyond vanilla stuff, or i miscalculated some things
		return (float)curRating / (float)bestRating;
	}
	
	public static float getDifficultyScaleForPos(World world, BlockPos pos) {
		/**
		 * 1 chunk calc
		 */
		/*Chunk chunk = world.getChunkFromBlockCoords(pos);
		if (chunk != null) {
			long inhabTime = chunk.getInhabitedTime();
			float scale = convertInhabTimeToDifficultyScale(inhabTime);
			return scale;
			
		}
		return 0F;*/
		
		/**
		 * average radius calc
		 */
		int chunkRange = 3;
		int chunkX = pos.getX() / 16;
		int chunkZ = pos.getZ() / 16;
		int count = 0;
		long totalTime = 0;
		for (int x = chunkX - chunkRange; x < chunkX + chunkRange; x++) {
			for (int z = chunkZ - chunkRange; z < chunkZ + chunkRange; z++) {
				BlockPos checkPos = new BlockPos(chunkX * 16 + 8, 128, chunkZ * 16 + 8);
				if (world.isBlockLoaded(checkPos)) {
					Chunk chunk = world.getChunkFromBlockCoords(checkPos);
					if (chunk != null) {
						totalTime += chunk.getInhabitedTime();
						count++;
					}
				}
			}
		}
		long averageTime = totalTime / count;
		
		float scale = convertInhabTimeToDifficultyScale(averageTime);
		return scale;
		
		/**
		 * best chunk count
		 */
		/*int chunkRange = 4;
		int chunkX = pos.getX() / 16;
		int chunkZ = pos.getZ() / 16;
		//int count = 0;
		long bestTime = 0;
		for (int x = chunkX - chunkRange; x < chunkX + chunkRange; x++) {
			for (int z = chunkZ - chunkRange; z < chunkZ + chunkRange; z++) {
				BlockPos checkPos = new BlockPos(chunkX * 16 + 8, 128, chunkZ * 16 + 8);
				if (world.isBlockLoaded(checkPos)) {
					Chunk chunk = world.getChunkFromBlockCoords(checkPos);
					if (chunk != null) {
						if (chunk.getInhabitedTime() > bestTime) {
							bestTime = chunk.getInhabitedTime();
						}
					}
				}
			}
		}
		//long averageTime = bestTime / count;
		
		float scale = convertInhabTimeToDifficultyScale(bestTime);
		return scale;*/
	}
	
	/**
	 * 
	 * Returns value between 0 and 1 based on configured values
	 * 
	 * @param inhabTime
	 * @return
	 */
	public static float convertInhabTimeToDifficultyScale(long inhabTime) {
		float scale = (float)inhabTime / (float)ConfigInvasion.difficulty_MaxTicksInChunk;
		return scale;
	}
	
	public boolean isInvasionTonight(World world) {
		//add 1 day because calculation is off, eg: if we want 1 warmup day, we dont want first night to be an invasion
		int dayAdjust = ConfigInvasion.warmupDaysToFirstInvasion + 1;
		long dayNumber = (world.getWorldTime() / 24000) + 1;
		return dayNumber >= dayAdjust && (dayNumber-dayAdjust == 0 || (dayNumber-dayAdjust) % Math.max(1, ConfigInvasion.daysBetweenInvasions) == 0);
	}
	
	public int getSpawnCountBuff(float difficultyScale) {
		int initialSpawns = ConfigInvasion.invasion_Spawns_Min;
		int maxSpawnsAllowed = ConfigInvasion.invasion_Spawns_Max;
		float scaleRate = (float) ConfigInvasion.invasion_Spawns_ScaleRate;
		return MathHelper.clamp_int(((int) ((float)(maxSpawnsAllowed) * difficultyScale * scaleRate)), initialSpawns, maxSpawnsAllowed);
	}
	
	public int getTargettingRangeBuff(float difficultyScale) {
		int initialRange = ConfigInvasion.invasion_TargettingRange_Min;
		int max = ConfigInvasion.invasion_TargettingRange_Max;
		float scaleRate = (float) ConfigInvasion.invasion_TargettingRange_ScaleRate;
		return MathHelper.clamp_int(((int) ((float)(max) * difficultyScale * scaleRate)), initialRange, max); 
	}
	
	public float getDigChanceBuff(float difficultyScale) {
		float initial = (float) ConfigInvasion.invasion_DiggerConvertChance_Min;
		float max = (float) ConfigInvasion.invasion_DiggerConvertChance_Max;
		float scaleRate = (float) ConfigInvasion.invasion_DiggerConvertChance_ScaleRate;
		return MathHelper.clamp_float((((float)(max) * difficultyScale * scaleRate)), initial, max);
	}
	
	public int getInventoryStageBuff(float difficultyScale) {
		float scaleDivide = 1F / inventoryStages;
		int inventoryStage = 0;
		for (int i = 0; i < inventoryStages; i++) {
			if (difficultyScale <= scaleDivide * (i+1)) {
				inventoryStage = i;
				break;
			}
		}
		return inventoryStage;
	}
	
	public String getInvasionDebug(float difficultyScale) {
		return "spawncount: " + getSpawnCountBuff(difficultyScale) + 
				" | targetrange: " + getTargettingRangeBuff(difficultyScale) + 
				" | dig chance: " + getDigChanceBuff(difficultyScale) + 
				" | inventory stage: " + getInventoryStageBuff(difficultyScale) + 
				" | scale: " + difficultyScale;
	}
	
	/**
	 * Returns a list of classes that are verified to extend EntityCreature
	 * 
	 * @param difficultyScale
	 * @return
	 */
	public List<Class> getSpawnableEntitiesForDifficulty(float difficultyScale) {
		try {
			List<Class> listSpawns = new ArrayList<Class>();
			String[] spawnArray = null;
			if (difficultyScale > 0.9F) {
				spawnArray = ConfigAdvancedSpawning.difficulty_9.split(",");
			} else if (difficultyScale > 0.8F) {
				spawnArray = ConfigAdvancedSpawning.difficulty_8.split(",");
			} else if (difficultyScale > 0.7F) {
				spawnArray = ConfigAdvancedSpawning.difficulty_7.split(",");
			} else if (difficultyScale > 0.6F) {
				spawnArray = ConfigAdvancedSpawning.difficulty_6.split(",");
			} else if (difficultyScale > 0.5F) {
				spawnArray = ConfigAdvancedSpawning.difficulty_5.split(",");
			} else if (difficultyScale > 0.4F) {
				spawnArray = ConfigAdvancedSpawning.difficulty_4.split(",");
			} else if (difficultyScale > 0.3F) {
				spawnArray = ConfigAdvancedSpawning.difficulty_3.split(",");
			} else if (difficultyScale > 0.2F) {
				spawnArray = ConfigAdvancedSpawning.difficulty_2.split(",");
			} else if (difficultyScale > 0.1F) {
				spawnArray = ConfigAdvancedSpawning.difficulty_1.split(",");
			} else if (difficultyScale > 0F) {
				spawnArray = ConfigAdvancedSpawning.difficulty_0.split(",");
			}
			if (spawnArray != null) {
				for (String entry : spawnArray) {
					try {
						Class clazz = Class.forName(entry.trim());
						if (!EntityCreature.class.isAssignableFrom(clazz)) {
							System.out.println("HW_Invasions: class not compatible, must extend EntityCreature, problem string: " + entry);
						} else {
							listSpawns.add(clazz);
						}
					} catch (ClassNotFoundException e) {
						System.out.println("HW_Invasions: unable to find class for string: " + entry);
					}
				}
			}
			return listSpawns;
		} catch (Exception e) {
			e.printStackTrace();
		}
		List<Class> listDefault = new ArrayList<Class>();
		listDefault.add(EntityZombie.class);
		return listDefault;
	}
}
