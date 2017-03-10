package com.corosus.inv;

import CoroUtil.difficulty.DynamicDifficulty;
import CoroUtil.difficulty.UtilEntityBuffs;
import CoroUtil.difficulty.data.DataCondition;
import CoroUtil.difficulty.data.DataMobSpawnsTemplate;
import CoroUtil.difficulty.data.DifficultyDataReader;
import CoroUtil.difficulty.data.conditions.ConditionContext;
import CoroUtil.difficulty.data.conditions.ConditionDifficulty;
import CoroUtil.difficulty.data.conditions.ConditionInvasionNumber;
import CoroUtil.difficulty.data.conditions.ConditionRandom;
import CoroUtil.util.*;
import com.corosus.inv.ai.BehaviorModifier;
import com.corosus.inv.capabilities.PlayerDataInstance;
import com.corosus.inv.config.ConfigAdvancedOptions;
import com.corosus.inv.config.ConfigAdvancedSpawning;
import com.corosus.inv.config.ConfigInvasion;
import com.mojang.realmsclient.gui.ChatFormatting;
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
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.EntityRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Corosus on 3/8/2017.
 */
public class InvasionManager {



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

    /*public static String dataPlayerInvasionActive = "HW_dataPlayerInvasionActive";
    public static String dataPlayerInvasionWarned = "HW_dataPlayerInvasionWarned";
    public static String dataPlayerInvasionWaveCountCur = "HW_dataPlayerInvasionWaveCountCur";
    public static String dataPlayerInvasionWaveCountMax = "HW_dataPlayerInvasionWaveCountMax";
    public static String dataCreatureLastPathWithDelay = "CoroAI_HW_CreatureLastPathWithDelay";*/
    public static String dataPlayerInvasionData = "HW_dataPlayerInvasionData";

    //public static HashMap<String, InvasionEntitySpawn> lookupUUIDToInvasionWave = new HashMap<>();

    /**
     *
     * Invasion reason redesign:
     *
     * 2 Invasion trigger ways
     * - Time
     * - Activity
     *
     * - Try to keep it shared between players / global, its fun to band together to fight them off, server events are fun
     *
     * - Do config for each
     *
     * - Time
     * -- Count up a timer instead of doing sketchy day number math
     * -- Track a central timer for dimension 0
     * --- support for per player ? why if its same time for all
     *
     * - Activity
     * -- Try to make it a shared value between players
     * -- Track a value, build it up when activity happens
     * -- When activityTracked > activityThreshold, schedule an invasion for that night
     *
     * -- What counts as activity? Ideas:
     * --- Mining resources
     * ---- How to handle quarries?
     *
     * --- Power usage / Pollution buildup like factorio
     * ---- Might need a whitelist / blacklist of mod machines
     * ----- for all power spending machines: blacklist stuff like batteries, trasmitters (wires)
     * ----- or whitelist only non batteries
     * ---- Using the various APIs, attempt to track their power consumption
     * N---- doesnt seem to be a standard way to track it spending energy, not even RF
     * ----- no sane way to hook into or detect it receiving energy...
     *
     * --- Player clicks
     * --- Player movement that maybe factors in distance travelled
     * --- Player mining time
     * --- Or instead of the 3 above, track hunger spending?
     *
     *
     * -- Use a generic activity value, and have things increase it, weighted by their severity
     *
     *
     * - Way to ward off invasions for specific player
     * -- must keep it global event
     * -- but allow a way for a player to prevent/reduce invasion entities specifically for them
     * -- maybe makes next invasion harder to compensate for reducing / cancelling pending invasion
     *
     *
     */

    public static boolean skipNextInvasionForPlayer(EntityPlayer player) {
        /**
         * mark in data they have a skip ready
         *
         * track amount of skips used up to 3, reset if they didnt skip invasion (factor in leaving as a skip)
         *
         * once code wants to start invasion for player, check for skip flag, dont do anything if present
         *
         * once invasion ends, count up consecutive skips used, remove skip flags,
         *
         * skipping counts as blood sacrifice, or leaving to cheat the invasion
         *
         * to make harder for next invasion:
         * manage a buff into coroutil ..... err, a thing that increases difficulty value, not a buff
         * during invasion, check for skip count
         * apply a +50% difficulty buff * skip count
         * somehow remove after invasion over
         */
        World world = player.worldObj;
        boolean skipped = player.getEntityData().getBoolean(DynamicDifficulty.dataPlayerInvasionSkipping);
        if (!skipped) {
            if (isInvasionTonight(world)) {
                //only allow skip during day before its actually active
                if (world.isDaytime()) {
                    int skipCountMax = 3;
                    int skipCount = player.getEntityData().getInteger(DynamicDifficulty.dataPlayerInvasionSkipCount);
                    if (skipCount < skipCountMax) {
                        skipCount++;
                        player.getEntityData().setBoolean(DynamicDifficulty.dataPlayerInvasionSkipping, true);
                        player.getEntityData().setInteger(DynamicDifficulty.dataPlayerInvasionSkipCount, skipCount);
                        player.addChatComponentMessage(new TextComponentString("Skipping tonights invasion, skip count: " + skipCount));
                        return true;
                    } else {
                        player.addChatComponentMessage(new TextComponentString("You've already skipped invasions " + skipCountMax + " times!"));
                    }

                } else {
                    player.addChatComponentMessage(new TextComponentString("Too late, invasion already started!"));
                }
            } else {
                player.addChatComponentMessage(new TextComponentString("Cant skip yet!"));
            }
        } else {
            player.addChatComponentMessage(new TextComponentString("You are already skipping this nights invasion!"));
        }
        return false;
    }



    /**
     * Ticked every 20 ticks
     *
     * @param player
     */
    public static void tickPlayer(EntityPlayer player) {
        try {
            World world = player.worldObj;
            net.minecraft.util.math.Vec3d posVec = new net.minecraft.util.math.Vec3d(player.posX, player.posY + (player.getEyeHeight() - player.getDefaultEyeHeight()), player.posZ);//player.getPosition(1F);
            BlockCoord pos = new BlockCoord(MathHelper.floor_double(posVec.xCoord), MathHelper.floor_double(posVec.yCoord), MathHelper.floor_double(posVec.zCoord));



            float difficultyScale = DynamicDifficulty.getDifficultyScaleAverage(world, player, pos);

            PlayerDataInstance storage = player.getCapability(Invasion.PLAYER_DATA_INSTANCE, null);

            //tickInvasionData(player, difficultyScale);

            ///Chunk chunk = world.getChunkFromBlockCoords(pos);
            //long inhabTime = chunk.getInhabitedTime();
            //System.out.println("difficultyScale: " + difficultyScale);

            //start at "1"
            long dayNumber = (world.getWorldTime() / 24000) + 1;
            //System.out.println("daynumber: " + dayNumber + " - " + world.getWorldTime() + " - " + world.isDaytime());

            boolean invasionActive = false;

            //debug
            //difficultyScale = 1F;

            boolean activeBool = storage.dataPlayerInvasionActive;
            boolean skippingBool = player.getEntityData().getBoolean(DynamicDifficulty.dataPlayerInvasionSkipping);

            //TODO: add a visual cue for invasion coming tonight + active invasion

            //track state of invasion for proper init and reset for wave counts, etc
            //new day starts just as sun is rising, so invasion stops just at the right time when sun is imminent, they burn 300 ticks before invasion ends, thats ok
            //FYI night val is based on sunlight level, so its not night ends @ 24000 cycle, its a bit before, 400ish ticks before, thats ok
            boolean invasionOnThisNight = isInvasionTonight(world);

            if (invasionOnThisNight && world.isDaytime()) {
                if (!storage.dataPlayerInvasionWarned) {
                    player.addChatComponentMessage(new TextComponentString(ChatFormatting.GOLD + "An invasion starts tonight! SpoOoOoky!"));
                    storage.dataPlayerInvasionWarned = true;
                }
            }

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

                    //before the skipping flag is reset for all, check if wasnt skipping, and reset their skip counter
                    //might be a better place to put this
                    if (!skippingBool) {
                        player.getEntityData().setInteger(DynamicDifficulty.dataPlayerInvasionSkipCount, 0);
                    }

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

            if (invasionActive && !skippingBool) {

                /**
                 * Target and path to player code
                 */

                if (player.onGround && world.getTotalWorldTime() % ConfigAdvancedOptions.aiTickRatePath == 0) {

                    int range = getTargettingRangeBuff(difficultyScale);
                    double moveSpeedAmp = 1D;

                    //TODO: instead of this expensive method and entity iteration, we could make distant targetting a targetTask!
                    List<EntityCreature> listEnts = world.getEntitiesWithinAABB(EntityCreature.class, new AxisAlignedBB(pos.posX, pos.posY, pos.posZ, pos.posX, pos.posY, pos.posZ).expand(range, range, range));

                    //System.out.println("ents: " + listEnts.size());



                    for (EntityCreature ent : listEnts) {
                        if (ent instanceof IMob && ent instanceof EntityCreature && !(ent instanceof EntityCreeper) && !(ent instanceof EntityEnderman)) {

                            long lastPathWithDelay = storage.dataCreatureLastPathWithDelay;
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

                                storage.dataCreatureLastPathWithDelay = world.getTotalWorldTime() + pathFindingDelay;
                            }

                        }
                    }
                }

                /**
                 * Buff with digging
                 */

                if (world.getTotalWorldTime() % ConfigAdvancedOptions.aiTickRateEnhance == 0) {
                    //TaskDigTowardsTarget task = new TaskDigTowardsTarget();

                    int modifyRange = ConfigAdvancedOptions.aiEnhanceRange;
                    float chanceToEnhance = getDigChanceBuff(difficultyScale);
                    //TODO: consider making the digging tasks disable after invasions "ends" so that player wont get surprised later on in day if a zombie survives and takes a while to get to him
                    BehaviorModifier.enhanceZombiesToDig(world, new Vec3(player.posX, player.posY, player.posZ),
							/*UtilEntityBuffs.tasksToInject, UtilEntityBuffs.taskPriorities[0],*/
                            modifyRange, chanceToEnhance);
                }

                /**
                 * Spawn extra with buffs
                 */

                if (world.getTotalWorldTime() % ConfigAdvancedOptions.aiTickRateSpawning == 0) {

                    spawnNewMobFromProfile(player, difficultyScale);

                    /*int spawnCountCur = player.getEntityData().getInteger(dataPlayerInvasionWaveCountCur);
                    int spawnCountMax = player.getEntityData().getInteger(dataPlayerInvasionWaveCountMax);
                    if (spawnCountCur < spawnCountMax) {
                        boolean spawned = spawnNewMobSurface(player, difficultyScale);
                        if (spawned) {
                            spawnCountCur++;
                            player.getEntityData().setInteger(dataPlayerInvasionWaveCountCur, spawnCountCur);
                            //System.out.println("spawned mob, wave count: " + spawnCountCur + " of " + spawnCountMax);
                        }
                    }*/
                }

                //tickSpawning(player, difficultyScale);

                /**
                 * new json backed spawn system
                 *
                 * - need way to support randomizing between types of invasions
                 * -- frost invaders
                 * -- flame invaders
                 *
                 * - look through all mob spawn profiles
                 * - filter by conditions except random one
                 * - do random one last
                 * -- random weight based evaluating
                 *
                 * - now we have a list of entities with cmods to spawn
                 * - transfer that list into a new currently unmade wave data class
                 * -- set invasion state to use decided entity list
                 *
                 * - class needs:
                 * -- per entity list
                 * --- entity to spawn
                 * --- amount spawned already
                 * --- amount to spawn (max)
                 * --- the cmods?
                 *
                 * - same experience for each player?
                 * -- simple option i guess
                 *
                 * - invasion instance per player either way
                 * -- player nbt json structure <-> invasion instance?
                 */


            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void tickSpawning(EntityPlayer player, float difficulty) {

    }

    public static void invasionStart(EntityPlayer player, float difficultyScale) {
        PlayerDataInstance storage = player.getCapability(Invasion.PLAYER_DATA_INSTANCE, null);
        //System.out.println("invasion started");
        if (player.getEntityData().getBoolean(DynamicDifficulty.dataPlayerInvasionSkipping)) {
            player.addChatMessage(new TextComponentString(ChatFormatting.GREEN + "An invasion has started! But skipped for you!"));
        } else {
            player.addChatMessage(new TextComponentString(ChatFormatting.RED + "An invasion has started! Be prepared!"));
        }

        //initNewInvasion(player, difficultyScale);

        storage.resetInvasion();
        storage.dataPlayerInvasionActive = true;

        DataMobSpawnsTemplate profile = chooseInvasionProfile(player, difficultyScale);
        if (profile != null) {
            storage.initNewInvasion(profile);
        } else {
            //TODO: no invasions for you! also this is bad?, perhaps hardcode a fallback default, what if no invasion is modpack makers intent
        }

        //TODO: readd spawn count scaling as an option via json
        //player.getEntityData().setInteger(dataPlayerInvasionWaveCountMax, getSpawnCountBuff(difficultyScale));
        //player.getEntityData().setInteger(dataPlayerInvasionWaveCountCur, 0);

        //add buff for player based on how many invasions they skipped (and only if this isnt a skipped invasion)
        if (!player.getEntityData().getBoolean(DynamicDifficulty.dataPlayerInvasionSkipping)) {
            float buffBase = 0.5F;
            float skipCount = player.getEntityData().getInteger(DynamicDifficulty.dataPlayerInvasionSkipCount);
            DynamicDifficulty.setInvasionSkipBuff(player, buffBase * skipCount);
        }
    }

    public static DataMobSpawnsTemplate getInvasionTestData(EntityPlayer player, float difficultyScale) {
        PlayerDataInstance storage = player.getCapability(Invasion.PLAYER_DATA_INSTANCE, null);

        DataMobSpawnsTemplate profile = chooseInvasionProfile(player, difficultyScale);
        /*if (profile != null) {
            storage.initNewInvasion(profile);
        } else {
            //TODO: no invasions for you! also this is bad?, perhaps hardcode a fallback default, what if no invasion is modpack makers intent
        }*/
        return profile;
    }

    public static void invasionStopReset(EntityPlayer player) {
        PlayerDataInstance storage = player.getCapability(Invasion.PLAYER_DATA_INSTANCE, null);
        //System.out.println("invasion ended");
        player.addChatMessage(new TextComponentString(ChatFormatting.GREEN + "The invasion has ended! Next invasion in " + ConfigInvasion.daysBetweenInvasions + " days!"));

        storage.dataPlayerInvasionActive = false;
        storage.dataPlayerInvasionWarned = false;
        storage.resetInvasion();

        player.getEntityData().setBoolean(DynamicDifficulty.dataPlayerInvasionSkipping, false);

        //remove invasion specific buff since invasion stopped
        DynamicDifficulty.setInvasionSkipBuff(player, 0);
    }

    public static boolean spawnNewMobFromProfile(EntityLivingBase player, float difficultyScale) {
        PlayerDataInstance storage = player.getCapability(Invasion.PLAYER_DATA_INSTANCE, null);
        int minDist = ConfigAdvancedOptions.spawnRangeMin;//20;//ZAConfigSpawning.extraSpawningDistMin;
        int maxDist = ConfigAdvancedOptions.spawnRangeMax;//ZAConfigSpawning.extraSpawningDistMax;
        int range = maxDist*2;

        Random rand = player.worldObj.rand;

        InvasionEntitySpawn randomEntityList = storage.getRandomEntityClassToSpawn();

        if (randomEntityList != null) {
            for (int tries = 0; tries < 5; tries++) {
                int tryX = MathHelper.floor_double(player.posX) - (range / 2) + (rand.nextInt(range));
                int tryZ = MathHelper.floor_double(player.posZ) - (range / 2) + (rand.nextInt(range));
                int tryY = player.worldObj.getHeight(new BlockPos(tryX, 0, tryZ)).getY();


                //TODO: make spawn check rules use entities own rules
                if (player.getDistance(tryX, tryY, tryZ) < minDist || player.getDistance(tryX, tryY, tryZ) > maxDist ||
                        !canSpawnMob(player.worldObj, tryX, tryY, tryZ) || player.worldObj.getLightFromNeighbors(new BlockPos(tryX, tryY, tryZ)) >= 6) {
                    //System.out.println("light: " + player.worldObj.getLightFromNeighbors(new BlockCoord(tryX, tryY, tryZ)));
                    continue;
                }

                try {


                    String spawn = randomEntityList.spawnProfile.entities.get(rand.nextInt(randomEntityList.spawnProfile.entities.size()));
                    Class classToSpawn = CoroUtilEntity.getClassFromRegisty(spawn);
                    if (classToSpawn != null) {
                        EntityCreature ent = (EntityCreature) classToSpawn.getConstructor(new Class[]{World.class}).newInstance(new Object[]{player.worldObj});

                        ent.setPosition(tryX, tryY, tryZ);
                        ent.onInitialSpawn(ent.worldObj.getDifficultyForLocation(new BlockPos(ent)), (IEntityLivingData) null);
                        ent.getEntityData().setBoolean(UtilEntityBuffs.dataEntityWaveSpawned, true);

                        //TODO: here we need to apply the cmods chosen for it

                        //old way
                        enhanceMobForDifficulty(ent, difficultyScale);

                        player.worldObj.spawnEntityInWorld(ent);
                        ent.setAttackTarget(player);

                        randomEntityList.spawnCountCurrent++;
                    } else {
                        System.out.println("could not find registered class for entity name: " + spawn);
                    }

                    //String spawn = storage.getRandomEntityClassToSpawn();


                    //Class classToSpawn = spawnables.get(randSpawn);


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
        }

        return false;
    }

    @Deprecated
    public static boolean spawnNewMobSurface(EntityLivingBase player, float difficultyScale) {

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
                ent.getEntityData().setBoolean(UtilEntityBuffs.dataEntityWaveSpawned, true);
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

    public static boolean canSpawnMob(World world, int x, int y, int z) {
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

    public static void enhanceMobForDifficulty(EntityCreature ent, float difficultyScale) {
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
        //TODO: clamp to 1.0 or account for other mods speed boosting, or both!
        double randBoost = ent.worldObj.rand.nextDouble() * ConfigAdvancedOptions.speedBoostBase * difficultyScale;
        AttributeModifier speedBoostModifier = new AttributeModifier(CoroUtilAttributes.SPEED_BOOST_UUID, "Invasion speed boost", randBoost, EnumAttribModifierType.INCREMENT_MULTIPLY_BASE.ordinal());
        if (!ent.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).hasModifier(speedBoostModifier)) {
            ent.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).applyModifier(speedBoostModifier);
        }

        UtilEntityBuffs.applyBuffSingularTry(UtilEntityBuffs.dataEntityBuffed_Inventory, ent, difficultyScale);

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



    public static boolean isInvasionTonight(World world) {
        //add 1 day because calculation is off, eg: if we want 1 warmup day, we dont want first night to be an invasion
        //switching to 0 indexed

        //adding +1 to ConfigInvasion.daysBetweenInvasions for the math because, eg:
        //- for 2, we want an invasion every 3rd day, so to skip 2 days, not do an invasion every 2 days

        int dayAdjust = ConfigInvasion.warmupDaysToFirstInvasion;
        long dayNumber = (world.getWorldTime() / 24000);
        return dayNumber >= dayAdjust &&
                (dayNumber-dayAdjust == 0 ||
                        (dayNumber-dayAdjust) % Math.max(1, ConfigInvasion.daysBetweenInvasions + 1) == 0);
    }

    /*public static int getSpawnCountBuff(float difficultyScale) {
        int initialSpawns = ConfigInvasion.invasion_Spawns_Min;
        int maxSpawnsAllowed = ConfigInvasion.invasion_Spawns_Max;
        float scaleRate = (float) ConfigInvasion.invasion_Spawns_ScaleRate;
        return MathHelper.clamp_int(((int) ((float)(maxSpawnsAllowed) * difficultyScale * scaleRate)), initialSpawns, maxSpawnsAllowed);
    }*/

    public static int getTargettingRangeBuff(float difficultyScale) {
        int initialRange = ConfigInvasion.invasion_TargettingRange_Min;
        int max = ConfigInvasion.invasion_TargettingRange_Max;
        float scaleRate = (float) ConfigInvasion.invasion_TargettingRange_ScaleRate;
        return MathHelper.clamp_int(((int) ((float)(max) * difficultyScale * scaleRate)), initialRange, max);
    }

    public static float getDigChanceBuff(float difficultyScale) {
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
    public static List<Class> getSpawnableEntitiesForDifficulty(float difficultyScale) {
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

    public static DataMobSpawnsTemplate chooseInvasionProfile(EntityPlayer player, float difficulty) {
        List<DataMobSpawnsTemplate> listPhase2 = new ArrayList<>();

        //System.out.println("phase 1 choice count: " + DifficultyDataReader.getData().listMobSpawnTemplates.size());
        for (DataMobSpawnsTemplate spawns : DifficultyDataReader.getData().listMobSpawnTemplates) {

            boolean fail = false;
            for (DataCondition condition : spawns.getConditionsFlattened()) {

                if (!(condition instanceof ConditionRandom)) {
                    if (!evaluateCondition(player, condition, difficulty)) {
                        fail = true;
                        break;
                    }
                }
            }

            if (!fail) {
                listPhase2.add(spawns);
            }

        }

        //do weighted random
        int totalWeight = 0;

        //index of weight should match index of listPhase2 entries
        List<Integer> listWeights = new ArrayList<>();
        //System.out.println("phase 1 choice count: " + listPhase2.size());
        for (DataMobSpawnsTemplate spawns : listPhase2) {

            //default 1 if no random found
            int weight = 1;
            for (DataCondition condition : spawns.getConditionsFlattened()) {

                //break on first match
                //TODO: how to deal with duplicates? (bad design if used)
                if (condition instanceof ConditionRandom) {
                    weight = ((ConditionRandom) condition).weight;
                    break;
                }
            }

            listWeights.add(weight);
            totalWeight += weight;
        }

        Random rand = new Random();
        int randVal = rand.nextInt(totalWeight);
        int index = -1;
        for (int i = 0; i < listWeights.size(); i++) {
            if (randVal < listWeights.get(i)) {
                index = i;
                break;
            }
            randVal -= listWeights.get(i);
        }

        if (index != -1) {
            return listPhase2.get(index);

            //System.out.println("final choice: " + spawn.name);
        } else {
            //System.out.println("design flaw!");
        }

        //temp
        return null;
    }

    //test method?
    public static void tickInvasionData(EntityPlayer player, float difficulty) {





        //TODO: uhh
        //DataMobSpawnsTemplate <-> storage.List<InvasionEntitySpawn>
        //dont need conditions
        //need instance info about actively spawned amount, go

        //storage.invasionEntitySpawn.

		/*String uuid = player.getPersistentID().toString();
		if (!lookupUUIDToInvasionWave.containsKey(uuid)) {
			lookupUUIDToInvasionWave.put(uuid, new InvasionEntitySpawn());
		}

		InvasionEntitySpawn data = lookupUUIDToInvasionWave.get(uuid);

		writeInvasionNBT(player);*/
    }



    public static boolean evaluateCondition(EntityPlayer player, DataCondition condition, float difficulty) {
        if (condition instanceof ConditionContext) {
            return true;
        } else if (condition instanceof ConditionDifficulty) {
            return difficulty >= ((ConditionDifficulty)condition).min && difficulty <= ((ConditionDifficulty)condition).max;
        } else if (condition instanceof ConditionInvasionNumber) {
            //TODO: implement invasion numbers, global or per player tracked?
            return false;
        }
        return false;
    }

	/*public static void writeInvasionNBT(EntityPlayer player) {
		String uuid = player.getPersistentID().toString();
		InvasionEntitySpawn data = lookupUUIDToInvasionWave.get(uuid);

		Gson json = new Gson();
		String dataJson = json.toJson(data);

		NBTTagCompound nbt = player.getEntityData().getCompoundTag(dataPlayerInvasionData);
	}*/
}
