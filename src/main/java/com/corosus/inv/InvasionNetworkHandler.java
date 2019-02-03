package com.corosus.inv;

import com.corosus.inv.network.RequestDPSData;
import com.corosus.inv.network.SendDPSData;
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

        INSTANCE.registerMessage(RequestDPSData.MessageHandler.class, RequestDPSData.class, nextID(), Side.SERVER);
        INSTANCE.registerMessage(SendDPSData.MessageHandler.class, SendDPSData.class, nextID(), Side.CLIENT);

    }

}
