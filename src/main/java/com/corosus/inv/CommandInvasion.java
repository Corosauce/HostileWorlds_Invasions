package com.corosus.inv;

import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import CoroUtil.util.BlockCoord;
import CoroUtil.util.CoroUtil;
import CoroUtil.world.player.DynamicDifficulty;

import com.corosus.inv.util.UtilMining;

public class CommandInvasion extends CommandBase {

	@Override
	public String getCommandName() {
		return "hw_invasions";
	}

	@Override
	public String getCommandUsage(ICommandSender icommandsender) {
		return "";
	}

	@Override
	public void processCommand(ICommandSender var1, String[] var2) {
		
		/*if (!(var1 instanceof EntityPlayerMP)) {
			System.out.println("Works for actual players only");
			return;
		}*/
		
		
		
		try {
			
			World world = DimensionManager.getWorld(0);
			long dayNumber = (world.getWorldTime() / 24000) + 1;
			
			if (var2.length < 1)
	        {
				if ((var1 instanceof EntityPlayerMP)) {
					EntityPlayerMP ent = (EntityPlayerMP) var1;
		    		//net.minecraft.util.Vec3 posVec = ent.getPosition(1F);
		    		net.minecraft.util.Vec3 posVec = net.minecraft.util.Vec3.createVectorHelper(ent.posX, ent.posY + (ent.getEyeHeight() - ent.getDefaultEyeHeight()), ent.posZ);//player.getPosition(1F);
		    		BlockCoord pos = new BlockCoord(MathHelper.floor_double(posVec.xCoord), MathHelper.floor_double(posVec.yCoord), MathHelper.floor_double(posVec.zCoord));
		    		//long dayNumber = (ent.worldObj.getWorldTime() / 24000) + 1;
		    		CoroUtil.sendPlayerMsg(ent, "day: " + dayNumber + ", difficulty for this area: " + DynamicDifficulty.getDifficultyScaleAverage(ent.worldObj, ent, pos));
				} else {
					var1.addChatMessage(new ChatComponentText("day: " + dayNumber));
		    		//CoroUtil.sendPlayerMsg(ent, "day: " + dayNumber + ", difficulty for this area: " + EventHandlerForge.getDifficultyScaleAverage(ent.worldObj, ent, pos));
				}
	        }
	        else
	        {

	        	if (var2[0].equalsIgnoreCase("canMine")) {
	        		if (var2.length <= 4) {
	        			int x = Integer.valueOf(var2[1]);
		        		int y = Integer.valueOf(var2[2]);
		        		int z = Integer.valueOf(var2[3]);
		        		
		        		Block block = world.getBlock(x, y, z);
		        		boolean canMine = UtilMining.canMineBlock(world, new BlockCoord(x, y, z), block);
		        		float blockStrength = block.getBlockHardness(world, x, y, z);
		        		
		        		var1.addChatMessage(new ChatComponentText("can mine? "/* + x + ", " + y + ", " + z + "?: "*/ + canMine + ", hardness: " + blockStrength + ", block: " + block.getLocalizedName()));
		        	}
	        		
	        	} else if (var2[0].equalsIgnoreCase("difficulty")) {
	        		if ((var1 instanceof EntityPlayerMP)) {
						EntityPlayerMP ent = (EntityPlayerMP) var1;
			    		//net.minecraft.util.Vec3 posVec = ent.getPosition(1F);
			    		net.minecraft.util.Vec3 posVec = net.minecraft.util.Vec3.createVectorHelper(ent.posX, ent.posY + (ent.getEyeHeight() - ent.getDefaultEyeHeight()), ent.posZ);//player.getPosition(1F);
			    		BlockCoord pos = new BlockCoord(MathHelper.floor_double(posVec.xCoord), MathHelper.floor_double(posVec.yCoord), MathHelper.floor_double(posVec.zCoord));
			    		//long dayNumber = (ent.worldObj.getWorldTime() / 24000) + 1;
			    		CoroUtil.sendPlayerMsg(ent, "Difficulties for you: ");
			    		CoroUtil.sendPlayerMsg(ent, "player rating: " + DynamicDifficulty.getDifficultyScaleForPlayerEquipment(ent));
			    		CoroUtil.sendPlayerMsg(ent, "server time: " + DynamicDifficulty.getDifficultyScaleForPlayerServerTime(ent));
			    		CoroUtil.sendPlayerMsg(ent, "avg chunk time: " + DynamicDifficulty.getDifficultyScaleForPos(ent.worldObj, pos));
					}
	        	}
	        	
	        	
	        	
	        }
			
			
		} catch (Exception ex) {
			System.out.println("Caught HW_Invasion command crash!!!");
			ex.printStackTrace();
		}
	}
	
	/*@Override
	public boolean canCommandSenderUseCommand(ICommandSender par1ICommandSender)
    {
        return true;//par1ICommandSender.canCommandSenderUseCommand(this.getRequiredPermissionLevel(), this.getCommandName());
    }*/
	
	@Override
	public boolean canCommandSenderUseCommand(ICommandSender p_71519_1_) {
		return true;
	}
	
	@Override
	public int getRequiredPermissionLevel() {
		return 0;
	}

}
