package com.corosus.inv;

import com.corosus.inv.block.TileEntitySacrifice;
import com.corosus.inv.gui.SacrificeContainer;
import com.corosus.inv.gui.SacrificeGUI;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

public class InvasionGUIHandler implements IGuiHandler {

    private static InvasionGUIHandler instance;

    private InvasionGUIHandler() {
    }

    public static InvasionGUIHandler getInstance() {
        if (instance == null) {
            instance = new InvasionGUIHandler();
        }
        return instance;
    }


    @Nullable
    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tileEntity = world.getTileEntity(new BlockPos(x, y, z));

        switch (id) {
            case 0:
                return new SacrificeContainer(player.inventory, (TileEntitySacrifice) tileEntity);
            default:
                return null;
        }
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tileEntity = world.getTileEntity(new BlockPos(x, y, z));

        switch (id) {
            case 0:
                return new SacrificeGUI(player.inventory, (TileEntitySacrifice) tileEntity);
            default:
                return null;
        }
    }
}
