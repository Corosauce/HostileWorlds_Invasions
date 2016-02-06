package com.corosus.inv;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import CoroUtil.OldUtil;
import CoroUtil.pathfinding.PFQueue;
import CoroUtil.util.CoroUtil;

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
		
		if (!(var1 instanceof EntityPlayerMP)) {
			System.out.println("Works for actual players only");
			return;
		}
		
		try {
			if (var2.length < 1)
	        {
	            throw new WrongUsageException("Invalid usage");
	        }
	        else
	        {
	        
	        	if (var2[0].equalsIgnoreCase("difficulty")) {
	        		EntityPlayerMP ent = (EntityPlayerMP) var1;
	        		CoroUtil.sendPlayerMsg(ent, "difficulty for this area: " + EventHandlerForge.getDifficultyScaleAverage(ent.worldObj, ent, ent.getPosition()));
	        	}
	        	
	        }
		} catch (Exception ex) {
			System.out.println("Caught HW_Invasion command crash!!!");
			ex.printStackTrace();
		}
	}
	
	@Override
	public boolean canCommandSenderUseCommand(ICommandSender par1ICommandSender)
    {
        return par1ICommandSender.canCommandSenderUseCommand(this.getRequiredPermissionLevel(), this.getCommandName());
    }

}
