package com.corosus.inv;

import CoroUtil.ai.tasks.EntityAIChaseFromFar;
import CoroUtil.ai.tasks.EntityAINearestAttackablePlayerOmniscience;
import CoroUtil.ai.tasks.TaskDigTowardsTarget;
import CoroUtil.difficulty.DynamicDifficulty;
import CoroUtil.difficulty.UtilEntityBuffs;
import CoroUtil.difficulty.data.DataCondition;
import CoroUtil.difficulty.data.spawns.DataMobSpawnsTemplate;
import CoroUtil.difficulty.data.DeserializerAllJson;
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
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

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
        World world = player.world;
        boolean skipped = player.getEntityData().getBoolean(DynamicDifficulty.dataPlayerInvasionSkipping);
        if (!skipped) {
            if (isInvasionTonight(world)) {
                //only allow skip during day before its actually active
                if (world.isDaytime()) {
                    int skipCount = player.getEntityData().getInteger(DynamicDifficulty.dataPlayerInvasionSkipCount);
                    if (skipCount < ConfigInvasion.maxConsecutiveInvasionSkips) {
                        skipCount++;
                        player.getEntityData().setBoolean(DynamicDifficulty.dataPlayerInvasionSkipping, true);
                        player.getEntityData().setInteger(DynamicDifficulty.dataPlayerInvasionSkipCount, skipCount);
                        player.sendMessage(new TextComponentString(TextFormatting.GREEN + "Skipping tonights invasion, skip count: " + skipCount));
                        return true;
                    } else {
                        player.sendMessage(new TextComponentString(TextFormatting.RED + "You've already skipped invasions " + ConfigInvasion.maxConsecutiveInvasionSkips + " times! You must fight!"));
                    }

                } else {
                    player.sendMessage(new TextComponentString(TextFormatting.RED + "Too late, invasion already started!"));
                }
            } else {
                player.sendMessage(new TextComponentString("Not an invasion night, cant skip yet!"));
            }
        } else {
            player.sendMessage(new TextComponentString(TextFormatting.GREEN + "You are already skipping this nights invasion!"));
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
            World world = player.world;
            net.minecraft.util.math.Vec3d posVec = new net.minecraft.util.math.Vec3d(player.posX, player.posY + (player.getEyeHeight() - player.getDefaultEyeHeight()), player.posZ);//player.getPosition(1F);
            BlockCoord pos = new BlockCoord(MathHelper.floor(posVec.x), MathHelper.floor(posVec.y), MathHelper.floor(posVec.z));



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
                    player.sendMessage(new TextComponentString(ChatFormatting.GOLD + "An invasion starts tonight! SpoOoOoky!"));
                    storage.dataPlayerInvasionWarned = true;
                }
            }

            if (invasionOnThisNight && !world.isDaytime()) {

                invasionActive = true;
                if (!activeBool) {
                    //System.out.println("triggering invasion start");
                    InvLog.dbg("attempting to start invasion for player: " + player.getName());
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

                boolean newTargetting = true;

                if (!newTargetting) {
                    if (player.onGround && world.getTotalWorldTime() % ConfigAdvancedOptions.aiTickRatePath == 0) {

                        int range = getTargettingRangeBuff(difficultyScale);
                        double moveSpeedAmp = 1D;

                        //TODO: instead of this expensive method and entity iteration, we could make distant targetting a targetTask!
                        List<EntityCreature> listEnts = world.getEntitiesWithinAABB(EntityCreature.class, new AxisAlignedBB(pos.posX, pos.posY, pos.posZ, pos.posX, pos.posY, pos.posZ).grow(range, range, range));

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

                                    if (ent.getNavigator().getPath() != null) {
                                        PathPoint finalPathPoint = ent.getNavigator().getPath().getFinalPathPoint();
                                        //if final path point is near player, thats good!
                                        if (finalPathPoint != null && player.getDistanceSq(finalPathPoint.x, finalPathPoint.y, finalPathPoint.z) < 1) {
                                            pathFindingDelay = ConfigAdvancedOptions.pathDelayBase;
                                        } else {
                                            pathFindingDelay += ConfigAdvancedOptions.pathFailDelayPenalty;
                                        }
                                    } else {
                                        pathFindingDelay += ConfigAdvancedOptions.pathFailDelayPenalty;
                                    }

                                    storage.dataCreatureLastPathWithDelay = world.getTotalWorldTime() + pathFindingDelay;
                                }

                            }
                        }
                    }
                } else {

                    if (world.getTotalWorldTime() % ConfigAdvancedOptions.aiTickRateEnhance == 0) {

                        /**TODO: re-evaluate use of this
                         * pretty sure i want anything spawned in to always chase at player
                         * in this case it also does this for same classes already spawned, maybe ok
                         */
                        //old way
                        int range = getTargettingRangeBuff(difficultyScale);
                        //int range = ConfigAdvancedOptions.aiOmniscienceRange;

                        List<EntityCreature> listEnts = world.getEntitiesWithinAABB(EntityCreature.class, new AxisAlignedBB(pos.posX, pos.posY, pos.posZ, pos.posX, pos.posY, pos.posZ).grow(range, range, range));

                        List<Class> listClassesSpawned = storage.getSpawnableClasses();

                        for (EntityCreature ent : listEnts) {

                            //TODO: put wave spawned entity list gathering here to compare against things we should enhance with omniscience
                            boolean shouldEnhanceEntity = listClassesSpawned.contains(ent.getClass());

                            if (shouldEnhanceEntity) {

                                //note, these arent being added in a way where its persistant, which is fine since this runs all the time anyways
                                //still needs a way to stop after invasion done

                                //targetting
                                if (!UtilEntityBuffs.hasTask(ent, EntityAINearestAttackablePlayerOmniscience.class, true)) {
                                    InvLog.dbg("trying to enhance with omniscience: " + ent.getName());
                                    UtilEntityBuffs.addTask(ent, EntityAINearestAttackablePlayerOmniscience.class, 10, true);

                                    //a bit of a temp patch, consider alternative due to maybe messing with non invasion enhanced stuff
                                    ent.getEntityData().setBoolean(TaskDigTowardsTarget.dataUseInvasionRules, true);
                                }

                                //long distance pathing
                                if (!UtilEntityBuffs.hasTask(ent, EntityAIChaseFromFar.class, false)) {
                                    UtilEntityBuffs.addTask(ent, EntityAIChaseFromFar.class, 4, false);
                                }
                            }
                        }
                    }
                }

                /**
                 * Buff with digging
                 */

                /*boolean enhanceAlreadyAlive = false;
                if (enhanceAlreadyAlive) {
                    if (world.getTotalWorldTime() % ConfigAdvancedOptions.aiTickRateEnhance == 0) {

                        int modifyRange = ConfigAdvancedOptions.aiEnhanceRange;
                        float chanceToEnhance = getDigChanceBuff(difficultyScale);
                        *//**
                         * TODO: consider making the digging tasks disable after invasions "ends"
                         * so that player wont get surprised later on in day if a zombie survives and takes a while to get to him
                         *//*
                        BehaviorModifier.enhanceZombiesToDig(world, new Vec3(player.posX, player.posY, player.posZ),
                                modifyRange, chanceToEnhance);
                    }
                }*/

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
            player.sendMessage(new TextComponentString(ChatFormatting.GREEN + "An invasion has started! But skipped for you!"));
        } else {
            player.sendMessage(new TextComponentString(ChatFormatting.RED + "An invasion has started! Be prepared!"));
        }

        //initNewInvasion(player, difficultyScale);

        InvLog.dbg("resetInvasion() for start");
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
            float finalCalc = buffBase * skipCount;
            InvLog.dbg("buffing invasion, inv count: " + skipCount + ", actual buff: " + finalCalc);
            DynamicDifficulty.setInvasionSkipBuff(player, finalCalc);
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
        player.sendMessage(new TextComponentString(ChatFormatting.GREEN + "The invasion has ended! Next invasion in " + ConfigInvasion.daysBetweenInvasions + " days!"));

        storage.dataPlayerInvasionActive = false;
        storage.dataPlayerInvasionWarned = false;
        InvLog.dbg("resetInvasion() for stop reset");
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

        Random rand = player.world.rand;

        InvasionEntitySpawn randomEntityList = storage.getRandomEntityClassToSpawn();

        if (randomEntityList != null) {
            for (int tries = 0; tries < 5; tries++) {
                int tryX = MathHelper.floor(player.posX) - (range / 2) + (rand.nextInt(range));
                int tryZ = MathHelper.floor(player.posZ) - (range / 2) + (rand.nextInt(range));
                int tryY = player.world.getHeight(new BlockPos(tryX, 0, tryZ)).getY();


                //TODO: make spawn check rules use entities own rules
                if (player.getDistance(tryX, tryY, tryZ) < minDist || player.getDistance(tryX, tryY, tryZ) > maxDist ||
                        !canSpawnMob(player.world, tryX, tryY, tryZ) || player.world.getLightFromNeighbors(new BlockPos(tryX, tryY, tryZ)) >= 6) {
                    //System.out.println("light: " + player.world.getLightFromNeighbors(new BlockCoord(tryX, tryY, tryZ)));
                    continue;
                }

                try {


                    String spawn = randomEntityList.spawnProfile.entities.get(rand.nextInt(randomEntityList.spawnProfile.entities.size()));
                    Class classToSpawn = CoroUtilEntity.getClassFromRegisty(spawn);
                    if (classToSpawn != null) {
                        EntityCreature ent = (EntityCreature) classToSpawn.getConstructor(new Class[]{World.class}).newInstance(new Object[]{player.world});

                        ent.setPosition(tryX, tryY, tryZ);
                        ent.onInitialSpawn(ent.world.getDifficultyForLocation(new BlockPos(ent)), (IEntityLivingData) null);
                        ent.getEntityData().setBoolean(UtilEntityBuffs.dataEntityWaveSpawned, true);

                        //TODO: here we need to apply the cmods chosen for it

                        //old way
                        //enhanceMobForDifficulty(ent, difficultyScale);

                        //set cmod data to entity
                        //JsonArray array = DeserializerAllJson.serializeCmods(randomEntityList.spawnProfile.cmods);
                        UtilEntityBuffs.registerAndApplyCmods(ent, randomEntityList.spawnProfile.cmods, difficultyScale);

                        //apply cmods from data
                        //UtilEntityBuffs.applyBuffSingularTry(UtilEntityBuffs.dataEntityBuffed_Inventory, ent, difficultyScale);

                        player.world.spawnEntity(ent);
                        ent.setAttackTarget(player);

                        randomEntityList.spawnCountCurrent++;

                        InvLog.dbg("Spawned " + randomEntityList.spawnCountCurrent + " mobs now: " + ent.getName());
                    } else {
                        InvLog.err("could not find registered class for entity name: " + spawn);
                    }

                    //String spawn = storage.getRandomEntityClassToSpawn();


                    //Class classToSpawn = spawnables.get(randSpawn);


                } catch (Exception e) {
                    InvLog.err("HW_Invasions: error spawning invasion entity: ");
                    e.printStackTrace();
                }


                /*EntityZombie entZ = new EntityZombie(player.world);
                entZ.setPosition(tryX, tryY, tryZ);
                entZ.onInitialSpawn(player.world.getDifficultyForLocation(new BlockCoord(entZ)), (IEntityLivingData)null);
                enhanceMobForDifficulty(entZ, difficultyScale);
                player.world.spawnEntityInWorld(entZ);

                entZ.setAttackTarget(player);*/

                //if (ZAConfig.debugConsoleSpawns) ZombieAwareness.dbg("spawnNewMobSurface: " + tryX + ", " + tryY + ", " + tryZ);
                //System.out.println("spawnNewMobSurface: " + tryX + ", " + tryY + ", " + tryZ);

                return true;
            }
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
        return MathHelper.clamp(((int) ((float)(max) * difficultyScale * scaleRate)), initialRange, max);
    }

    public static float getDigChanceBuff(float difficultyScale) {
        float initial = (float) ConfigInvasion.invasion_DiggerConvertChance_Min;
        float max = (float) ConfigInvasion.invasion_DiggerConvertChance_Max;
        float scaleRate = (float) ConfigInvasion.invasion_DiggerConvertChance_ScaleRate;
        return MathHelper.clamp((((float)(max) * difficultyScale * scaleRate)), initial, max);
    }

	/*public String getInvasionDebug(float difficultyScale) {
		return "spawncount: " + getSpawnCountBuff(difficultyScale) +
				" | targetrange: " + getTargettingRangeBuff(difficultyScale) +
				" | dig chance: " + getDigChanceBuff(difficultyScale) +
				" | inventory stage: " + getInventoryStageBuff(difficultyScale) +
				" | scale: " + difficultyScale;
	}*/

    public static DataMobSpawnsTemplate chooseInvasionProfile(EntityPlayer player, float difficulty) {
        List<DataMobSpawnsTemplate> listPhase2 = new ArrayList<>();

        InvLog.dbg("choosing invasion profile for player: " + player.getName() + ", difficulty: " + difficulty);

        //System.out.println("phase 1 choice count: " + DifficultyDataReader.getData().listMobSpawnTemplates.size());
        for (DataMobSpawnsTemplate spawns : DifficultyDataReader.getData().listMobSpawnTemplates) {

            boolean fail = false;
            InvLog.dbg("evaluating conditions for spawn template: " + spawns.toString());
            for (DataCondition condition : DeserializerAllJson.getConditionsFlattened(spawns.conditions)) {

                if (!(condition instanceof ConditionRandom)) {

                    //TODO: toString() for conditions to output min/max difficulty etc

                    if (!evaluateCondition(player, condition, difficulty)) {
                        InvLog.dbg("evaluating failed for condition: " + condition + ", dbg: " + condition.toString());
                        fail = true;
                        break;
                    } else {
                        InvLog.dbg("evaluating passed for condition: " + condition + ", dbg: " + condition.toString());
                    }
                }
            }

            if (!fail) {
                InvLog.dbg("adding spawn template: " + spawns.name);
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
            for (DataCondition condition : DeserializerAllJson.getConditionsFlattened(spawns.conditions)) {

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

        int index = -1;
        Random rand = new Random();

        if (totalWeight > 0) {
            int randVal = rand.nextInt(totalWeight);

            for (int i = 0; i < listWeights.size(); i++) {
                if (randVal < listWeights.get(i)) {
                    index = i;
                    break;
                }
                randVal -= listWeights.get(i);
            }
        }

        if (index != -1) {
            InvLog.dbg("chooseInvasionProfile() for: " + player.getName() + ", " + listPhase2.get(index).toString());
            return listPhase2.get(index);

            //System.out.println("final choice: " + spawn.name);
        } else {
            //System.out.println("design flaw!");
        }

        //temp
        InvLog.dbg("chooseInvasionProfile() returned no invasion profile for: " + player.getName());
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
