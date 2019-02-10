package com.corosus.inv.network;

import CoroUtil.difficulty.DifficultyInfoPlayer;
import CoroUtil.difficulty.DynamicDifficulty;
import CoroUtil.forge.CULog;
import CoroUtil.util.BlockCoord;
import com.corosus.inv.InvasionNetworkHandler;
import com.corosus.inv.block.TileEntitySacrifice;
import com.corosus.inv.config.ConfigInvasion;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageRequestDifficultyData implements IMessage {

    private int dimID = 0;
    private BlockPos tileEntityPos = new BlockPos(0, 0, 0);

    //used by client generic instantiation
    public MessageRequestDifficultyData() {

    }

    public MessageRequestDifficultyData(World world, BlockPos tileEntityPos) {
        //username = player.getName();
        this.tileEntityPos = tileEntityPos;
        this.dimID = world.provider.getDimension();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        dimID = buf.readInt();
        tileEntityPos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(dimID);
        buf.writeInt(tileEntityPos.getX());
        buf.writeInt(tileEntityPos.getY());
        buf.writeInt(tileEntityPos.getZ());
    }

    public static class MessageHandler implements IMessageHandler<MessageRequestDifficultyData, IMessage> {

        @Override
        public IMessage onMessage(MessageRequestDifficultyData message, MessageContext ctx) {

            FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(() -> {

                World world = DimensionManager.getWorld(message.dimID);
                if (world != null) {
                    TileEntity tEnt = world.getTileEntity(message.tileEntityPos);

                    if (tEnt instanceof TileEntitySacrifice) {

                        /**
                         * We specifically want the player that owns the tile entity, not whos sending the packet request
                         */
                        EntityPlayer player = ((TileEntitySacrifice) tEnt).getPlayer();

                        if (player instanceof EntityPlayerMP) {

                            CULog.dbg("request dps from " + player.getName());

                            DifficultyInfoPlayer difficultyInfoPlayer = new DifficultyInfoPlayer(message.tileEntityPos);

                            difficultyInfoPlayer.updateData(player, ConfigInvasion.Sacrifice_CountNeeded, ConfigInvasion.Sacrifice_CountNeeded_Multiplier);
                            ((TileEntitySacrifice) tEnt).setDifficultyInfoPlayer(difficultyInfoPlayer);

                            InvasionNetworkHandler.INSTANCE.sendTo(new MessageSendDifficultyData(difficultyInfoPlayer, (TileEntitySacrifice) tEnt), (EntityPlayerMP) player);

                        }
                    }
                }

            });

            return null;
        }

    }
}
