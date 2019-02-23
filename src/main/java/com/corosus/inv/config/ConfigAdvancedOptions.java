package com.corosus.inv.config;

import java.io.File;

import modconfig.ConfigComment;
import modconfig.IConfigCategory;

public class ConfigAdvancedOptions implements IConfigCategory {

	public static int spawnRangeMin = 24;
	public static int spawnRangeMax = 100;

	@ConfigComment("amount of times it loops per aiTickRateSpawning trigger")
	public static int attemptsPerSpawn = 100;

	public static int aiTickRateEnhance = 200;
	@ConfigComment("Used during slow player tick run once every 20 ticks, so be carefull of what values used")
	public static int aiTickRateSpawning = 40;
	public static int aiOmniscienceRange = 100;
	//public static boolean enhanceOnlyExtraSpawnedForDigging = true;
	public static String blackListPlayers = "";
	public static boolean useBlacklistAsWhitelist = false;
	@ConfigComment("Eg: If the invasion spawns in creepers, all creepers that already existed before the invasion also get omniscience, etc")
	public static boolean enhanceAllMobsOfSpawnedTypesForOmniscience = true;

	@ConfigComment("will be overridden by failedTriesBeforeAllowingSpawnInLitAreas if it triggers")
	public static boolean mobsMustSpawnInDarkness = true;

	@ConfigComment("-1 to disable")
	public static int failedTriesBeforeAllowingSpawnInLitAreas = 1000;

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
