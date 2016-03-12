package com.corosus.inv.ai.tasks;

import java.util.Random;

import com.corosus.inv.util.UtilMining;

import CoroPets.ai.ITaskInitializer;
import CoroUtil.util.BlockCoord;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class TaskDigTowardsTarget extends EntityAIBase implements ITaskInitializer
{
    private EntityCreature entity = null;
    private BlockCoord posCurMining = null;
    private EntityLivingBase targetLastTracked = null;
    private int digTimeCur = 0;
    private int digTimeMax = 15*20;
    private double curBlockDamage = 0D;
    //doesnt factor in ai tick delay of % 3
    private int noMoveTicks = 0;

    public TaskDigTowardsTarget()
    {
        //this.setMutexBits(3);
    }
    
    @Override
    public void setEntity(EntityCreature creature) {
    	this.entity = creature;
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    public boolean shouldExecute()
    {
    	//this method ticks every 3 ticks in best conditions
    	
    	//prevent day digging, easy way to prevent digging once invasion ends
    	if (entity.worldObj.isDaytime()) return false;
    	
    	//System.out.println("should?");
    	/**
    	 * Zombies wouldnt try to mine if they are bunched up behind others, as they are still technically pathfinding, this helps resolve that issue, and maybe water related issues
    	 */
    	double movementThreshold = 0.05D;
    	int noMoveThreshold = 5;
    	if (posCurMining == null && entity.motionX < movementThreshold && entity.motionX > -movementThreshold && 
    			entity.motionZ < movementThreshold && entity.motionZ > -movementThreshold) {
    		
    		noMoveTicks++;
    		
    	} else {
    		noMoveTicks = 0;
    	}
    	
    	//System.out.println("noMoveTicks: " + noMoveTicks);
    	/*if (noMoveTicks > noMoveThreshold) {
    		System.out.println("ent not moving enough, try to mine!? " + noMoveTicks + " ent: " + entity.getEntityId());
    	}*/
    	
    	if (!entity.onGround && !entity.isInWater()) return false;
    	//return true if not pathing, has target
    	if (entity.getAttackTarget() != null || targetLastTracked != null) {
    		if (entity.getAttackTarget() == null) {
    			//System.out.println("forcing reset of target2");
    			entity.setAttackTarget(targetLastTracked);
    		} else {
    			targetLastTracked = entity.getAttackTarget();
    		}
    		//if (!entity.getNavigator().noPath()) System.out.println("path size: " + entity.getNavigator().getPath().getCurrentPathLength());
    		if (entity.getNavigator().noPath() || entity.getNavigator().getPath().getCurrentPathLength() == 1 || noMoveTicks > noMoveThreshold) {
    		//if (entity.motionX < 0.1D && entity.motionZ < 0.1D) {
    			if (updateBlockToMine()) {
    				//System.out.println("should!");
    				return true;
    			}
    		} else {
    			//clause for if stuck trying to path
    			
    		}
    	}
    	
        return false;
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    public boolean continueExecuting()
    {
    	//System.out.println("continue!");
    	if (posCurMining == null) return false;
    	if (entity.worldObj.getBlock(posCurMining.posX, posCurMining.posY, posCurMining.posZ) != Blocks.air) {
    		return true;
    	} else {
    		posCurMining = null;
    		//System.out.println("ending execute");
    		return false;
    	}
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    public void startExecuting()
    {
    	//System.out.println("start!");
    }

    /**
     * Resets the task
     */
    public void resetTask()
    {
    	//System.out.println("reset!");
    	digTimeCur = 0;
    	curBlockDamage = 0;
    	posCurMining = null;
    }

    /**
     * Updates the task
     */
    public void updateTask()
    {
    	//System.out.println("running!");
    	
    	if (entity.getAttackTarget() != null) {
    		targetLastTracked = entity.getAttackTarget();
    	} else {
    		if (targetLastTracked != null) {
    			//System.out.println("forcing reset of target");
    			entity.setAttackTarget(targetLastTracked);
    		}
    	}
    	
    	tickMineBlock();
    }
    
    public boolean updateBlockToMine() {
    	
    	posCurMining = null;
    	
    	double vecX = entity.getAttackTarget().posX - entity.posX;
    	//feet
    	double vecY = entity.getAttackTarget().posY - entity.boundingBox.minY;
    	double vecZ = entity.getAttackTarget().posZ - entity.posZ;
    	
    	double dist = (double)MathHelper.sqrt_double(vecX * vecX/* + vecY * vecY*/ + vecZ * vecZ);
    	
    	double scanX = entity.posX + (vecX / dist);
    	double scanZ = entity.posZ + (vecZ / dist);
    	
    	Random rand = new Random(entity.worldObj.getTotalWorldTime());
    	
    	if (rand.nextBoolean()/*Math.abs(vecX) < Math.abs(vecZ)*/) {
    		//scanX = entity.posX;
        	scanZ = entity.posZ + 0;
    	} else {
    		scanX = entity.posX + 0;
        	//scanZ = entity.posZ;
    	}
    	
    	BlockCoord coords = new BlockCoord(MathHelper.floor_double(scanX), MathHelper.floor_double(entity.boundingBox.minY + 1), MathHelper.floor_double(scanZ));
    	
    	//IBlockState state = entity.worldObj.getBlockState(coords);
    	Block block = entity.worldObj.getBlock(coords.posX, coords.posY, coords.posZ);
    	
    	//System.out.println("ahead to target: " + block);
    	
    	if (UtilMining.canMineBlock(entity.worldObj, coords, block)) {
    		posCurMining = coords;
    		//entity.worldObj.setBlock(coords.posX, coords.posY, coords.posZ, Blocks.air);
    		return true;
    	} else {
    		if (vecY > 0) {
    			coords.posY++;
    			//coords = coords.add(0, 1, 0);
    			//state = entity.worldObj.getBlockState(coords);
    	    	block = entity.worldObj.getBlock(coords.posX, coords.posY, coords.posZ);
        		if (UtilMining.canMineBlock(entity.worldObj, coords, block)) {
            		posCurMining = coords;
            		return true;
        		}
    		}
    		
    		//if dont or cant dig up, continue strait
    		coords.posY--;
    		//coords = coords.add(0, -1, 0);
    		
    		//state = entity.worldObj.getBlockState(coords);
	    	block = entity.worldObj.getBlock(coords.posX, coords.posY, coords.posZ);//state.getBlock();
    		if (UtilMining.canMineBlock(entity.worldObj, coords, block)) {
        		posCurMining = coords;
        		return true;
    		} else {
    			//try to dig down if all else failed and target is below
    			if (vecY < 0) {
    				//coords = coords.add(0, -1, 0);
    				coords.posY--;
    	    		//state = entity.worldObj.getBlockState(coords);
    	    		//block = state.getBlock();
    	    		block = entity.worldObj.getBlock(coords.posX, coords.posY, coords.posZ);
    		    	
    	    		if (UtilMining.canMineBlock(entity.worldObj, coords, block)) {
    	        		posCurMining = coords;
    	        		return true;
    	    		}
    			}
    		}
    		
    		return false;
    	}
    }
    
    public void tickMineBlock() {
    	if (posCurMining == null) return;
    	
    	//force stop mining if pushed away
    	if (entity.getDistance(posCurMining.posX, posCurMining.posY, posCurMining.posZ) > 3) {
    		entity.worldObj.destroyBlockInWorldPartially(entity.getEntityId(), posCurMining.posX, posCurMining.posY, posCurMining.posZ, 0);
    		posCurMining = null;
    		return;
    	}
    	
    	entity.getNavigator().clearPathEntity();
    	
    	Block block = entity.worldObj.getBlock(posCurMining.posX, posCurMining.posY, posCurMining.posZ);
    	//double blockStrength = block.getBlockHardness(entity.worldObj, posCurMining.posX, posCurMining.posY, posCurMining.posZ);
    	//IBlockState state = entity.worldObj.getBlockState(posCurMining);
    	//Block block = state.getBlock();
    	
    	double blockStrength = block.getBlockHardness(entity.worldObj, posCurMining.posX, posCurMining.posY, posCurMining.posZ);
    	
    	if (blockStrength == -1) {
    		posCurMining = null;
    		return;
    	}
    	
    	curBlockDamage += 0.01D / blockStrength;
    	
    	if (curBlockDamage > 1D) {
    		entity.worldObj.destroyBlockInWorldPartially(entity.getEntityId(), posCurMining.posX, posCurMining.posY, posCurMining.posZ, 0);
    		entity.worldObj.setBlock(posCurMining.posX, posCurMining.posY, posCurMining.posZ, Blocks.air);
    		
    	} else {
    		entity.worldObj.destroyBlockInWorldPartially(entity.getEntityId(), posCurMining.posX, posCurMining.posY, posCurMining.posZ, (int)(curBlockDamage * 10D));
    	}
    	if (entity.worldObj.getTotalWorldTime() % 10 == 0) {
    		entity.swingItem();
    		//System.out.println("swing!");
    		entity.worldObj.playSoundEffect(posCurMining.getX(), posCurMining.getY(), posCurMining.getZ(), block.stepSound.getBreakSound(), 0.5F, 1F);
    	}
    }
}
