package com.example.brokenbones;

import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BrokenBonesMod.MODID)
public class CommonEvents {

    private static final int MAX = 99999999;

    /** Hit = útok od entity (mob, hráč, projektil...). Pád, oheň, hlad apod. se nepočítají. */
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer player)) return;

        DamageSource src = e.getSource();
        if (src.getEntity() == null && src.getDirectEntity() == null) return;
        if (player.getAbilities().invulnerable) return;       // kreativ
        if (player.invulnerableTime > 10) return;             // neprobíhající "i-frames" = jeden hit jednou

        int perHit = BoneData.getPerHit(player);
        if (perHit <= 0) return;
        BoneData.addBones(player, perHit);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) BoneData.sync(p, 0);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) BoneData.sync(p, 0);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) BoneData.sync(p, 0);
    }

    /** Po smrti zachovat počet kostí. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        CompoundTag old = e.getOriginal().getPersistentData();
        if (old.contains(Player.PERSISTED_NBT_TAG, 10)) {
            e.getEntity().getPersistentData().put(Player.PERSISTED_NBT_TAG,
                    old.getCompound(Player.PERSISTED_NBT_TAG).copy());
        }
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) {
        // /bonesperhit <číslo> - kolik kostí se přičte za 1 hit
        e.getDispatcher().register(Commands.literal("bonesperhit")
                .then(Commands.argument("amount", IntegerArgumentType.integer(0, MAX))
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                            BoneData.setPerHit(p, amount);
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("Za každý hit se nyní přičte kostí: " + amount), false);
                            return 1;
                        })));

        // /addbones <číslo> - hned přičte kosti
        e.getDispatcher().register(Commands.literal("addbones")
                .then(Commands.argument("amount", IntegerArgumentType.integer(0, MAX))
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                            BoneData.addBones(p, amount);
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("Přičteno zlomených kostí: " + amount), false);
                            return 1;
                        })));
    }
}
