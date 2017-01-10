package com.corosus.inv;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import CoroUtil.difficulty.UtilEntityBuffs;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayer.SleepResult;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import CoroUtil.util.BlockCoord;
import CoroUtil.util.CoroUtilBlock;
import CoroUtil.util.CoroUtilPath;
import CoroUtil.util.Vec3;
import CoroUtil.difficulty.DynamicDifficulty;

import com.corosus.inv.ai.BehaviorModifier;
import CoroUtil.ai.tasks.TaskDigTowardsTarget;
import com.corosus.inv.config.ConfigAdvancedOptions;
import com.corosus.inv.config.ConfigAdvancedSpawning;
import com.corosus.inv.config.ConfigInvasion;
import com.mojang.realmsclient.gui.ChatFormatting;

public class EventHandlerForge {
	
	/**
	 * TODO: features:
	 * x health boosts
	 * - config to make only mobs we spawn potential miners
	 * -- if config is true, detect if player is in cave, if so, try to spawn stuff in cave
	 * --- to solve cliffs bedrock base issue of having little to no invaders
	 * x DPS location specific calculator for better adapting to players / bases capabilities
	 * x- store data per chunk?
	 * - raining triggering invasions accidentally, switch to modulus time range for 'is night' 
	 */
	
	public static String dataPlayerInvasionActive = "HW_dataPlayerInvasionActive";
	public static String dataPlayerInvasionWaveCountCur = "HW_dataPlayerInvasionWaveCountCur";
	public static String dataPlayerInvasionWaveCountMax = "HW_dataPlayerInvasionWaveCountMax";
	public static String dataCreatureLastPathWithDelay = "CoroAI_HW_CreatureLastPathWithDelay";
	

	

	
	public EventHandlerForge() {
		
		//init inventories for difficulties
		
		

	}
	
	@SubscribeEvent
	public void canSleep(PlayerSleepInBedEvent event) {
		if (event.getEntityPlayer().worldObj.isRemote) return;
		if (ConfigInvasion.preventSleepDuringInvasions) {
			if (isInvasionTonight(event.getEntityPlayer().worldObj)) {
				EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
				player.addChatMessage(new TextComponentString("You can't sleep during invasion nights!"));
				event.setResult(SleepResult.NOT_SAFE);
			} else {
				
			}
		}
	}

	/*//TODO: generic task flagging and restoration system in CoroUtil
	@SubscribeEvent
	public void entityCreated(EntityJoinWorldEvent event) {
		if (event.getEntity().worldObj.isRemote) return;
		if (event.getEntity() instanceof EntityCreature) {
			EntityCreature ent = (EntityCreature) event.getEntity();
			if (ent.getEntityData().getBoolean(BehaviorModifier.dataEntityEnhanced)) {
				BehaviorModifier.addTaskIfMissing(ent, TaskDigTowardsTarget.class, UtilEntityBuffs.tasksToInjectInv, UtilEntityBuffs.taskPrioritiesInv[0]);
			}
		}
	}*/
	
	@SubscribeEvent
	public void tickServer(ServerTickEvent event) {
		
		if (event.phase == Phase.START) {
			//System.out.println("tick ZA");
			//ZombieAwareness.instance.onTick(MinecraftServer.getServer());
			
			
			World world = DimensionManager.getWorld(0);
			if (world != null) {
				if (world.getTotalWorldTime() % 20 == 0) {
					for (Object player : world.playerEntities) {
						tickPlayer((EntityPlayer)player);
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
		try {
			World world = player.worldObj;
			net.minecraft.util.math.Vec3d posVec = new net.minecraft.util.math.Vec3d(player.posX, player.posY + (player.getEyeHeight() - player.getDefaultEyeHeight()), player.posZ);//player.getPosition(1F);
			BlockCoord pos = new BlockCoord(MathHelper.floor_double(posVec.xCoord), MathHelper.floor_double(posVec.yCoord), MathHelper.floor_double(posVec.zCoord));
			
			
			
			float difficultyScale = DynamicDifficulty.getDifficultyScaleAverage(world, player, pos);
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
				if (player.onGround && world.getTotalWorldTime() % ConfigAdvancedOptions.aiTickRatePath == 0) {
				
					int range = getTargettingRangeBuff(difficultyScale);
					double moveSpeedAmp = 1D;
					
					//TODO: instead of this expensive method and entity iteration, we could make distant targetting a targetTask! 
					List<EntityCreature> listEnts = world.getEntitiesWithinAABB(EntityCreature.class, new AxisAlignedBB(pos.posX, pos.posY, pos.posZ, pos.posX, pos.posY, pos.posZ).expand(range, range, range));
					
					//System.out.println("ents: " + listEnts.size());
					
					
					
					for (EntityCreature ent : listEnts) {
						if (ent instanceof IMob && ent instanceof EntityCreature && !(ent instanceof EntityCreeper) && !(ent instanceof EntityEnderman)) {
							
							long lastPathWithDelay = ent.getEntityData().getLong(dataCreatureLastPathWithDelay);
							if (world.getTotalWorldTime() > lastPathWithDelay) {
							
								EntityPlayer targetPlayer = null;
								if (ent.getAttackTarget() == null || !(ent.getAttackTarget() instanceof EntityPlayer)) {
									targetPlayer = player;
								} else {
									targetPlayer = (EntityPlayer) ent.getAttackTarget();
								}
								
								ent.setAttackTarget(targetPlayer);
								CoroUtilPath.tryMoveToEntityLivingLongDist(ent, targetPlayer, moveSpeedAmp);
								
								int pathFindingDelay = ConfigAdvancedOptions.pathDelayBase;
								
								if (ent.getNavigator().getPath() != null)
					            {
					                PathPoint finalPathPoint = ent.getNavigator().getPath().getFinalPathPoint();
					                //if final path point is near player, thats good!
					                if (finalPathPoint != null && player.getDistanceSq(finalPathPoint.xCoord, finalPathPoint.yCoord, finalPathPoint.zCoord) < 1)
					                {
					                    pathFindingDelay = ConfigAdvancedOptions.pathDelayBase;
					                }
					                else
					                {
					                    pathFindingDelay += ConfigAdvancedOptions.pathFailDelayPenalty;
					                }
					            }
					            else
					            {
					                pathFindingDelay += ConfigAdvancedOptions.pathFailDelayPenalty;
					            }
								
								ent.getEntityData().setLong(dataCreatureLastPathWithDelay, world.getTotalWorldTime() + pathFindingDelay);
							}
							
						}
					}
				}
				
				if (world.getTotalWorldTime() % ConfigAdvancedOptions.aiTickRateEnhance == 0) {
					//TaskDigTowardsTarget task = new TaskDigTowardsTarget();
					
					int modifyRange = ConfigAdvancedOptions.aiEnhanceRange;
					float chanceToEnhance = getDigChanceBuff(difficultyScale);
					//TODO: consider making the digging tasks disable after invasions "ends" so that player wont get surprised later on in day if a zombie survives and takes a while to get to him
					BehaviorModifier.enhanceZombiesToDig(world, new Vec3(player.posX, player.posY, player.posZ),
							/*UtilEntityBuffs.tasksToInject, UtilEntityBuffs.taskPriorities[0],*/
							modifyRange, chanceToEnhance);
				}
				
				if (world.getTotalWorldTime() % ConfigAdvancedOptions.aiTickRateSpawning == 0) {
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
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public void invasionStart(EntityPlayer player, float difficultyScale) {
		//System.out.println("invasion started");
		player.addChatMessage(new TextComponentString(ChatFormatting.RED + "An invasion has started! Be prepared!"));
		player.getEntityData().setBoolean(dataPlayerInvasionActive, true);
		
		player.getEntityData().setInteger(dataPlayerInvasionWaveCountMax, getSpawnCountBuff(difficultyScale));
		player.getEntityData().setInteger(dataPlayerInvasionWaveCountCur, 0);
	}
	
	public void invasionStopReset(EntityPlayer player) {
		//System.out.println("invasion ended");
		player.addChatMessage(new TextComponentString(ChatFormatting.GREEN + "The invasion has ended! Next invasion in " + ConfigInvasion.daysBetweenInvasions + " days!"));
		player.getEntityData().setBoolean(dataPlayerInvasionActive, false);
	}
	
	public boolean spawnNewMobSurface(EntityLivingBase player, float difficultyScale) {
        
        //adjusted to work best with new targetting range base value of 30
        int minDist = ConfigAdvancedOptions.spawnRangeMin;//20;//ZAConfigSpawning.extraSpawningDistMin;
        int maxDist = ConfigAdvancedOptions.spawnRangeMax;//ZAConfigSpawning.extraSpawningDistMax;
        int range = maxDist*2;
        
        Random rand = player.worldObj.rand;
        
        List<Class> spawnables = getSpawnableEntitiesForDifficulty(difficultyScale);
        
        if (spawnables.size() == 0) return false;
        
        for (int tries = 0; tries < 5; tries++) {
	        int tryX = MathHelper.floor_double(player.posX) - (range/2) + (rand.nextInt(range));
	        int tryZ = MathHelper.floor_double(player.posZ) - (range/2) + (rand.nextInt(range));
	        int tryY = player.worldObj.getHeight(new BlockPos(tryX, 0, tryZ)).getY();
	
	        
	        if (player.getDistance(tryX, tryY, tryZ) < minDist || player.getDistance(tryX, tryY, tryZ) > maxDist || 
	        		!canSpawnMob(player.worldObj, tryX, tryY, tryZ) || player.worldObj.getLightFromNeighbors(new BlockPos(tryX, tryY, tryZ)) >= 6) {
	        	//System.out.println("light: " + player.worldObj.getLightFromNeighbors(new BlockCoord(tryX, tryY, tryZ)));
	            continue;
	        }
	
	        
	        
	        
	        try {
	        	int randSpawn = rand.nextInt(spawnables.size());
		        Class classToSpawn = spawnables.get(randSpawn);
	        	
	        	EntityCreature ent = (EntityCreature)classToSpawn.getConstructor(new Class[] {World.class}).newInstance(new Object[] {player.worldObj});
	        	
	        	ent.setPosition(tryX, tryY, tryZ);
				ent.onInitialSpawn(ent.worldObj.getDifficultyForLocation(new BlockPos(ent)), (IEntityLivingData)null);
				ent.getEntityData().setBoolean(BehaviorModifier.dataEntityWaveSpawned, true);
				enhanceMobForDifficulty(ent, difficultyScale);
				player.worldObj.spawnEntityInWorld(ent);
				ent.setAttackTarget(player);
			} catch (Exception e) {
				System.out.println("HW_Invasions: error spawning invasion entity: ");
				e.printStackTrace();
			}
	        
	        
	        /*EntityZombie entZ = new EntityZombie(player.worldObj);
			entZ.setPosition(tryX, tryY, tryZ);
			entZ.onInitialSpawn(player.worldObj.getDifficultyForLocation(new BlockCoord(entZ)), (IEntityLivingData)null);
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
        //Block id = world.getBlockState(new BlockCoord(x-1,y,z)).getBlock();//Block.pressurePlatePlanks.blockID;
		IBlockState state = world.getBlockState(new BlockPos(x,y,z));
		Block id = state.getBlock();//Block.pressurePlatePlanks.blockID;

        /*if (id == Block.grass.blockID || id == Block.stone.blockID || id == Block.tallGrass.blockID || id == Block.grass.blockID || id == Block.sand.blockID) {
            return true;
        }*/
        if (CoroUtilBlock.isAir(id) || state.getMaterial() == Material.LEAVES) {
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
		/*try {
			int xp = ObfuscationReflectionHelper.getPrivateValue(EntityLiving.class, ent, "field_70728_aV", "experienceValue");
			xp += difficultyScale * 10F;
			ObfuscationReflectionHelper.setPrivateValue(EntityLiving.class, ent, xp, "field_70728_aV", "experienceValue");
		} catch (Exception e) {
			e.printStackTrace();
		}*/
		
		//movement speed buff
		//TODO: clamp to 1.0 or account for other mods speed bosting, or both!
		double randBoost = ent.worldObj.rand.nextDouble() * ConfigAdvancedOptions.speedBoostBase * difficultyScale;
		AttributeModifier speedBoostModifier = new AttributeModifier(UUID.fromString("B9766B59-9566-4402-BC1F-2EE2A276D836"), "Invasion speed boost", randBoost, 1);
		ent.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).applyModifier(speedBoostModifier);
		
		/*int inventoryStage = getInventoryStageBuff(difficultyScale);
		
		EquipmentForDifficulty equipment = lookupDifficultyToEquipment.get(inventoryStage);
		if (equipment != null) {
			//allow for original weapon to remain if there was one and we are trying to remove it
			if (equipment.getWeapon() != null) setEquipment(ent, EntityEquipmentSlot.MAINHAND, equipment.getWeapon());
			//ent.setCurrentItemOrArmor(0, equipment.getWeapon());
			for (int i = 0; i < 4; i++) {
				//TODO: verify 1.10.2 update didnt mess with this, maybe rewrite a bit for new sane slot based system
				if (equipment.getListArmor().size() >= i+1) {
					setEquipment(ent, equipment.getSlotForSlotID(i)*//*i+1*//*, equipment.getListArmor().get(i));
					//ent.setCurrentItemOrArmor(i+1, equipment.getListArmor().get(i));
				} else {
					setEquipment(ent, equipment.getSlotForSlotID(i)*//*i+1*//*, null);
					//ent.setCurrentItemOrArmor(i+1, null);
					
				}
			}
			
		} else {
			System.out.println("error, couldnt find equipment for difficulty value: " + inventoryStage);
		}*/
		
		
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
	
	/*public String getInvasionDebug(float difficultyScale) {
		return "spawncount: " + getSpawnCountBuff(difficultyScale) + 
				" | targetrange: " + getTargettingRangeBuff(difficultyScale) + 
				" | dig chance: " + getDigChanceBuff(difficultyScale) + 
				" | inventory stage: " + getInventoryStageBuff(difficultyScale) + 
				" | scale: " + difficultyScale;
	}*/
	
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
			} else if (difficultyScale >= 0F) {
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
