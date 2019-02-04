package com.corosus.inv.network;

import CoroUtil.difficulty.DifficultyInfoPlayer;
import CoroUtil.difficulty.DynamicDifficulty;
import CoroUtil.forge.CULog;
import CoroUtil.util.BlockCoord;
import com.corosus.inv.InvasionNetworkHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.text.DecimalFormat;

public class MessageRequestDifficultyData implements IMessage {

    private String username;
    private BlockPos tileEntityPos = new BlockPos(0, 0, 0);

    public MessageRequestDifficultyData() {

    }

    public MessageRequestDifficultyData(EntityPlayer player, BlockPos tileEntityPos) {
        username = player.getName();
        this.tileEntityPos = tileEntityPos;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        tileEntityPos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        username = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(tileEntityPos.getX());
        buf.writeInt(tileEntityPos.getY());
        buf.writeInt(tileEntityPos.getZ());
        ByteBufUtils.writeUTF8String(buf, username);
    }

    public static class MessageHandler implements IMessageHandler<MessageRequestDifficultyData, IMessage> {

        @Override
        public IMessage onMessage(MessageRequestDifficultyData message, MessageContext ctx) {
            CULog.dbg("request dps from " + message.username);

            MinecraftServer mc = FMLCommonHandler.instance().getMinecraftServerInstance();

            mc.addScheduledTask(() -> {
                EntityPlayerMP player = mc.getPlayerList().getPlayerByUsername(message.username);

                if (player != null) {
                    //TODO: ship difficulty data
                    DifficultyInfoPlayer difficultyInfoPlayer = new DifficultyInfoPlayer();
                    difficultyInfoPlayer.dps = DynamicDifficulty.getDifficultyScaleForPosDPS(player.world, new BlockCoord(player.getPosition()));
                    InvasionNetworkHandler.INSTANCE.sendTo(new MessageSendDifficultyData(difficultyInfoPlayer, message.tileEntityPos), player);
                }

            });

            return null;
        }

    }
}
