package com.corosus.inv.config;

import net.minecraft.util.MathHelper;
import modconfig.IConfigCategory;

public class InvConfig implements IConfigCategory {

	public static int warmupDays = 3;
	public static int daysBetweenAttacks = 2;
	
	public static int maxTicksForDifficulty = 20*60*60*50;
	
	public static boolean preventSleepDuringInvasions = true;
	
	@Override
	public String getConfigFileName() {
		return "InvConfig";
	}

	@Override
	public String getCategory() {
		return "Misc";
	}

	@Override
	public void hookUpdatedValues() {
		
		warmupDays = MathHelper.clamp_int(warmupDays, 0, 99);
		daysBetweenAttacks = MathHelper.clamp_int(daysBetweenAttacks, 1, 99);
		
	}

}
