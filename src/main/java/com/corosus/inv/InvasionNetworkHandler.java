package com.corosus.inv;

import com.corosus.inv.network.MessageRequestDifficultyData;
import com.corosus.inv.network.MessageSendDifficultyData;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class InvasionNetworkHandler {

    public final static SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(Invasion.modID);

    private static int ID = 0;

    public static int nextID() {
        return ID++;
    }

    public static void initNetworking() {

        INSTANCE.registerMessage(MessageRequestDifficultyData.MessageHandler.class, MessageRequestDifficultyData.class, nextID(), Side.SERVER);
        INSTANCE.registerMessage(MessageSendDifficultyData.MessageHandler.class, MessageSendDifficultyData.class, nextID(), Side.CLIENT);

    }

}
