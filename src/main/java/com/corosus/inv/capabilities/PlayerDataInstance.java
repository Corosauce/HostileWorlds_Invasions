package com.corosus.inv.capabilities;

import CoroUtil.difficulty.data.spawns.DataActionMobSpawns;
import CoroUtil.difficulty.data.spawns.DataMobSpawnsTemplate;
import CoroUtil.forge.CULog;
import CoroUtil.util.CoroUtilEntity;
import com.corosus.inv.InvLog;
import com.corosus.inv.InvasionEntitySpawn;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class PlayerDataInstance {

    private List<InvasionEntitySpawn> listSpawnables = new ArrayList<>();
    private EntityPlayer player;

    private float difficultyForInvasion = 0;

    //used to fix problem of invasion warning triggering again after invasion stops, during that small window where it counts as "daylight" in 2 separate spots on the same time window
    public boolean dataPlayerInvasionHappenedThisDay;
    public boolean dataPlayerInvasionActive;
    public boolean dataPlayerInvasionWarned;
    //when trying dark areas for a long time fails, fallsback to spawning in light
    public boolean allowSpawnInLitAreas = false;

    private List<Class> listSpawnablesCached = new ArrayList<>();

    //persistent data that should never be cleared without specific commands run
    public int lastWaveNumber = 0;

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
        InvLog.dbg("init invasion spawn template to runtime data");
        if (profile.spawns.size() > 0) {
            for (DataActionMobSpawns spawns : profile.spawns) {
                InvasionEntitySpawn newSpawn = new InvasionEntitySpawn();
                //we must clone the list, not use direct reference, or we will corrupt(blank out) the data later
                newSpawn.spawnProfile = spawns.copy();
                InvLog.dbg("adding spawns: " + newSpawn.toString(true));
                listSpawnables.add(newSpawn);
            }
        } else {
            InvLog.dbg("CRITICAL: there was no spawn data to setup!");
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
        InvLog.dbg("resetInvasion()");
        //we make a copy of this, but why clear anyways?
        for (InvasionEntitySpawn spawns : listSpawnables) {
            spawns.clear();
        }
        listSpawnables.clear();
        listSpawnablesCached.clear();
        allowSpawnInLitAreas = false;
    }

    public void resetPersistentData() {
        lastWaveNumber = 0;
    }

    public InvasionEntitySpawn getRandomEntityClassToSpawn() {
        List<InvasionEntitySpawn> listSpawnablesTry = new ArrayList<>();

        //filter out ones that are used up
        for (InvasionEntitySpawn spawns : listSpawnables) {
            if (spawns.spawnCountCurrent < spawns.spawnProfile.getMaxMobCountDynamic(difficultyForInvasion) && spawns.spawnProfile.entities.size() > 0) {
                listSpawnablesTry.add(spawns);
            }
        }

        Random random = new Random();

        if (listSpawnablesTry.size() > 0) {
            InvasionEntitySpawn spawn = listSpawnablesTry.get(random.nextInt(listSpawnablesTry.size()));
            InvLog.dbg("returning this to spawn in: " + spawn.toString());
            return spawn;
        } else {
            //this should be ok, happens when all the things that will spawn have spawned
            if (listSpawnables.size() <= 0) {
                InvLog.dbg("nothing to spawn and there was never anything to choose from, nothing to invade, this is bad?");
            } else {
                //fine, probably means everything already spawned
                //InvLog.dbg("all spawnables spawned in");
            }
            //System.out.println("nothing to spawn!");
            //return new InvasionEntitySpawn();
            return null;
        }


    }

    public List<Class> getSpawnableClasses() {
        if (listSpawnablesCached.size() == 0) {
            for (InvasionEntitySpawn spawns : listSpawnables) {
                for (String spawnable : spawns.spawnProfile.entities) {
                    Class classToSpawn = CoroUtilEntity.getClassFromRegistry(spawnable);
                    if (classToSpawn != null) {
                        if (!listSpawnablesCached.contains(classToSpawn)) {
                            listSpawnablesCached.add(classToSpawn);
                        }
                    }
                }
            }
        }
        return listSpawnablesCached;
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

        dataPlayerInvasionHappenedThisDay = nbtTagCompound.getBoolean("dataPlayerInvasionHappenedThisDay");
        dataPlayerInvasionActive = nbtTagCompound.getBoolean("dataPlayerInvasionActive");
        dataPlayerInvasionWarned = nbtTagCompound.getBoolean("dataPlayerInvasionWarned");
        allowSpawnInLitAreas = nbtTagCompound.getBoolean("allowSpawnInLitAreas");

        difficultyForInvasion = nbtTagCompound.getFloat("difficultyForInvasion");

        lastWaveNumber = nbtTagCompound.getInteger("lastWaveNumber");

        CULog.dbg("read done");
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

        nbtTagCompound.setBoolean("dataPlayerInvasionHappenedThisDay", dataPlayerInvasionHappenedThisDay);
        nbtTagCompound.setBoolean("dataPlayerInvasionActive", dataPlayerInvasionActive);
        nbtTagCompound.setBoolean("dataPlayerInvasionWarned", dataPlayerInvasionWarned);
        nbtTagCompound.setBoolean("allowSpawnInLitAreas", allowSpawnInLitAreas);

        nbtTagCompound.setFloat("difficultyForInvasion", difficultyForInvasion);

        nbtTagCompound.setInteger("lastWaveNumber", lastWaveNumber);
    }

    public float getDifficultyForInvasion() {
        return difficultyForInvasion;
    }

    public void setDifficultyForInvasion(float difficultyForInvasion) {
        this.difficultyForInvasion = difficultyForInvasion;
    }

}
