package com.corosus.inv;

import CoroUtil.ai.tasks.EntityAIChaseFromFar;
import CoroUtil.ai.tasks.EntityAINearestAttackablePlayerOmniscience;
import CoroUtil.ai.tasks.TaskDigTowardsTarget;
import CoroUtil.difficulty.DifficultyQueryContext;
import CoroUtil.difficulty.DynamicDifficulty;
import CoroUtil.difficulty.UtilEntityBuffs;
import CoroUtil.difficulty.data.DataCondition;
import CoroUtil.difficulty.data.DeserializerAllJson;
import CoroUtil.difficulty.data.DifficultyDataReader;
import CoroUtil.difficulty.data.conditions.*;
import CoroUtil.difficulty.data.spawns.DataMobSpawnsTemplate;
import CoroUtil.forge.CULog;
import CoroUtil.util.BlockCoord;
import CoroUtil.util.CoroUtilEntity;
import CoroUtil.util.CoroUtilPath;
import CoroUtil.util.CoroUtilWorldTime;
import com.corosus.inv.capabilities.PlayerDataInstance;
import com.corosus.inv.config.ConfigAdvancedOptions;
import com.corosus.inv.config.ConfigInvasion;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Corosus on 3/8/2017.
 */
public class InvasionManager {

    public static boolean invasionOnThisNight_Last = false;
    public static boolean isDayLast = false;

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
                if (!CoroUtilWorldTime.isNightPadded(world)) {
                    int skipCount = player.getEntityData().getInteger(DynamicDifficulty.dataPlayerInvasionSkipCount);
                    if (skipCount < ConfigInvasion.maxConsecutiveInvasionSkips) {
                        skipCount++;
                        player.getEntityData().setBoolean(DynamicDifficulty.dataPlayerInvasionSkipping, true);
                        player.getEntityData().setInteger(DynamicDifficulty.dataPlayerInvasionSkipCount, skipCount);
                        player.sendMessage(new TextComponentString(String.format(ConfigInvasion.Invasion_Message_skipping, skipCount)));
                        return true;
                    } else {
                        player.sendMessage(new TextComponentString(String.format(ConfigInvasion.Invasion_Message_skippedTooMany, ConfigInvasion.maxConsecutiveInvasionSkips)));
                    }

                } else {
                    player.sendMessage(new TextComponentString(ConfigInvasion.Invasion_Message_tooLate));
                }
            } else {
                player.sendMessage(new TextComponentString(ConfigInvasion.Invasion_Message_notInvasionNight));
            }
        } else {
            player.sendMessage(new TextComponentString(ConfigInvasion.Invasion_Message_alreadySkipping));
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
            long dayNumber = (world.getWorldTime() / CoroUtilWorldTime.getDayLength()) + 1;
            //System.out.println("daynumber: " + dayNumber + " - " + world.getWorldTime() + " - " + world.isDaytime());

            boolean invasionActive = false;

            //debug
            //difficultyScale = 1F;

            boolean activeBool = storage.dataPlayerInvasionActive;
            boolean skippingBool = player.getEntityData().getBoolean(DynamicDifficulty.dataPlayerInvasionSkipping);

            //track state of invasion for proper init and reset for wave counts, etc
            //new day starts just as sun is rising, so invasion stops just at the right time when sun is imminent, they burn 300 ticks before invasion ends, thats ok
            //FYI night val is based on sunlight level, so its not night ends @ 24000 cycle, its a bit before, 400ish ticks before, thats ok
            boolean invasionOnThisNight = isInvasionTonight(world);

            if (invasionOnThisNight != invasionOnThisNight_Last) {
                InvLog.dbg("invasionOnThisNight: " + invasionOnThisNight);
                invasionOnThisNight_Last = invasionOnThisNight;
            }

            boolean isDay = !CoroUtilWorldTime.isNightPadded(world);

            if (isDay != isDayLast) {
                InvLog.dbg("world.isDaytime(): " + isDay + ", time: " + world.getWorldTime() + ", timemod: " + world.getWorldTime() % CoroUtilWorldTime.getDayLength());
                isDayLast = isDay;
            }

            if (invasionOnThisNight && isDay) {
                if (!storage.dataPlayerInvasionWarned && !storage.dataPlayerInvasionHappenedThisDay) {
                    player.sendMessage(new TextComponentString(ConfigInvasion.Invasion_Message_startsTonight));
                    storage.dataPlayerInvasionWarned = true;
                }
            }

            if (!invasionOnThisNight) {
                storage.dataPlayerInvasionHappenedThisDay = false;
            }

            if (invasionOnThisNight && !isDay) {

                storage.dataPlayerInvasionHappenedThisDay = true;

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

            //now that warn flag is serialized, we need to reset it incase time changes during warning stage
            if (!invasionOnThisNight) {
                storage.dataPlayerInvasionWarned = false;
                player.getEntityData().setBoolean(DynamicDifficulty.dataPlayerInvasionSkipping, false);
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
                        if (ConfigAdvancedOptions.enhanceAllMobsOfSpawnedTypesForOmniscience) {
                            //old way
                            //int range = getTargettingRangeBuff(difficultyScale);
                            int range = ConfigAdvancedOptions.aiOmniscienceRange;

                            List<EntityCreature> listEnts = world.getEntitiesWithinAABB(EntityCreature.class, new AxisAlignedBB(pos.posX, pos.posY, pos.posZ, pos.posX, pos.posY, pos.posZ).grow(range, range, range));

                            List<Class> listClassesSpawned = storage.getSpawnableClasses();

                            for (EntityCreature ent : listEnts) {

                                boolean shouldEnhanceEntity = listClassesSpawned.contains(ent.getClass());

                                if (shouldEnhanceEntity) {

                                    //note, these arent being added in a way where its persistant, which is fine since this runs all the time anyways
                                    //still needs a way to stop after invasion done

                                    //targetting
                                    if (!UtilEntityBuffs.hasTask(ent, EntityAINearestAttackablePlayerOmniscience.class, true)) {
                                        InvLog.dbg("trying to enhance with omniscience via pre-existing mob way: " + ent.getName());
                                        UtilEntityBuffs.addTask(ent, EntityAINearestAttackablePlayerOmniscience.class, 10, true);
                                    }

                                    //long distance pathing
                                    if (!UtilEntityBuffs.hasTask(ent, EntityAIChaseFromFar.class, false)) {
                                        UtilEntityBuffs.addTask(ent, EntityAIChaseFromFar.class, 4, false);
                                    }
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
            player.sendMessage(new TextComponentString(ConfigInvasion.Invasion_Message_startedButSkippedForYou));
        } else {
            player.sendMessage(new TextComponentString(ConfigInvasion.Invasion_Message_started));
        }

        //initNewInvasion(player, difficultyScale);

        InvLog.dbg("resetInvasion() for start");
        storage.resetInvasion();
        storage.dataPlayerInvasionActive = true;
        storage.setDifficultyForInvasion(difficultyScale);

        DataMobSpawnsTemplate profile = chooseInvasionProfile(player, new DifficultyQueryContext(ConditionContext.TYPE_INVASION, InvasionManager.getInvasionNumber(player.world), difficultyScale));
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

    public static DataMobSpawnsTemplate getInvasionTestData(EntityPlayer player, DifficultyQueryContext context) {
        PlayerDataInstance storage = player.getCapability(Invasion.PLAYER_DATA_INSTANCE, null);

        DataMobSpawnsTemplate profile = chooseInvasionProfile(player, context);
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
        player.sendMessage(new TextComponentString(String.format(ConfigInvasion.Invasion_Message_ended, ConfigInvasion.invadeEveryXDays)));

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

                //set position in the solid ground
                //previously it was targetting above ground but not excluding things like tallgrass
                //eg: this fixes mobs not spawning in desert where theres no tallgrass
                tryY--;

                //dont factor in y for high elevation change based spawns, otherwise things wont spawn in certain conditions, eg base on high hill
                double dist = player.getDistance(tryX, player.posY, tryZ);

                //TODO: make spawn check rules use entities own rules
                /*if (dist < minDist || dist > maxDist ||
                        !canSpawnMob(player.world, tryX, tryY, tryZ) || player.world.getLightFromNeighbors(new BlockPos(tryX, tryY, tryZ)) >= 6) {
                    continue;
                }*/

                if (dist < minDist || dist > maxDist) {
                    CULog.dbg("spawnNewMobFromProfile: dist fail, minDist: " + minDist + ", maxDist: " + maxDist + ", tryDist: " + dist);
                    continue;
                }

                if (!CoroUtilEntity.canSpawnMobOnGround(player.world, tryX, tryY, tryZ)) {
                    CULog.dbg("spawnNewMobFromProfile: canSpawnMobOnGround fail");
                    continue;
                }

                if (player.world.getLightFromNeighbors(new BlockPos(tryX, tryY, tryZ)) >= 6) {
                    CULog.dbg("spawnNewMobFromProfile: getLightFromNeighbors fail");
                    continue;
                }

                try {


                    String spawn = randomEntityList.spawnProfile.entities.get(rand.nextInt(randomEntityList.spawnProfile.entities.size()));
                    Class classToSpawn = CoroUtilEntity.getClassFromRegistry(spawn);
                    if (classToSpawn != null) {
                        EntityCreature ent = (EntityCreature) classToSpawn.getConstructor(new Class[]{World.class}).newInstance(new Object[]{player.world});

                        //set to above the solid block we can spawn on
                        ent.setPosition(tryX, tryY + 1, tryZ);
                        ent.onInitialSpawn(ent.world.getDifficultyForLocation(new BlockPos(ent)), (IEntityLivingData) null);
                        ent.getEntityData().setBoolean(UtilEntityBuffs.dataEntityWaveSpawned, true);
                        ent.getEntityData().setBoolean(TaskDigTowardsTarget.dataUseInvasionRules, true);

                        //TODO: here we need to apply the cmods chosen for it

                        //old way
                        //enhanceMobForDifficulty(ent, difficultyScale);

                        //set cmod data to entity
                        //JsonArray array = DeserializerAllJson.serializeCmods(randomEntityList.spawnProfile.cmods);
                        UtilEntityBuffs.registerAndApplyCmods(ent, randomEntityList.spawnProfile.cmods, difficultyScale);

                        //apply cmods from data
                        //UtilEntityBuffs.applyBuffSingularTry(UtilEntityBuffs.dataEntityBuffed_Inventory, ent, difficultyScale);

                        ent.getEntityData().setBoolean(UtilEntityBuffs.dataEntityInitialSpawn, true);
                        player.world.spawnEntity(ent);
                        ent.getEntityData().setBoolean(UtilEntityBuffs.dataEntityInitialSpawn, false);
                        //leave this to omniscience task if config says so
                        //ent.setAttackTarget(player);

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

    public static boolean isInvasionTonight(World world) {

        int dayAdjust = ConfigInvasion.firstInvasionNight;
        int dayNumber = (int)(world.getWorldTime() / CoroUtilWorldTime.getDayLength()) + 1;
        int dayStart = (dayNumber-dayAdjust);

        if (dayStart < 0) {
            return false;
        }

        return (float)dayStart % (float)ConfigInvasion.invadeEveryXDays == 0;

        /*return dayNumber >= dayAdjust &&
                (dayNumber-dayAdjust == 0 ||
                        (dayNumber-dayAdjust) % Math.max(1, ConfigInvasion.invadeEveryXDays + 1) == 0);*/
    }

    /**
     * get staticly calculated wave number based on time and config, not per player
     *
     * @param world
     * @return
     */
    public static int getInvasionNumber(World world) {

        //lets not use 0 indexed
        int dayAdjust = ConfigInvasion.firstInvasionNight;
        int dayNumber = (int)(world.getWorldTime() / CoroUtilWorldTime.getDayLength()) + 1;
        int dayStart = (dayNumber-dayAdjust);

        if (dayStart < 0) {
            return 1;
        }

        //float test = (float)dayStart / (float)ConfigInvasion.invadeEveryXDays;
        //System.out.println("inv num2: " + test);

        int invasionNumber = dayStart / ConfigInvasion.invadeEveryXDays;

        return invasionNumber + 1;
    }

    /*public static int getSpawnCountBuff(float difficultyScale) {
        int initialSpawns = ConfigInvasion.invasion_Spawns_Min;
        int maxSpawnsAllowed = ConfigInvasion.invasion_Spawns_Max;
        float scaleRate = (float) ConfigInvasion.invasion_Spawns_ScaleRate;
        return MathHelper.clamp_int(((int) ((float)(maxSpawnsAllowed) * difficultyScale * scaleRate)), initialSpawns, maxSpawnsAllowed);
    }*/

    public static int getTargettingRangeBuff(float difficultyScale) {
        int initialRange = ConfigInvasion.Invasion_TargettingRange_Min;
        int max = ConfigInvasion.Invasion_TargettingRange_Max;
        float scaleRate = (float) ConfigInvasion.Invasion_TargettingRange_ScaleRate;
        return MathHelper.clamp(((int) ((float)(max) * difficultyScale * scaleRate)), initialRange, max);
    }

    public static float getDigChanceBuff(float difficultyScale) {
        float initial = (float) ConfigInvasion.Invasion_DiggerConvertChance_Min;
        float max = (float) ConfigInvasion.Invasion_DiggerConvertChance_Max;
        float scaleRate = (float) ConfigInvasion.Invasion_DiggerConvertChance_ScaleRate;
        return MathHelper.clamp((((float)(max) * difficultyScale * scaleRate)), initial, max);
    }

	/*public String getInvasionDebug(float difficultyScale) {
		return "spawncount: " + getSpawnCountBuff(difficultyScale) +
				" | targetrange: " + getTargettingRangeBuff(difficultyScale) +
				" | dig chance: " + getDigChanceBuff(difficultyScale) +
				" | inventory stage: " + getInventoryStageBuff(difficultyScale) +
				" | scale: " + difficultyScale;
	}*/

    public static DataMobSpawnsTemplate chooseInvasionProfile(EntityPlayer player, DifficultyQueryContext context) {

        List<DataMobSpawnsTemplate> listPhase2 = new ArrayList<>();

        InvLog.dbg("choosing invasion profile for player: " + player.getName() + ", difficulty: " + context.getDifficulty());

        //System.out.println("phase 1 choice count: " + DifficultyDataReader.getData().listMobSpawnTemplates.size());
        for (DataMobSpawnsTemplate spawns : DifficultyDataReader.getData().listMobSpawnTemplates) {

            boolean fail = false;
            InvLog.dbg("evaluating conditions for spawn template: " + spawns.toString());
            for (DataCondition condition : DeserializerAllJson.getConditionsFlattened(spawns.conditions)) {

                if (!(condition instanceof ConditionRandom)) {

                    //TODO: toString() for conditions to output min/max difficulty etc

                    if (!evaluateCondition(player, condition, context)) {
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



    public static boolean evaluateCondition(EntityPlayer player, DataCondition condition, DifficultyQueryContext context) {
        if (condition instanceof ConditionContext) {
            return context.getContext().equals(((ConditionContext) condition).type);
        } else if (condition instanceof ConditionDifficulty) {
            return context.getDifficulty() >= ((ConditionDifficulty)condition).min &&
                    context.getDifficulty() <= ((ConditionDifficulty)condition).max;
        } else if (condition instanceof ConditionInvasionNumber) {
            /**
             * TODO: global or per player tracked? also i dont think invasion number is even set yet
             * also what about skipped invasions?
             * could just stick with global and do the math to figure out what inv number it is
             * - doing this for now
             */
            return context.getInvasionNumber() >= ((ConditionInvasionNumber)condition).min &&
                    context.getInvasionNumber() <= ((ConditionInvasionNumber)condition).max;
        } else if (condition instanceof ConditionInvasionRate) {
            return context.getInvasionNumber() % ((ConditionInvasionRate) condition).rate == 0;
        } else if (condition instanceof ConditionModLoaded) {
            if (((ConditionModLoaded) condition).isInverted()) {
                return !Loader.isModLoaded(((ConditionModLoaded) condition).mod_id);
            } else {
                return Loader.isModLoaded(((ConditionModLoaded) condition).mod_id);
            }
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
