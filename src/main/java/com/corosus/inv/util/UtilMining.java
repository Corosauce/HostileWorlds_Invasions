package com.corosus.inv.util;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import CoroUtil.util.BlockCoord;

public class UtilMining {

    public static boolean canMineBlock(World world, BlockCoord pos, Block block) {
    	
    	//System.out.println("check: " + block);
    	
    	//dont mine tile entities
    	if (world.getTileEntity(pos.posX, pos.posY, pos.posZ) != null) {
    		return false;
    	}
    	if (block == Blocks.air) {
    		return false;
    	}
    	/*if (block == Blocks.obsidian) {
    		return false;
    	}*/
    	if (block.getMaterial().isLiquid()) {
    		return false;
    	}
    	
    	return true;
    }
	
}
