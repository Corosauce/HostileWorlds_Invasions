package com.corosus.inv.network;

import CoroUtil.difficulty.DifficultyInfoPlayer;
import CoroUtil.forge.CULog;
import com.corosus.inv.block.TileEntitySacrifice;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageSendDifficultyData implements IMessage {

    public DifficultyInfoPlayer difficultyInfoPlayer;
    private BlockPos tileEntityPos = new BlockPos(0, 0, 0);

    public MessageSendDifficultyData() {
        difficultyInfoPlayer = new DifficultyInfoPlayer();
    }

    public MessageSendDifficultyData(DifficultyInfoPlayer difficultyInfoPlayer, BlockPos tileEntityPos) {
        this.difficultyInfoPlayer = difficultyInfoPlayer;
        this.tileEntityPos = tileEntityPos;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        tileEntityPos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        difficultyInfoPlayer.dps = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(tileEntityPos.getX());
        buf.writeInt(tileEntityPos.getY());
        buf.writeInt(tileEntityPos.getZ());
        buf.writeFloat(difficultyInfoPlayer.dps);
    }

    public static class MessageHandler implements IMessageHandler<MessageSendDifficultyData, IMessage> {

        @Override
        public IMessage onMessage(MessageSendDifficultyData message, MessageContext ctx) {
            CULog.dbg("dps: " + message.difficultyInfoPlayer.dps);

            Minecraft.getMinecraft().addScheduledTask(() -> {
                TileEntity tEnt = Minecraft.getMinecraft().world.getTileEntity(message.tileEntityPos);
                if (tEnt instanceof TileEntitySacrifice) {
                    ((TileEntitySacrifice) tEnt).setDifficultyInfoPlayer(message.difficultyInfoPlayer);
                }
            });


            return null;
        }

    }
}
