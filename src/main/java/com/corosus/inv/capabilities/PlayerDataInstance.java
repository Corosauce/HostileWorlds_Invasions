package com.corosus.inv.capabilities;

import com.corosus.inv.InvasionEntitySpawn;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Corosus on 3/4/2017.
 */
public class PlayerDataInstance {

    //public InvasionEntitySpawn invasionEntitySpawn = new InvasionEntitySpawn();
    List<InvasionEntitySpawn> listSpawnables = new ArrayList<>();
    public int val;

    public PlayerDataInstance() {

    }

    public void readNBT(NBTTagCompound nbtTagCompound) {
        System.out.println("read");
        val = nbtTagCompound.getInteger("val");
    }

    public void writeNBT(NBTTagCompound nbtTagCompound) {
        System.out.println("write");
        nbtTagCompound.setInteger("val", val);
    }

}
