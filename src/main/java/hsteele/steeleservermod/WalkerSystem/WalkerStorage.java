package hsteele.steeleservermod.WalkerSystem;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;


public class WalkerStorage {

    public static void register() {

        UseItemCallback.EVENT.register((player, world, hand) -> {

            if (isHoldingSpawner(player)) {

                ServerPlayer p = world.getServer().getPlayerList().getPlayer(player.getUUID());
                if (p == null) {
                    return InteractionResult.PASS;
                }

                HitResult result = player.pick(10, 1.0f, false);
                Vec3 pos = result.getLocation();

                List<Double> segmentLengths = new ArrayList<>();

                Walker walker = new Walker(pos, p, world.getServer().getLevel(world.dimension()));

                segmentLengths.add(0.8);
                segmentLengths.add(1.6);
                segmentLengths.add(1.4);
                segmentLengths.add(1.0);

                for (int i = 0; i < 6; i++) {
                    walker.addLeg(new Leg(pos, segmentLengths, world));
                }

                WalkerStorage.SHARED.addWalker(walker);

                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        });

        ServerTickEvents.START_LEVEL_TICK.register(world -> {
            WalkerStorage.SHARED.tick();

            List<ServerPlayer> players = world.players();
            for (ServerPlayer player: players) {
                if (isHoldingSpawner(player)) {
                    Vec3 pos = player.pick(10, 1.0f, false).getLocation();
                    world.sendParticles(ParticleTypes.END_ROD, pos.x(), pos.y() + 0.1, pos.z(), 1, 0, 0, 0, 0);
                }
            }
        });

        ServerLifecycleEvents.SERVER_STOPPED.register((server) -> {
            WalkerStorage.SHARED.killAll();
        });
    }

    public static LiteralArgumentBuilder<CommandSourceStack> registerCommand() {

        LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> walkerCommand = Commands.literal("walker")
                .requires(Commands.hasPermission(Commands.LEVEL_OWNERS));

        walkerCommand.then(Commands.literal("killAll")
                .executes(WalkerStorage::killAll)
        );

        return walkerCommand;
    }

    private static int killAll(CommandContext<CommandSourceStack> context) {
        WalkerStorage.SHARED.killAll();
        context.getSource().sendSuccess(
                () -> Component.literal("Killed all walkers"),
                true
        );
        return 1;
    }

    private void killAll() {
        for (int i = this.walkers.size() - 1; i >= 0; i--) {
            Walker walker = this.walkers.get(i);
            walker.kill();
        }
    }

    private static boolean isHoldingSpawner(Player player) {
        if (player.getItemInHand(player.getUsedItemHand()).getItem() != Items.AMETHYST_SHARD) { return false; }
        Component name = player.getItemInHand(player.getUsedItemHand()).getCustomName();
        if (name == null) { return false; }
        return name.getString().equals("Walker");
    }

    public static WalkerStorage SHARED = new WalkerStorage();

    private final List<Walker> walkers = new ArrayList<>();

    public void addWalker(Walker w) {
        walkers.add(w);
    }
    public void removeWalker(Walker w) {
        walkers.remove(w);
    }

    public void tick() {
        for (int i = 0; i < walkers.size(); i++) {
            walkers.get(i).tick();
        }
    }


}
