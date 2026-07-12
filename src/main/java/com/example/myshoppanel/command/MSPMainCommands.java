package com.example.myshoppanel.command;

import com.example.myshoppanel.shop.MainMenuConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Map;

/**
 * /mspMain 指令 — 控制主菜单按钮的显示/隐藏。
 * 用法：
 *   /mspMain <button> on/off  开关指定按钮
 *   /mspMain list             查看所有按钮状态
 */
public class MSPMainCommands {

    private static final String[] VALID_BUTTONS = {"player_market", "admin_shop", "warehouse", "history"};

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mspMain")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("list")
                        .executes(MSPMainCommands::listStatus)
                )
                .then(Commands.argument("button", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (String b : VALID_BUTTONS) builder.suggest(b);
                            return builder.buildFuture();
                        })
                        .then(Commands.literal("on")
                                .executes(ctx -> setButton(ctx, true))
                        )
                        .then(Commands.literal("off")
                                .executes(ctx -> setButton(ctx, false))
                        )
                )
        );
    }

    private static int setButton(CommandContext<CommandSourceStack> ctx, boolean enabled) {
        String button = StringArgumentType.getString(ctx, "button");
        boolean valid = false;
        for (String b : VALID_BUTTONS) {
            if (b.equals(button)) { valid = true; break; }
        }
        if (!valid) {
            ctx.getSource().sendFailure(Component.literal("§c未知按钮: " + button + "。可用: " + String.join(", ", VALID_BUTTONS)));
            return 0;
        }
        MainMenuConfig.setEnabled(button, enabled);
        String label = getButtonLabel(button);
        ctx.getSource().sendSuccess(() -> Component.literal("§a主菜单按钮 '" + label + "' 已" + (enabled ? "§a显示" : "§c隐藏")), true);
        return 1;
    }

    private static int listStatus(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal("§e===== 主菜单按钮状态 ====="), false);
        Map<String, Boolean> all = MainMenuConfig.getAll();
        for (Map.Entry<String, Boolean> entry : all.entrySet()) {
            String label = getButtonLabel(entry.getKey());
            String status = entry.getValue() ? "§a显示" : "§c隐藏";
            ctx.getSource().sendSuccess(() -> Component.literal("  " + label + ": " + status), false);
        }
        return 1;
    }

    private static String getButtonLabel(String id) {
        return switch (id) {
            case "player_market" -> "玩家市场";
            case "admin_shop" -> "世界商店";
            case "warehouse" -> "冗余仓库";
            case "history" -> "交易记录";
            default -> id;
        };
    }
}