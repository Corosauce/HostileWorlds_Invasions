package com.corosus.inv.capabilities;

import CoroUtil.difficulty.data.DataActionMobSpawns;
import CoroUtil.difficulty.data.DataMobSpawnsTemplate;
import com.corosus.inv.InvasionEntitySpawn;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Corosus on 3/4/2017.
 */
public class PlayerDataInstance {

    //public InvasionEntitySpawn invasionEntitySpawn = new InvasionEntitySpawn();
    List<InvasionEntitySpawn> listSpawnables = new ArrayList<>();
    public int val;
    private EntityPlayer player;

    public PlayerDataInstance() {

    }

    public EntityPlayer getPlayer() {
        return player;
    }

    public PlayerDataInstance setPlayer(EntityPlayer player) {
        this.player = player;
        return this;
    }

    public void initNewInvasion(DataMobSpawnsTemplate profile) {
        resetInvasion();

        //convert data to runtime
        for (DataActionMobSpawns spawns : profile.spawns) {
            InvasionEntitySpawn newSpawn = new InvasionEntitySpawn();
            newSpawn.spawnProfile = spawns;
            listSpawnables.add(newSpawn);
        }

        //TEST
        /*NBTTagCompound nbt = new NBTTagCompound();
        writeNBT(nbt);

        resetInvasion();

        readNBT(nbt);*/
    }

    public void stopInvasion() {
        resetInvasion();
    }

    public void resetInvasion() {
        for (InvasionEntitySpawn spawns : listSpawnables) {
            spawns.clear();
        }
        listSpawnables.clear();
    }

    public void readNBT(NBTTagCompound nbtTagCompound) {

        //if (true) return;

        System.out.println("read");
        val = nbtTagCompound.getInteger("val");

        NBTTagCompound nbt = nbtTagCompound.getCompoundTag("spawns");

        Iterator it = nbt.getKeySet().iterator();

        while (it.hasNext()) {
            String tagName = (String) it.next();
            NBTTagCompound nbtEntry = nbt.getCompoundTag(tagName);

            InvasionEntitySpawn spawn = new InvasionEntitySpawn();
            spawn.readNBT(nbtEntry);
            listSpawnables.add(spawn);
        }

        System.out.println("read done");
    }

    public void writeNBT(NBTTagCompound nbtTagCompound) {
        System.out.println("write");
        nbtTagCompound.setInteger("val", val);

        NBTTagCompound nbt = new NBTTagCompound();
        for (int i = 0; i < listSpawnables.size(); i++) {
            NBTTagCompound nbtEntry = new NBTTagCompound();
            InvasionEntitySpawn spawn = listSpawnables.get(i);
            spawn.writeNBT(nbtEntry);
            nbt.setTag("spawn_" + i, nbtEntry);
        }
        nbtTagCompound.setTag("spawns", nbt);
    }

}
