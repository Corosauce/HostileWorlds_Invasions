package com.corosus.inv;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import CoroUtil.util.BlockCoord;
import CoroUtil.util.CoroUtilMisc;
import CoroUtil.difficulty.DynamicDifficulty;

import CoroUtil.util.UtilMining;

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
	public void execute(MinecraftServer server, ICommandSender var1, String[] var2) {
		
		/*if (!(var1 instanceof EntityPlayerMP)) {
			System.out.println("Works for actual players only");
			return;
		}*/

		EntityPlayer player = null;
		if (var1 instanceof EntityPlayer) {
			player = (EntityPlayer) var1;
		}
		World world = var1.getEntityWorld();
		int dimension = world.provider.getDimension();
		BlockPos posBlock = var1.getPosition();
		Vec3d posVec = var1.getPositionVector();
		
		try {
			
			world = DimensionManager.getWorld(0);
			long dayNumber = (world.getWorldTime() / 24000) + 1;
			
			if (var2.length < 1)
	        {
				if ((var1 instanceof EntityPlayerMP)) {
					EntityPlayerMP ent = (EntityPlayerMP) var1;
		    		//net.minecraft.util.Vec3 posVec = ent.getPosition(1F);
		    		/*net.minecraft.util.math.Vec3d */posVec = new net.minecraft.util.math.Vec3d(ent.posX, ent.posY + (ent.getEyeHeight() - ent.getDefaultEyeHeight()), ent.posZ);//player.getPosition(1F);
		    		BlockCoord pos = new BlockCoord(MathHelper.floor_double(posVec.xCoord), MathHelper.floor_double(posVec.yCoord), MathHelper.floor_double(posVec.zCoord));
		    		//long dayNumber = (ent.worldObj.getWorldTime() / 24000) + 1;
		    		CoroUtilMisc.sendCommandSenderMsg(ent, "day: " + dayNumber + ", difficulty for this area: " + DynamicDifficulty.getDifficultyScaleAverage(ent.worldObj, ent, pos));
				} else {
					var1.addChatMessage(new TextComponentString("day: " + dayNumber));
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

						BlockPos pos = new BlockPos(x, y, z);
						IBlockState state = world.getBlockState(pos);
						Block block = state.getBlock();
						boolean canMine = UtilMining.canMineBlock(world, new BlockCoord(x, y, z), block);
						float blockStrength = state.getBlockHardness(world, pos);

						var1.addChatMessage(new TextComponentString("can mine? "/* + x + ", " + y + ", " + z + "?: "*/ + canMine + ", hardness: " + blockStrength + ", block: " + block.getLocalizedName()));
					}

				} else if (var2[0].equalsIgnoreCase("skip")) {
					if (player != null) {
						EventHandlerForge.skipNextInvasionForPlayer(player);
					} else {
						var1.addChatMessage(new TextComponentString("requires player reference"));
					}
				} else if (var2[0].equalsIgnoreCase("ti")) {
					if (player != null) {
						BlockCoord pos = new BlockCoord(MathHelper.floor_double(posVec.xCoord), MathHelper.floor_double(posVec.yCoord), MathHelper.floor_double(posVec.zCoord));
						float difficultyScale = DynamicDifficulty.getDifficultyScaleAverage(world, player, pos);
						EventHandlerForge.initNewInvasion(player, difficultyScale);
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
	public boolean checkPermission(MinecraftServer server, ICommandSender par1ICommandSender)
    {
		return true;
	}
	
	@Override
	public int getRequiredPermissionLevel() {
		return 0;
	}

}
