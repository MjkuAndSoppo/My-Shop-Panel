package com.example.myshoppanel.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.lang.reflect.Type;
import java.util.*;

/**
 * 交易历史持久化。每个玩家最多保留 200 条记录。
 */
public class TransactionHistoryData extends SavedData {

    private static final String DATA_NAME = "msp_transaction_history";
    private static final int MAX_RECORDS = 200;
    private static final Gson GSON = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation().create();

    private final Map<UUID, List<TransactionRecord>> records = new HashMap<>();

    public static TransactionHistoryData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                TransactionHistoryData::load,
                TransactionHistoryData::new,
                DATA_NAME
        );
    }

    public void addRecord(UUID playerUUID, TransactionRecord record) {
        List<TransactionRecord> list = records.computeIfAbsent(playerUUID, k -> new ArrayList<>());
        list.add(0, record); // 最新在前
        while (list.size() > MAX_RECORDS) {
            list.remove(list.size() - 1);
        }
        setDirty();
    }

    public List<TransactionRecord> getRecords(UUID playerUUID) {
        return records.getOrDefault(playerUUID, Collections.emptyList());
    }

    public void clearRecords(UUID playerUUID) {
        records.remove(playerUUID);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        for (Map.Entry<UUID, List<TransactionRecord>> entry : records.entrySet()) {
            tag.putString(entry.getKey().toString(), GSON.toJson(entry.getValue()));
        }
        tag.putInt("playerCount", records.size());
        return tag;
    }

    public static TransactionHistoryData load(CompoundTag tag) {
        TransactionHistoryData data = new TransactionHistoryData();
        if (tag.contains("playerCount")) {
            int count = tag.getInt("playerCount");
            Type listType = new TypeToken<List<TransactionRecord>>(){}.getType();
            for (String key : tag.getAllKeys()) {
                if (key.equals("playerCount")) continue;
                try {
                    UUID uuid = UUID.fromString(key);
                    List<TransactionRecord> list = GSON.fromJson(tag.getString(key), listType);
                    if (list != null) {
                        data.records.put(uuid, list);
                    }
                } catch (Exception ignored) {}
            }
        }
        return data;
    }
}