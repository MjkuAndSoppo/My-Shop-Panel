package com.example.myshoppanel.economy;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 测试用：模拟玩家不在线状态。
 * /MSPtest OnlineMode T/F 控制。
 * 仅服务端使用，不持久化。
 */
public class SimulateOfflineData {
    private static final Set<UUID> simulatedOffline = new HashSet<>();

    public static boolean isSimulatedOffline(UUID playerUUID) {
        return simulatedOffline.contains(playerUUID);
    }

    public static void setSimulatedOffline(UUID playerUUID, boolean offline) {
        if (offline) {
            simulatedOffline.add(playerUUID);
        } else {
            simulatedOffline.remove(playerUUID);
        }
    }
}
