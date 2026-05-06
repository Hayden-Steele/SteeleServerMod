package hsteele.steeleservermod.AFKSystem;


import hsteele.steeleservermod.config.ConfigSystem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.*;

public class AFKManager {

    private static final Map<UUID, Long> lastActivity = new HashMap<>();
    private static final Set<UUID> afkPlayers = new HashSet<>();

    public static void register() {

        // Register AFK Manager at end of server tick
        ServerTickEvents.END_SERVER_TICK.register(AFKManager::tick);

        // Register player movement event with afk tick
        PlayerMovementTracker.register();

        // Register the movement listener
        PlayerMoveCallback.EVENT.register((player, from, to) -> {
            AFKManager.onPlayerActivity(player);
            return InteractionResult.PASS;
        });


        // Clear player from storage on join and disconnect
        // -> This stops the afk messages from appearing when the join / leave
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            AFKManager.forceRemoveAFK(player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            AFKManager.forceRemoveAFK(player);
        });
    }

    public static void setTimeoutMinutes(int minutes) {
        ConfigSystem.get().afkTimeoutMinutes = minutes;
        ConfigSystem.save();
    }

    public static void onPlayerActivity(ServerPlayer player) {
        lastActivity.put(player.getUUID(), System.currentTimeMillis());

        // Remove AFK status if they were AFK
        if (isAFK(player)) {
            removeAFK(player);
        }
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) { return; } // Only check once a second

        long now = System.currentTimeMillis();
        int AFK_TIMEOUT_MILLIS = ConfigSystem.get().afkTimeoutMinutes * 60 * 1000;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {

            UUID uuid = player.getUUID();
            long lastSeen = lastActivity.getOrDefault(uuid, now);
            long dt = now - lastSeen;

            if (!isAFK(player) && dt > AFK_TIMEOUT_MILLIS) {
                addAFK(player);
            }

            if (isAFK(player)) {
                player.sendSystemMessage(Component.literal(getMessageString(server.getTickCount())), true);
            }
        }
    }

    private static String getMessageString(int tick) {

        int size = 3;
        int t = tick / 20 % (size * 2);
        if (t > size) {
            t = size - (t - size);
        }

        return "-".repeat(t + 1) +
                " You are AFK " +
                "-".repeat(size - t + 1);
    }

    public static void addAFK(ServerPlayer player) {
        afkPlayers.add(player.getUUID());
        MinecraftServer server = player.level().getServer();
        server.getPlayerList().broadcastSystemMessage(
                Component.literal(player.getName().getString() + " is now AFK").withStyle(ChatFormatting.YELLOW),
                false
        );
        addAFKPrefix(player);
    }
    public static void removeAFK(ServerPlayer player) {
        afkPlayers.remove(player.getUUID());
        MinecraftServer server = player.level().getServer();
        server.getPlayerList().broadcastSystemMessage(
                Component.literal(player.getName().getString() + " is no longer AFK").withStyle(ChatFormatting.YELLOW),
                false
        );
        removeAFKPrefix(player);
    }

    public static boolean isAFK(ServerPlayer player) {
        return afkPlayers.contains(player.getUUID());
    }

    private static void forceRemoveAFK(ServerPlayer player) {
        afkPlayers.remove(player.getUUID());
        lastActivity.remove(player.getUUID());
        removeAFKPrefix(player);
    }


    private static void addAFKPrefix(ServerPlayer player) {
        Scoreboard scoreboard = player.level().getServer().getScoreboard();

        PlayerTeam team = scoreboard.getPlayerTeam("afk");
        // Create team if it does not exist
        if (team == null) {
            team = scoreboard.addPlayerTeam("afk");
            team.setPlayerPrefix(Component.literal("[AFK] ").withStyle(ChatFormatting.GRAY));
        }
        scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
    }
    private static void removeAFKPrefix(ServerPlayer player) {

        PlayerTeam playerteam = player.getTeam();
        if (playerteam == null) {
            return;
        }

        Scoreboard scoreboard = player.level().getServer().getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam("afk");
        if (team != null) {
            scoreboard.removePlayerFromTeam(player.getScoreboardName(), team);
        }
    }

}
