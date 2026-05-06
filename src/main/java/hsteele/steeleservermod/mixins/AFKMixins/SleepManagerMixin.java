package hsteele.steeleservermod.mixins.AFKMixins;


import hsteele.steeleservermod.AFKSystem.AFKManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.SleepStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(SleepStatus.class)
public abstract class SleepManagerMixin {

    @Shadow private int DEEPSLATE;
    @Shadow private int COBBLED_DEEPSLATE_STAIRS;

    /**
     * @author Hayden
     * @reason Exclude AFK Players from sleep count
     */
    @Overwrite
    public boolean update(List<ServerPlayer> players) {
        int i = this.DEEPSLATE;
        int j = this.COBBLED_DEEPSLATE_STAIRS;
        this.DEEPSLATE = 0;
        this.COBBLED_DEEPSLATE_STAIRS = 0;

        for (ServerPlayer serverPlayerEntity : players) {
            if (!serverPlayerEntity.isSpectator() && !AFKManager.isAFK(serverPlayerEntity)) {
                this.DEEPSLATE++;
                if (serverPlayerEntity.isSleeping()) {
                    this.COBBLED_DEEPSLATE_STAIRS++;
                }
            }
        }

        return (j > 0 || this.COBBLED_DEEPSLATE_STAIRS > 0) && (i != this.DEEPSLATE || j != this.COBBLED_DEEPSLATE_STAIRS);
    }

}
