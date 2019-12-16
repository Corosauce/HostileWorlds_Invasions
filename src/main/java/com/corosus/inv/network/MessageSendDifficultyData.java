package com.corosus.inv.network;

import CoroUtil.difficulty.DifficultyInfoPlayer;
import CoroUtil.forge.CULog;
import com.corosus.inv.block.TileEntitySacrifice;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageSendDifficultyData implements IMessage {

    public DifficultyInfoPlayer difficultyInfoPlayer;
    private BlockPos tileEntityPos = new BlockPos(0, 0, 0);

    //used by client generic instantiation
    public MessageSendDifficultyData() {
        difficultyInfoPlayer = new DifficultyInfoPlayer();
    }

    public MessageSendDifficultyData(DifficultyInfoPlayer difficultyInfoPlayer, TileEntitySacrifice tEnt) {
        this.difficultyInfoPlayer = difficultyInfoPlayer;
        this.tileEntityPos = tEnt.getPos();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        tileEntityPos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());

        difficultyInfoPlayer.fromBytes(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(tileEntityPos.getX());
        buf.writeInt(tileEntityPos.getY());
        buf.writeInt(tileEntityPos.getZ());

        difficultyInfoPlayer.toBytes(buf);
    }

    public static class MessageHandler implements IMessageHandler<MessageSendDifficultyData, IMessage> {

        @Override
        public IMessage onMessage(MessageSendDifficultyData message, MessageContext ctx) {
            CULog.dbg("dps: " + message.difficultyInfoPlayer.difficultyDPS);

            Minecraft.getMinecraft().addScheduledTask(() -> {
                TileEntity tEnt = Minecraft.getMinecraft().world.getTileEntity(message.tileEntityPos);
                if (tEnt instanceof TileEntitySacrifice) {
                    TileEntitySacrifice tileEntitySacrifice = (TileEntitySacrifice) tEnt;
                    tileEntitySacrifice.setDifficultyInfoPlayer(message.difficultyInfoPlayer);
                }
            });


            return null;
        }

    }
}
