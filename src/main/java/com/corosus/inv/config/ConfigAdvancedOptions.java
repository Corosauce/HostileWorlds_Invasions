package com.corosus.inv.config;

import java.io.File;

import modconfig.ConfigComment;
import modconfig.IConfigCategory;

public class ConfigAdvancedOptions implements IConfigCategory {

	public static int spawnRangeMin = 24;
	public static int spawnRangeMax = 50;

	public static int attemptsPerSpawn = 100;

	public static int aiTickRateEnhance = 200;
	public static int aiTickRatePath = 100;
	public static int aiTickRateSpawning = 10;
	public static double speedBoostBase = 0.8D;
	//public static int aiEnhanceRange = 100;
	public static int aiOmniscienceRange = 100;
	public static int pathFailDelayPenalty = 200;
	public static int pathDelayBase = 50;
	//public static boolean enhanceOnlyExtraSpawnedForDigging = true;
	public static String blackListPlayers = "";
	public static boolean useBlacklistAsWhitelist = false;
	@ConfigComment("Eg: If the invasion spawns in creepers, all creepers that already existed before the invasion also get omniscience, etc")
	public static boolean enhanceAllMobsOfSpawnedTypesForOmniscience = true;

	/*@ConfigComment("Max allowed extra spawns at highest difficulty")
	public static int invasion_Spawns_Max = 50;
	@ConfigComment("Starting spawncount at lowest difficulty")
	public static int invasion_Spawns_Min = 10;
	@ConfigComment("How fast it increases spawnrate as the difficulty increases, 2 = doubled rate, 0.5 = halved rate")
	public static double invasion_Spawns_ScaleRate = 1D;*/

	/*@ConfigComment("Max allowed target range of mobs at highest difficulty")
	public static int Invasion_TargettingRange_Max = 256;
	@ConfigComment("Starting target range of mobs at lowest difficulty")
	public static int Invasion_TargettingRange_Min = 30;
	@ConfigComment("How fast it increases target range as the difficulty increases, 2 = doubled rate, 0.5 = halved rate")
	public static double Invasion_TargettingRange_ScaleRate = 1D;

	@ConfigComment("Max allowed chance to convert mob to digger type at highest difficulty, 1 = 100% chance, 0.5 = 50% chance")
	public static double Invasion_DiggerConvertChance_Max = 1D;
	@ConfigComment("Starting allowed chance to convert mob to digger type at lowest difficulty, 1 = 100% chance, 0.5 = 50% chance")
	public static double Invasion_DiggerConvertChance_Min = 0.1D;
	@ConfigComment("How fast it increases convert chance as the difficulty increases, 2 = doubled rate, 0.5 = halved rate")
	public static double Invasion_DiggerConvertChance_ScaleRate = 1D;*/

	@Override
	public String getName() {
		return "AdvancedOptions";
	}

	@Override
	public String getRegistryName() {
		return "invasionAdvancedConfigOptions";
	}

	@Override
	public String getConfigFileName() {
		return "HW_Invasions" + File.separator + getName();
	}

	@Override
	public String getCategory() {
		return "AdvancedOptions";
	}

	@Override
	public void hookUpdatedValues() {
		
	}

}
