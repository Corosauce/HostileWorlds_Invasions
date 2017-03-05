package com.corosus.inv.capabilities;

import com.corosus.inv.InvasionWave;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

public class ExtendedPlayerStorage implements Capability.IStorage<PlayerDataInstance> {

    @Override
    public void readNBT(Capability<PlayerDataInstance> capability, PlayerDataInstance instance, EnumFacing facing, NBTBase nbt) {
        if(!(instance instanceof PlayerDataInstance) || !(nbt instanceof NBTTagCompound))
            return;
        PlayerDataInstance extendedPlayer = (PlayerDataInstance)instance;
        NBTTagCompound extTagCompound = (NBTTagCompound)nbt;
        extendedPlayer.readNBT(extTagCompound);
    }

    @Override
    public NBTBase writeNBT(Capability<PlayerDataInstance> capability, PlayerDataInstance instance, EnumFacing facing) {
        if(!(instance instanceof PlayerDataInstance))
            return null;
        PlayerDataInstance extendedPlayer = (PlayerDataInstance)instance;
        NBTTagCompound extTagCompound = new NBTTagCompound();
        extendedPlayer.writeNBT(extTagCompound);
        return extTagCompound;
    }
}
