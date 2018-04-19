package com.corosus.inv.config;

import java.io.File;

import modconfig.IConfigCategory;

public class ConfigAdvancedOptions implements IConfigCategory {

	public static int spawnRangeMin = 20;
	public static int spawnRangeMax = 40;
	public static int aiTickRateEnhance = 200;
	public static int aiTickRatePath = 100;
	public static int aiTickRateSpawning = 10;
	public static double speedBoostBase = 0.8D;
	//public static int aiEnhanceRange = 100;
	public static int aiOmniscienceRange = 100;
	public static int pathFailDelayPenalty = 200;
	public static int pathDelayBase = 50;
	public static boolean enhanceOnlyExtraSpawnedForDigging = true;
	public static String blackListPlayers = "";
	public static boolean useBlacklistAsWhitelist = false;

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
