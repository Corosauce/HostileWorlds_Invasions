package com.corosus.inv.config;

import java.io.File;

import modconfig.ConfigComment;
import modconfig.IConfigCategory;
import net.minecraft.util.math.MathHelper;

public class ConfigInvasion implements IConfigCategory {

	@ConfigComment("Days before invasions start, not specific to each player")
	public static int warmupDaysToFirstInvasion = 3;
	@ConfigComment("Days between each invasion")
	public static int daysBetweenInvasions = 3;
	
	@ConfigComment("Max allowed extra spawns at highest difficulty")
	public static int invasion_Spawns_Max = 50;
	@ConfigComment("Starting spawncount at lowest difficulty")
	public static int invasion_Spawns_Min = 10;
	@ConfigComment("How fast it increases spawnrate as the difficulty increases, 2 = doubled rate, 0.5 = halved rate")
	public static double invasion_Spawns_ScaleRate = 1D;
	
	@ConfigComment("Max allowed target range of mobs at highest difficulty")
	public static int invasion_TargettingRange_Max = 256;
	@ConfigComment("Starting target range of mobs at lowest difficulty")
	public static int invasion_TargettingRange_Min = 30;
	@ConfigComment("How fast it increases target range as the difficulty increases, 2 = doubled rate, 0.5 = halved rate")
	public static double invasion_TargettingRange_ScaleRate = 1D;
	
	@ConfigComment("Max allowed chance to convert mob to digger type at highest difficulty, 1 = 100% chance, 0.5 = 50% chance")
	public static double invasion_DiggerConvertChance_Max = 1D;
	@ConfigComment("Starting allowed chance to convert mob to digger type at lowest difficulty, 1 = 100% chance, 0.5 = 50% chance")
	public static double invasion_DiggerConvertChance_Min = 0.1D;
	@ConfigComment("How fast it increases convert chance as the difficulty increases, 2 = doubled rate, 0.5 = halved rate")
	public static double invasion_DiggerConvertChance_ScaleRate = 1D;
	
	@ConfigComment("Prevent players from sleeping through the night during invasion nights")
	public static boolean preventSleepDuringInvasions = true;

	@Override
	public String getName() {
		return "InvasionConfig";
	}

	@Override
	public String getRegistryName() {
		return "invasionConfig";
	}

	@Override
	public String getConfigFileName() {
		return "HW_Invasions" + File.separator + "InvasionConfig";
	}

	@Override
	public String getCategory() {
		return "Misc";
	}

	@Override
	public void hookUpdatedValues() {
		
		warmupDaysToFirstInvasion = MathHelper.clamp_int(warmupDaysToFirstInvasion, 0, 99);
		daysBetweenInvasions = MathHelper.clamp_int(daysBetweenInvasions, 0, 99);
		
	}

}
