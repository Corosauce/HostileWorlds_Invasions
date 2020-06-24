package com.corosus.inv;

import CoroUtil.ai.tasks.EntityAIChaseFromFar;
import CoroUtil.ai.tasks.EntityAINearestAttackablePlayerOmniscience;
import CoroUtil.ai.tasks.TaskDigTowardsTarget;
import CoroUtil.config.ConfigCoroUtil;
import CoroUtil.difficulty.DifficultyQueryContext;
import CoroUtil.difficulty.DynamicDifficulty;
import CoroUtil.difficulty.UtilEntityBuffs;
import CoroUtil.difficulty.data.DataCondition;
import CoroUtil.difficulty.data.DeserializerAllJson;
import CoroUtil.difficulty.data.DifficultyDataReader;
import CoroUtil.difficulty.data.conditions.*;
import CoroUtil.difficulty.data.spawns.DataMobSpawnsTemplate;
import CoroUtil.forge.CULog;
import CoroUtil.util.*;
import com.corosus.inv.capabilities.PlayerDataInstance;
import com.corosus.inv.config.ConfigAdvancedOptions;
import com.corosus.inv.config.ConfigInvasion;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandException;
import net.minecraft.entity.*;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketChangeGameState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
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
     * ---- Might need a ist / blacklist of mod machines
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

    public static boolean canPlayerSkipInvasion(EntityPlayer player) {
        World world = player.world;
        boolean skipped = player.getEntityData().getBoolean(DynamicDifficulty.dataPlayerInvasionSkipping);
        if (!skipped) {
            if (isInvasionTonight(world)) {
                //only allow skip during day before its actually active
                if (!CoroUtilWorldTime.isNightPadded(world)) {
                    int skipCount = player.getEntityData().getInteger(DynamicDifficulty.dataPlayerInvasionSkipCount);
                    if (ConfigInvasion.maxConsecutiveInvasionSkips == -1 || skipCount < ConfigInvasion.maxConsecutiveInvasionSkips) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

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
                    if (ConfigInvasion.maxConsecutiveInvasionSkips == -1 || skipCount < ConfigInvasion.maxConsecutiveInvasionSkips) {
                        skipCount++;
                        player.getEntityData().setBoolean(DynamicDifficulty.dataPlayerInvasionSkipping, true);
                        player.getEntityData().setInteger(DynamicDifficulty.dataPlayerInvasionSkipCount, skipCount);

                        int skipCountAllTime = player.getEntityData().getInteger(DynamicDifficulty.dataPlayerInvasionSkipCountForMultiplier) + 1;
                        player.getEntityData().setInteger(DynamicDifficulty.dataPlayerInvasionSkipCountForMultiplier, skipCountAllTime);

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
     * Ticked every tick
     *
     * @param player
     */
    public static void tickPlayer(EntityPlayer player) {

        //morpheus workaround
        if (ConfigInvasion.preventSleepDuringInvasions) {
            if (InvasionManager.shouldLockOutFeaturesForPossibleActiveInvasion(player.world)) {
                if (player.isPlayerSleeping()) {
                    player.wakeUpPlayer(true, true, false);
                    player.sendMessage(new TextComponentString(ConfigInvasion.Invasion_Message_cantSleep));
                }
            }
        }
    }

    /**
     * Ticked every tick regardless of dimension, fired from living event tick
     *
     * @param player
     */
    public static void tickPlayerEverywhere(EntityPlayer player) {
        if (ConfigInvasion.forcePlayersToOverworldDuringInvasion) {
            if (CoroUtilWorldTime.isNightPadded(player.world) && InvasionManager.isInvasionTonight(player.world)) {
                //ignore skipping players
                if (!InvasionManager.isPlayerSkippingInvasion(player)) {
                    PlayerDataInstance storage = player.getCapability(Invasion.PLAYER_DATA_INSTANCE, null);

                    if (player.dimension == 0) {
                        storage.ticksNotInOverworld = 0;
                    } else {
                        if (ConfigInvasion.forcePlayersToOverworldDuringInvasion_TickDelay > 0) {
                            if (storage.ticksNotInOverworld == 0 && !ConfigInvasion.forcePlayersToOverworldDuringInvasion_FirstWarningMessage.equals("")) {
                                //first warning message if theres time for them to react
                                player.sendMessage(new TextComponentString(
                                        String.format(ConfigInvasion.forcePlayersToOverworldDuringInvasion_FirstWarningMessage,
                                                (int) (ConfigInvasion.forcePlayersToOverworldDuringInvasion_TickDelay / 20))
                                ));
                            }

                            if (ConfigInvasion.forcePlayersToOverworldDuringInvasion_TickDelay - storage.ticksNotInOverworld == 200 &&
                                    !ConfigInvasion.forcePlayersToOverworldDuringInvasion_FinalWarningMessage.equals("")) {
                                player.sendMessage(new TextComponentString(ConfigInvasion.forcePlayersToOverworldDuringInvasion_FinalWarningMessage));
                            }
                        }
                        CULog.dbg("time: " + player.world.getTotalWorldTime() + " - " +
                                storage.ticksNotInOverworld);
                        storage.ticksNotInOverworld++;

                        if (storage.ticksNotInOverworld >= ConfigInvasion.forcePlayersToOverworldDuringInvasion_TickDelay) {
                            if (player instanceof EntityPlayerMP) {
                                player = (EntityPlayer) player.changeDimension(0);

                                if (player != null) {
                                    BlockPos spawnPos = player.getBedLocation(0);
                                    if (spawnPos != null) {
                                        spawnPos = EntityPlayer.getBedSpawnLocation(
                                                ((EntityPlayerMP) player).world, spawnPos, false);
                                        if (spawnPos == null) {
                                            ((EntityPlayerMP) player).connection.sendPacket(new SPacketChangeGameState(0, 0.0F));
                                        }
                                    }
                                    if (spawnPos == null) {
                                        CULog.dbg("force tp, no bed location found, reverting to world spawn");
                                        spawnPos = player.world.provider.getRandomizedSpawnPoint();
                                    }
                                    CULog.dbg("spawnpoint to use: " + spawnPos);
                                    ((EntityPlayerMP) player).connection.setPlayerLocation(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), player.rotationYaw, player.rotationPitch);
                                    if (!ConfigInvasion.forcePlayersToOverworldDuringInvasion_TPMessage.equals("")) {
                                        player.sendMessage(new TextComponentString(ConfigInvasion.forcePlayersToOverworldDuringInvasion_TPMessage));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     *
     * Ticked every 20 ticks
     *
     * Need to do some stuff before main player loop runs
     *
     * @param player
     */
    public static void tickPlayerSlowPre(EntityPlayer player) {
        if (ConfigInvasion.invasionCountingPerPlayer) {
            long ticksPlayed = player.getEntityData().getLong(DynamicDifficulty.dataPlayerServerTicks);
            //doing +1 for case of player time = server time, since first invasion technically happens before 3 day count (72000 ticks)
            //isInvasionTonight does same thing of +1
            int dayNum = (int) (ticksPlayed / CoroUtilWorldTime.getDayLength()) + 1;
            //CULog.dbg("per player day num: " + dayNum);
            if (dayNum < ConfigInvasion.firstInvasionNight) {
                //CULog.dbg("too soon for specific player: " + player.getDisplayNameString() + ", skipping invasion");
                player.getEntityData().setBoolean(DynamicDifficulty.dataPlayerInvasionSkippingTooSoon, true);
            }
        }
    }

    /**
     *
     *
     * @param player
     */
    public static void tickPlayerSlow(EntityPlayer player) {
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
            boolean skippingBool = isPlayerSkippingInvasion(player);

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
                    if (player.getEntityData().getBoolean(DynamicDifficulty.dataPlayerInvasionSkippingTooSoon)) {
                        //if (world.playerEntities.size() > 1) {
                        if (isAnyoneBeingInvadedTonight(player.world)) {
                            //CULog.dbg("someone being invaded");
                            //if others on server
                            if (!ConfigInvasion.Invasion_Message_startsTonightButNotYou.equals("")) {
                                player.sendMessage(new TextComponentString(String.format(ConfigInvasion.Invasion_Message_startsTonightButNotYou, ConfigInvasion.firstInvasionNight)));
                            }
                        } else {
                            //CULog.dbg("noone being invaded");
                        }
                    } else {
                        if (!ConfigInvasion.Invasion_Message_startsTonight.equals("")) {
                            player.sendMessage(new TextComponentString(ConfigInvasion.Invasion_Message_startsTonight));
                        }
                    }
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
                    if (!skippingBool) {
                        //System.out.println("triggering invasion start");
                        InvLog.dbg("attempting to start invasion for player: " + player.getName());
                        invasionStart(player, difficultyScale);
                    } else {
                        //from invasionStart() to keep state correct for other things since initial design didnt account for skipping players too well
                        storage.dataPlayerInvasionActive = true;

                        if (player.getEntityData().getBoolean(DynamicDifficulty.dataPlayerInvasionSkipping)) {
                            if (!ConfigInvasion.Invasion_Message_startedButSkippedForYou.equals("")) {
                                player.sendMessage(new TextComponentString(ConfigInvasion.Invasion_Message_startedButSkippedForYou));
                            }
                        } else if (player.getEntityData().getBoolean(DynamicDifficulty.dataPlayerInvasionSkippingTooSoon)) {
                            if (isAnyoneBeingInvadedTonight(player.world)) {
                                //CULog.dbg("someone being invaded");
                                if (!ConfigInvasion.Invasion_Message_startedButSkippedForYouTooSoon.equals("")) {
                                    player.sendMessage(new TextComponentString(String.format(ConfigInvasion.Invasion_Message_startedButSkippedForYouTooSoon, ConfigInvasion.firstInvasionNight)));
                                }
                            } else {
                                //CULog.dbg("noone being invaded");
                            }
                        }
                    }
                }
            } else {
                invasionActive = false;
                if (activeBool) {
                    //System.out.println("triggering invasion stop");

                    //before the skipping flag is reset for all, check if wasnt skipping, and reset their skip counter
                    //might be a better place to put this
                    if (!skippingBool) {
                        player.getEntityData().setInteger(DynamicDifficulty.dataPlayerInvasionSkipCount, 0);
                        if (ConfigInvasion.Sacrifice_CountNeeded_Multiplier_ResetOnInvasionNoSkip) {
                            player.getEntityData().setInteger(DynamicDifficulty.dataPlayerInvasionSkipCountForMultiplier, 0);
                        }
                    }

                    invasionStopReset(player);
                }
            }

            //now that warn flag is serialized, we need to reset it incase time changes during warning stage
            if (!invasionOnThisNight) {
                storage.dataPlayerInvasionWarned = false;

                //also done in invasionStopReset, might be redundant but cant be sure for all cases, logic is fickle
                player.getEntityData().setBoolean(DynamicDifficulty.dataPlayerInvasionSkipping, false);
                player.getEntityData().setBoolean(DynamicDifficulty.dataPlayerInvasionSkippingTooSoon, false);
            }

            //int playerRating = UtilPlayer.getPlayerRating(player);

            //System.out.println("invasion?: " + invasionActive + " - day# " + dayNumber + " - time: " + world.getWorldTime() + " - invasion tonight: " + invasionOnThisNight);
            //System.out.println("inv info: " + getInvasionDebug(difficultyScale));
            //System.out.println("player rating: " + playerRating);

            //debug
            //invasionActive = true;
            //world.getDifficultyForLocation(player.playerLocation);

            if (invasionActive && !skippingBool) {

                if (world.getTotalWorldTime() % ConfigAdvancedOptions.aiTickRateEnhance == 0) {
                    if (ConfigAdvancedOptions.enhanceAllMobsOfSpawnedTypesForOmniscience) {

                        boolean debugTPSSpike = false;
                        if (debugTPSSpike) {
                            player.sendMessage(new TextComponentString("enhancing extra mobs with omniscience"));
                            CULog.dbg("enhancing extra mobs with omniscience");
                        }

                        //old way
                        //int range = getTargettingRangeBuff(difficultyScale);
                        int range = ConfigAdvancedOptions.aiOmniscienceRange;

                        List<EntityCreature> listEnts = world.getEntitiesWithinAABB(EntityCreature.class, new AxisAlignedBB(pos.posX, pos.posY, pos.posZ, pos.posX, pos.posY, pos.posZ).grow(range, range, range));

                        //enhances only mobs of that _type_ that have been invading

                        List<Class> listClassesSpawned = storage.getSpawnableClasses();

                        for (EntityCreature ent : listEnts) {

                            boolean shouldEnhanceEntity = listClassesSpawned.contains(ent.getClass());

                            //no point in giving a cow only omniscience
                            boolean hostileMobsOnly = true;

                            if (shouldEnhanceEntity && (!hostileMobsOnly || ent instanceof EntityMob)) {

                                //note, these arent being added in a way where its persistant, which is fine since this runs all the time anyways
                                //still needs a way to stop after invasion done

                                //this should flag the entity so tasks will get removed later
                                ent.getEntityData().setBoolean(UtilEntityBuffs.dataEntityBuffed, true);
                                ent.getEntityData().setBoolean(UtilEntityBuffs.dataEntityBuffed_AI_Omniscience, true);

                                //stagger the first pathfind delay
                                ent.getEntityData().setLong(UtilEntityBuffs.dataEntityBuffed_LastTimePathfindLongDist, ent.world.getTotalWorldTime() + (ent.getEntityId() % 20));

                                //targetting
                                if (!UtilEntityBuffs.hasTask(ent, EntityAINearestAttackablePlayerOmniscience.class, true)) {
                                    //InvLog.dbg("trying to enhance with omniscience via pre-existing mob way: " + ent.getName());
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

                /**
                 * Spawn extra with buffs
                 */

                if (world.getTotalWorldTime() % ConfigAdvancedOptions.aiTickRateSpawning == 0) {

                    spawnNewMobFromProfile(player, difficultyScale);

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void tickSpawning(EntityPlayer player, float difficulty) {

    }

    public static void invasionStart(EntityPlayer player, float difficultyScale) {
        PlayerDataInstance storage = player.getCapability(Invasion.PLAYER_DATA_INSTANCE, null);

        //initNewInvasion(player, difficultyScale);

        InvLog.dbg("resetInvasion() for start");
        storage.resetInvasion();
        storage.dataPlayerInvasionActive = true;
        storage.setDifficultyForInvasion(difficultyScale);
        storage.lastWaveNumber++;

        int waveNumberToUse;
        if (ConfigInvasion.invasionCountingPerPlayer) {
            waveNumberToUse = storage.lastWaveNumber;
        } else {
            waveNumberToUse = InvasionManager.getInvasionNumber(player.world);
        }

        DataMobSpawnsTemplate profile = chooseInvasionProfile(player, new DifficultyQueryContext(ConditionContext.TYPE_INVASION, waveNumberToUse, difficultyScale));
        if (profile != null) {
            storage.initNewInvasion(profile);
        } else {
            //TODO: no invasions for you! also this is bad?, perhaps hardcode a fallback default, what if no invasion is modpack makers intent
        }


        //System.out.println("invasion started");


        if (profile != null && !profile.wave_message.equals("<NULL>")) {
            //support for no message override for wave, might as well just check if its blank and prevent code from running
            if (!profile.wave_message.equals("")) {
                player.sendMessage(new TextComponentString(profile.wave_message));
            }
        } else {
            player.sendMessage(new TextComponentString(ConfigInvasion.Invasion_Message_started));
        }

        //add buff for player based on how many invasions they skipped (and only if this isnt a skipped invasion)
        //if (!player.getEntityData().getBoolean(DynamicDifficulty.dataPlayerInvasionSkipping)) {
            float buffBase = 0.5F;
            float skipCount = player.getEntityData().getInteger(DynamicDifficulty.dataPlayerInvasionSkipCount);
            float finalCalc = buffBase * skipCount;
            InvLog.dbg("buffing invasion, inv count: " + skipCount + ", actual buff: " + finalCalc);
            DynamicDifficulty.setInvasionSkipBuff(player, finalCalc);
        //}
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
        if (!isPlayerSkippingInvasion(player)) {
            if (!ConfigInvasion.Invasion_Message_ended.equals("")) {
                player.sendMessage(new TextComponentString(String.format(ConfigInvasion.Invasion_Message_ended, ConfigInvasion.invadeEveryXDays)));
            }
        }

        storage.dataPlayerInvasionActive = false;
        storage.dataPlayerInvasionWarned = false;
        InvLog.dbg("resetInvasion() for stop reset");
        storage.resetInvasion();

        player.getEntityData().setBoolean(DynamicDifficulty.dataPlayerInvasionSkipping, false);
        player.getEntityData().setBoolean(DynamicDifficulty.dataPlayerInvasionSkippingTooSoon, false);

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

        int tryCachedCaveSpotThreshold = 300;
        int tryWaterSurfaceAreasThreshold = 300;

        if (randomEntityList != null) {
            for (int tries = 0; tries < ConfigAdvancedOptions.attemptsPerSpawn; tries++) {
                storage.triesSinceWorkingAnySpawn++;
                int tryX = MathHelper.floor(player.posX) - (range / 2) + (rand.nextInt(range));
                int tryZ = MathHelper.floor(player.posZ) - (range / 2) + (rand.nextInt(range));
                int tryY = MathHelper.floor(player.posY) - (range / 2) + (rand.nextInt(range));
                int surfaceY = player.world.getHeight(new BlockPos(tryX, 0, tryZ)).getY();

                //set position in the solid ground
                //previously it was targetting above ground but not excluding things like tallgrass
                //eg: this fixes mobs not spawning in desert where theres no tallgrass
                surfaceY--;

                //dont factor in y for high elevation change based spawns, otherwise things wont spawn in certain conditions, eg base on high hill
                double distXZ = player.getDistance(tryX, player.posY, tryZ);

                //TODO: make spawn check rules use entities own rules
                /*if (dist < minDist || dist > maxDist ||
                        !canSpawnMob(player.world, tryX, tryY, tryZ) || player.world.getLightFromNeighbors(new BlockPos(tryX, tryY, tryZ)) >= 6) {
                    continue;
                }*/



                if (distXZ < minDist || distXZ > maxDist) {
                    //CULog.dbg("spawnNewMobFromProfile: dist fail, minDist: " + minDist + ", maxDist: " + maxDist + ", tryDist: " + distXZ);
                    continue;
                }

                boolean requireSolidGround = true;
                boolean allowWaterSurfaceSpawn = false;
                if (randomEntityList.spawnProfile.spawnType == EnumSpawnPlacementType.WATER || randomEntityList.spawnProfile.spawnType == EnumSpawnPlacementType.AIR) {
                    requireSolidGround = false;
                }

                int yToUse = surfaceY;

                boolean caveSpawn = false;

                if (randomEntityList.spawnProfile.spawnType == EnumSpawnPlacementType.GROUND) {
                    //if near surface
                    if (player.posY + 10 > surfaceY) {
                        //try surface
                        yToUse = surfaceY;
                    } else {
                        //try cave
                        yToUse = tryY;
                        caveSpawn = true;
                    }
                } else if (randomEntityList.spawnProfile.spawnType == EnumSpawnPlacementType.CAVE) {
                    caveSpawn = true;
                    yToUse = tryY;
                } else if (randomEntityList.spawnProfile.spawnType == EnumSpawnPlacementType.SURFACE) {
                    //redundant given above defaults, but here for sake of clarity
                    caveSpawn = false;
                    yToUse = surfaceY;
                } else if (randomEntityList.spawnProfile.spawnType == EnumSpawnPlacementType.WATER) {
                    yToUse = tryY;
                    BlockPos pos = new BlockPos(tryX, yToUse, tryZ);
                    IBlockState state = player.world.getBlockState(pos);
                    if (state.getMaterial() != Material.WATER || player.world.getBlockState(pos.down()).getMaterial() != Material.WATER || player.world.getBlockState(pos.up()).isNormalCube()) {
                        continue;
                    }
                } else if (randomEntityList.spawnProfile.spawnType == EnumSpawnPlacementType.AIR) {
                    yToUse = tryY;
                    BlockPos pos = new BlockPos(tryX, yToUse, tryZ);
                    //IBlockState state = player.world.getBlockState(pos);
                    if (!player.world.isAirBlock(pos) || !player.world.isAirBlock(pos.up())) {
                        continue;
                    }
                }

                //CULog.dbg("spawn type chosen: " + (caveSpawn ? "cave" : "surface"));

                if (caveSpawn && storage.triesSinceWorkingCaveSpawn > tryCachedCaveSpotThreshold && storage.listGoodCavePositions.size() > 0) {
                    CULog.dbg("trying cached cave spot to spawn with");
                    BlockPos pos = storage.listGoodCavePositions.get((storage.listGoodCavePositions.size() == 1) ? 0 : rand.nextInt(storage.listGoodCavePositions.size()-1));
                    tryX = pos.getX();
                    yToUse = pos.getY();
                    tryZ = pos.getZ();
                }

                //if we cant find any surface area to spawn something, its probably because theyre on an island, support ocean spawning, but dont lockout land spawning
                if (!caveSpawn && storage.triesSinceWorkingSolidGroundSpawn > tryWaterSurfaceAreasThreshold) {
                    allowWaterSurfaceSpawn = true;
                }

                boolean isWaterSurfaceSpawned = false;

                if (requireSolidGround) {

                    storage.triesSinceWorkingSolidGroundSpawn++;

                    //TEST
                    if (!CoroUtilEntity.canSpawnMobOnGround(player.world, tryX, yToUse, tryZ)) {
                        //CULog.dbg("spawnNewMobFromProfile: canSpawnMobOnGround fail");
                        if (allowWaterSurfaceSpawn) {

                            CULog.dbg("trying surface water to spawn with");

                            BlockPos pos = new BlockPos(tryX, surfaceY, tryZ);
                            IBlockState state = player.world.getBlockState(pos);
                            //if spot at feet isnt water or where head is isnt air, we dont want to spawn them submerged or in solid block
                            if (state.getMaterial() != Material.WATER || !CoroUtilBlock.isAir(player.world.getBlockState(pos.up()).getBlock())) {
                                continue;
                            }

                            isWaterSurfaceSpawned = true;
                            //dont contribute to counter if its a water surface spawn
                            storage.triesSinceWorkingSolidGroundSpawn--;
                        } else {
                            continue;
                        }

                    }

                }

                if (ConfigAdvancedOptions.failedTriesBeforeAllowingSpawnInLitAreas != -1) {
                    if (!storage.allowSpawnInLitAreas && storage.triesSinceWorkingAnySpawn > ConfigAdvancedOptions.failedTriesBeforeAllowingSpawnInLitAreas) {
                        //give up on finding a dark spot and allow lit areas
                        CULog.dbg("couldnt find a dark area to spawn for " + ConfigAdvancedOptions.failedTriesBeforeAllowingSpawnInLitAreas + " tries, allowing spawning in lit areas now");
                        storage.allowSpawnInLitAreas = true;
                    }
                }

                boolean skipDarknessCheck = storage.allowSpawnInLitAreas || !ConfigAdvancedOptions.mobsMustSpawnInDarkness;

                if (caveSpawn) {
                    storage.triesSinceWorkingCaveSpawn++;
                    if (!CoroUtilEntity.isInDarkCave(player.world, tryX, yToUse, tryZ, true, skipDarknessCheck)) {
                        //CULog.dbg("spawnNewMobFromProfile: isInDarkCave fail");
                        continue;
                    }
                } else {
                    if (!skipDarknessCheck && player.world.getLightFromNeighbors(new BlockPos(tryX, yToUse + 1, tryZ)) >= 6) {
                        //CULog.dbg("spawnNewMobFromProfile: getLightFromNeighbors fail");
                        continue;
                    }
                }


                try {

                    String spawnStr = randomEntityList.spawnProfile.entities.get(rand.nextInt(randomEntityList.spawnProfile.entities.size()));

                    String spawn = CoroUtilEntity.getEntityNameStringFromNBTLoadedName(spawnStr);
                    String spawnStrNBT = CoroUtilEntity.getEntityNBTStringFromNBTLoadedName(spawnStr);

                    //hardcoded fixes to convert to AI taskable entities
                    if (spawn.equals("minecraft:bat")) {
                        spawn = "coroutil:bat_smart";
                    }

                    Class classToSpawn = CoroUtilEntity.getClassFromRegistry(spawn);
                    if (classToSpawn != null) {
                        if (EntityCreature.class.isAssignableFrom(classToSpawn)) {

                            EntityCreature ent = null;
                            NBTTagCompound spawnNBT = null;

                            boolean handleSpawning = true;

                            if (spawnStrNBT != "") {
                                handleSpawning = false;
                                try {
                                    spawnNBT = JsonToNBT.getTagFromJson(spawnStrNBT);
                                    spawnNBT.setString("id", spawn);
                                    spawnNBT.setBoolean(UtilEntityBuffs.dataEntityInitialSpawn, true);
                                } catch (NBTException nbtexception) {
                                    throw new CommandException("commands.summon.tagError", new Object[]{nbtexception.getMessage()});
                                }
                                ent = (EntityCreature) AnvilChunkLoader.readWorldEntityPos(spawnNBT, player.world, tryX, yToUse + 1, tryZ, true);
                            } else {
                                ent = (EntityCreature) classToSpawn.getConstructor(new Class[]{World.class}).newInstance(new Object[]{player.world});
                            }

                            //set to above the solid block we can spawn on
                            ent.setPosition(tryX, yToUse + 1, tryZ);
                            ent.onInitialSpawn(ent.world.getDifficultyForLocation(new BlockPos(ent)), (IEntityLivingData) null);
                            ent.getEntityData().setBoolean(UtilEntityBuffs.dataEntityWaveSpawned, true);
                            ent.getEntityData().setBoolean(TaskDigTowardsTarget.dataUseInvasionRules, true);

                            //store players name the mob was spawned for
                            ent.getEntityData().setString(UtilEntityBuffs.dataEntityBuffed_PlayerSpawnedFor, player.getName());

                            //set cmod data to entity
                            UtilEntityBuffs.registerAndApplyCmods(ent, randomEntityList.spawnProfile.cmods, difficultyScale);


                            if (handleSpawning) {
                                //put into nbt above before entity instanced otherwise
                                ent.getEntityData().setBoolean(UtilEntityBuffs.dataEntityInitialSpawn, true);
                                player.world.spawnEntity(ent);
                            }
                            ent.getEntityData().setBoolean(UtilEntityBuffs.dataEntityInitialSpawn, false);
                            //leave this to omniscience task if config says so
                            //ent.setAttackTarget(player);

                            //no children!
                            if (ent instanceof EntityZombie) {
                                ((EntityZombie) ent).setChild(false);
                            }

                            randomEntityList.spawnCountCurrent++;

                            storage.triesSinceWorkingAnySpawn = 0;

                            if (caveSpawn) {
                                storage.triesSinceWorkingCaveSpawn = 0;

                                BlockPos pos = new BlockPos(tryX, yToUse, tryZ);

                                if (!storage.listGoodCavePositions.contains(pos)) {
                                    CULog.dbg("found good cave spot, adding: " + pos);
                                    storage.listGoodCavePositions.add(pos);
                                }
                            }

                            //if we arent in water fallback mode and actually got a successfull spawning on ground, reset
                            if (requireSolidGround && !isWaterSurfaceSpawned) {
                                storage.triesSinceWorkingSolidGroundSpawn = 0;
                            }

                            //InvLog.dbg("skipDarknessCheck: " + skipDarknessCheck);

                            InvLog.dbg("Spawned " + randomEntityList.spawnCountCurrent + " at " + new BlockPos(tryX, yToUse, tryZ) + " mobs now: " + ent.getName() + (randomEntityList.spawnProfile.spawnType == EnumSpawnPlacementType.GROUND ? (caveSpawn ? " cavespawned" : " surfacespawned") : "") + " " + randomEntityList.spawnProfile.spawnType);
                        } else {
                            InvLog.err("only EntityCreature extended entities are supported, couldnt spawn: " + spawn);
                        }
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
        int dayAdjust = 0;

        if (!ConfigInvasion.invasionCountingPerPlayer) {
            dayAdjust = ConfigInvasion.firstInvasionNight;
        }

        int dayNumber = (int)(world.getWorldTime() / CoroUtilWorldTime.getDayLength()) + 1;
        int dayStart = (dayNumber-dayAdjust);

        if (!ConfigInvasion.invasionCountingPerPlayer) {
            if (dayStart < 0) {
                return false;
            }
        }

        return (float)dayStart % (float)ConfigInvasion.invadeEveryXDays == 0;

        /*return dayNumber >= dayAdjust &&
                (dayNumber-dayAdjust == 0 ||
                        (dayNumber-dayAdjust) % Math.max(1, ConfigInvasion.invadeEveryXDays + 1) == 0);*/
    }

    /**
     * get staticly calculated wave number based on time and config, not used for per player
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

    public static DataMobSpawnsTemplate chooseInvasionProfile(EntityPlayer player, DifficultyQueryContext context) {

        List<DataMobSpawnsTemplate> listPhase2 = new ArrayList<>();

        InvLog.dbg("choosing invasion profile for player: " + player.getName() + ", difficulty: " + context.getDifficulty());

        if (!ConfigCoroUtil.mobSpawnsWaveToForceUse.equals("")) {
            for (DataMobSpawnsTemplate spawns : DifficultyDataReader.getData().listMobSpawnTemplates) {
                if (spawns.name.equals(ConfigCoroUtil.mobSpawnsWaveToForceUse)) {
                    return spawns;
                }
            }
            InvLog.err("Couldnt find mob spawn profile: " + ConfigCoroUtil.mobSpawnsWaveToForceUse);
        }

        //System.out.println("phase 1 choice count: " + DifficultyDataReader.getData().listMobSpawnTemplates.size());
        for (DataMobSpawnsTemplate spawns : DifficultyDataReader.getData().listMobSpawnTemplates) {

            boolean fail = false;
            InvLog.dbg("evaluating conditions for spawn template: " + spawns.toString());
            for (DataCondition condition : DeserializerAllJson.getConditionsFlattened(spawns.conditions)) {

                if (!(condition instanceof ConditionRandom)) {
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
            ConditionInvasionNumber condInv = (ConditionInvasionNumber)condition;
            return (condInv.min == -1 || context.getInvasionNumber() >= condInv.min) &&
                    (condInv.max == -1 || context.getInvasionNumber() <= condInv.max);
        } else if (condition instanceof ConditionInvasionRate) {
            return context.getInvasionNumber() % ((ConditionInvasionRate) condition).rate == 0;
        } else if (condition instanceof ConditionModLoaded) {
            if (((ConditionModLoaded) condition).isInverted()) {
                return !Loader.isModLoaded(((ConditionModLoaded) condition).mod_id);
            } else {
                return Loader.isModLoaded(((ConditionModLoaded) condition).mod_id);
            }
        } else if (condition instanceof ConditionGameStages) {
            return CoroUtilCrossMod.gameStages_hasStages(player, ((ConditionGameStages) condition).game_stages, ((ConditionGameStages) condition).matchAll());
        }
        return false;
    }

    /**
     * Expected to only be run during actual invasion nights
     *
     * DOES NOT account for min nights required etc
     *
     * @param world
     * @return
     */
    public static boolean isAnyoneBeingInvadedTonight(World world) {
        boolean foundNotSkipping = false;
        for (EntityPlayer player : world.playerEntities) {
            //System.out.println(player.getDisplayNameString() + ": " + player.getEntityData().getBoolean(DynamicDifficulty.dataPlayerInvasionSkipping) + ", " + player.getEntityData().getBoolean(DynamicDifficulty.dataPlayerInvasionSkipping));
            if (!isPlayerSkippingInvasion(player)) {
                foundNotSkipping = true;
                break;
            }
        }

        return foundNotSkipping;
    }

    public static boolean isPlayerSkippingInvasion(EntityPlayer player) {
        return player.getEntityData().getBoolean(DynamicDifficulty.dataPlayerInvasionSkipping) ||
                player.getEntityData().getBoolean(DynamicDifficulty.dataPlayerInvasionSkippingTooSoon);
    }

    public static boolean shouldLockOutFeaturesForPossibleActiveInvasion(World world) {
        if (CoroUtilWorldTime.isNightPadded(world) && InvasionManager.isInvasionTonight(world)) {
            if (InvasionManager.isAnyoneBeingInvadedTonight(world)) {
                return true;
            }
        }
        return false;
    }
}
