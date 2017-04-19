package com.corosus.inv.capabilities;

import CoroUtil.difficulty.data.spawns.DataActionMobSpawns;
import CoroUtil.difficulty.data.spawns.DataMobSpawnsTemplate;
import com.corosus.inv.InvasionEntitySpawn;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Created by Corosus on 3/4/2017.
 */
public class PlayerDataInstance {

    List<InvasionEntitySpawn> listSpawnables = new ArrayList<>();
    private EntityPlayer player;

    public boolean dataPlayerInvasionActive;
    public boolean dataPlayerInvasionWarned;
    public long dataCreatureLastPathWithDelay;

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
        //resetInvasion();

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

    public InvasionEntitySpawn getRandomEntityClassToSpawn() {
        List<InvasionEntitySpawn> listSpawnablesTry = new ArrayList<>();

        //filter out ones that are used up
        for (InvasionEntitySpawn spawns : listSpawnables) {
            if (spawns.spawnCountCurrent < spawns.spawnProfile.count && spawns.spawnProfile.entities.size() > 0) {
                listSpawnablesTry.add(spawns);
            }
        }

        Random random = new Random();
        //chose random spawn profile and increment
        //InvasionEntitySpawn spawns = listSpawnablesTry.get(random.nextInt(listSpawnablesTry.size()));

        //TODO: reorder code logic, outside this, spawn could fail so we wouldnt want to increment this!
        //spawns.spawnCountCurrent++; FIX ^

        //return spawns.spawnProfile.entities.get(random.nextInt(spawns.spawnProfile.entities.size()));

        if (listSpawnablesTry.size() > 0) {
            return listSpawnablesTry.get(random.nextInt(listSpawnablesTry.size()));
        } else {
            System.out.println("nothing to spawn!");
            //return new InvasionEntitySpawn();
            return null;
        }


    }

    public void readNBT(NBTTagCompound nbtTagCompound) {

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
