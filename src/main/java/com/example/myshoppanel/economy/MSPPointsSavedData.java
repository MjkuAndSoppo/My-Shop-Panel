package com.example.myshoppanel.economy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 存储玩家 M.S.P.Points 点数数据。
 * 绑定主世界，以 <世界文件夹>/data/msp_points.dat 形式持久化。
 * 支持小数及负数（double 精度）。
 */
public class MSPPointsSavedData extends SavedData {

    private static final String DATA_NAME = "msp_points";
    private final Map<UUID, Double> pointsMap = new HashMap<>();

    // ========== 工厂方法 ==========

    public static MSPPointsSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                MSPPointsSavedData::load,
                MSPPointsSavedData::new,
                DATA_NAME
        );
    }

    // ========== 读取 / 写入 ==========

    public static MSPPointsSavedData load(CompoundTag tag) {
        MSPPointsSavedData data = new MSPPointsSavedData();
        ListTag list = tag.getList("Points", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            UUID uuid = entry.getUUID("UUID");
            double points = entry.getDouble("Points");
            data.pointsMap.put(uuid, points);
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Double> entry : pointsMap.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID("UUID", entry.getKey());
            entryTag.putDouble("Points", entry.getValue());
            list.add(entryTag);
        }
        tag.put("Points", list);
        return tag;
    }

    // ========== 业务方法 ==========

    public double getPoints(UUID playerUUID) {
        return pointsMap.getOrDefault(playerUUID, 0.0);
    }

    public void setPoints(UUID playerUUID, double amount) {
        pointsMap.put(playerUUID, amount);
        setDirty();
    }

    public double addPoints(UUID playerUUID, double amount) {
        double current = getPoints(playerUUID);
        double newBalance = current + amount;
        pointsMap.put(playerUUID, newBalance);
        setDirty();
        return newBalance;
    }

    public double cutPoints(UUID playerUUID, double amount) {
        return addPoints(playerUUID, -amount);
    }

    public Map<UUID, Double> getAllPoints() {
        return new HashMap<>(pointsMap);
    }
}
