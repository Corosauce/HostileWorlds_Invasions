package com.corosus.inv.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class RequestDPSData implements IMessage {


    @Override
    public void fromBytes(ByteBuf buf) {

    }

    @Override
    public void toBytes(ByteBuf buf) {

    }

    public static class MessageHandler implements IMessageHandler<RequestDPSData, IMessage> {

        @Override
        public IMessage onMessage(RequestDPSData message, MessageContext ctx) {
            return null;
        }

    }
}
