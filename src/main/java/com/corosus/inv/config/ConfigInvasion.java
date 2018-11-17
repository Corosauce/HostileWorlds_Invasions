package com.corosus.inv.config;

import java.io.File;

import modconfig.ConfigComment;
import modconfig.IConfigCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;

public class ConfigInvasion implements IConfigCategory {

	@ConfigComment("First night number that an invasion starts, not specific to each player, uses global server world time")//Days before invasions start,
	public static int firstInvasionNight = 3;
	//public static int warmupDaysToFirstInvasion = 3;
	@ConfigComment("If set to 3, there are 2 days of no invasions then the 3rd day is an invasion, etc")
	public static int invadeEveryXDays = 3;

	@ConfigComment("Max amount of invasions you can skip in a row before you are forced to deal with one")
	public static int maxConsecutiveInvasionSkips = 3;
	
	/*@ConfigComment("Max allowed extra spawns at highest difficulty")
	public static int invasion_Spawns_Max = 50;
	@ConfigComment("Starting spawncount at lowest difficulty")
	public static int invasion_Spawns_Min = 10;
	@ConfigComment("How fast it increases spawnrate as the difficulty increases, 2 = doubled rate, 0.5 = halved rate")
	public static double invasion_Spawns_ScaleRate = 1D;*/
	
	@ConfigComment("Max allowed target range of mobs at highest difficulty")
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
	public static double Invasion_DiggerConvertChance_ScaleRate = 1D;

	public static String Invasion_Message_startsTonight = TextFormatting.GOLD + "An invasion starts tonight! SpoOoOoky!";

	public static String Invasion_Message_started = TextFormatting.RED + "An invasion has started! Be prepared!";

	public static String Invasion_Message_ended = TextFormatting.GREEN + "The invasion has ended! Next invasion in %d days!";

	public static String Invasion_Message_startedButSkippedForYou = TextFormatting.GREEN + "An invasion has started! But skipped for you!";

	public static String Invasion_Message_tooLate = TextFormatting.RED + "Too late, invasion already started!";

	public static String Invasion_Message_notInvasionNight = "Not an invasion night, cant skip yet!";

	public static String Invasion_Message_alreadySkipping = TextFormatting.GREEN + "You are already skipping this nights invasion!";

	public static String Invasion_Message_skipping = TextFormatting.GREEN + "Skipping tonights invasion, skip count: %d";

	public static String Invasion_Message_skippedTooMany = TextFormatting.RED + "You've already skipped invasions %d times! You must fight!";

	public static String Invasion_Message_cantSleep = "You can't sleep during invasion nights!";
	
	@ConfigComment("Prevent players from sleeping through the night during invasion nights")
	public static boolean preventSleepDuringInvasions = true;

	@ConfigComment("For seldom used but important things to print out in production")
	public static boolean useLoggingLog = true;

	@ConfigComment("For debugging things")
	public static boolean useLoggingDebug = false;

	@ConfigComment("For logging warnings/errors")
	public static boolean useLoggingError = true;

	@ConfigComment("Use at own risk, will not support, requires game restart on change")
	public static boolean enableAdvancedDeveloperConfigFiles = false;

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
		
		firstInvasionNight = MathHelper.clamp(firstInvasionNight, 0, 99);
		invadeEveryXDays = MathHelper.clamp(invadeEveryXDays, 0, 99);
		
	}

}
