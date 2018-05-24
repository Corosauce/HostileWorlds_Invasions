package com.corosus.inv;

import CoroUtil.difficulty.data.spawns.DataActionMobSpawns;
import CoroUtil.difficulty.data.DeserializerAllJson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Runtime instance of DataActionMobSpawns
 *
 * Created by Corosus on 3/4/2017.
 */
public class InvasionEntitySpawn {

    public DataActionMobSpawns spawnProfile;
    public int spawnCountCurrent;

    /*public List<String> listEntityNames;

    public int spawnCountMax;
    public List<DataCmod> listCmods;*/

    public void clear() {
        spawnProfile.cmods.clear();
        spawnProfile.entities.clear();
    }

    public void readNBT(NBTTagCompound nbtTagCompound) {
        spawnCountCurrent = nbtTagCompound.getInteger("spawnCountCurrent");
        //spawnProfile = (new Gson()).fromJson(nbtTagCompound.getString("json"), DataActionMobSpawns.class);
        JsonObject json = (new JsonParser()).parse(nbtTagCompound.getString("json")).getAsJsonObject();
        try {
            spawnProfile = DeserializerAllJson.deserializeSpawns(json);
        } catch (Exception ex) {
            ex.printStackTrace();
        }


    }

    public void writeNBT(NBTTagCompound nbtTagCompound) {
        nbtTagCompound.setInteger("spawnCountCurrent", spawnCountCurrent);
        //nbtTagCompound.setString("json", (new Gson()).toJson(spawnProfile, DataActionMobSpawns.class));
        nbtTagCompound.setString("json", DeserializerAllJson.serializeSpawns(spawnProfile).toString());
    }

    public String toString(boolean detailed) {
        String str = "spawnCountCurrent: " + spawnCountCurrent + ", " + super.toString();
        return str;
    }

    @Override
    public String toString() {
        String str = "spawnCountCurrent: " + spawnCountCurrent + " of total " + spawnProfile.count +
                " * difficulty * " + spawnProfile.count_difficulty_multiplier + " maxing at " + spawnProfile.count_max +
                ", choosing from " + spawnProfile.entities.size() + " entities";
        return str;
    }
}
