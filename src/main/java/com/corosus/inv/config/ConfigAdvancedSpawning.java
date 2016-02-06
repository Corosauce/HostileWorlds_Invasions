package com.corosus.inv.config;

import java.io.File;

import net.minecraft.util.MathHelper;
import modconfig.ConfigComment;
import modconfig.IConfigCategory;

public class ConfigAdvancedSpawning implements IConfigCategory {

	@ConfigComment("Specify class of entity to spawn for wave, supports multiple entries separated by commas")
	public static String difficulty_0 = "net.minecraft.entity.monster.EntityZombie";
	public static String difficulty_1 = "net.minecraft.entity.monster.EntityZombie";
	public static String difficulty_2 = "net.minecraft.entity.monster.EntityZombie";
	public static String difficulty_3 = "net.minecraft.entity.monster.EntityZombie";
	public static String difficulty_4 = "net.minecraft.entity.monster.EntityZombie";
	public static String difficulty_5 = "net.minecraft.entity.monster.EntityZombie";
	public static String difficulty_6 = "net.minecraft.entity.monster.EntityZombie";
	public static String difficulty_7 = "net.minecraft.entity.monster.EntityZombie";
	public static String difficulty_8 = "net.minecraft.entity.monster.EntityZombie";
	public static String difficulty_9 = "net.minecraft.entity.monster.EntityZombie";
	
	@Override
	public String getConfigFileName() {
		return "HW_Invasions" + File.separator + "AdvancedSpawning";
	}

	@Override
	public String getCategory() {
		return "Spawning";
	}

	@Override
	public void hookUpdatedValues() {
		
	}

}
