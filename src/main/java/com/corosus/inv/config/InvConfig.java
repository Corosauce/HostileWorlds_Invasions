package com.corosus.inv.config;

import modconfig.IConfigCategory;

public class InvConfig implements IConfigCategory {

	public static int warmupDays = 0;
	public static int daysBetweenAttacks = 1;
	
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
		// TODO Auto-generated method stub
		
	}

}
