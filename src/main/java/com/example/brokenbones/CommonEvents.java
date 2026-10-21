package com.example.brokenbones;

import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BrokenBonesMod.MODID)
public class CommonEvents {

    private static final int MAX = 99999999;

    /**
     * Spustí se při jakémkoli poškození hráče (mob, pád, oheň, výbuch, hlad...), tedy přesně když
     * zazní zvuk zranění. Počítá se i když hráči díky absorpci/brnění neubydou srdíčka.
     */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer player)) return;

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

        // /removebones <číslo> - odebere kosti (nejméně na 0)
        e.getDispatcher().register(Commands.literal("removebones")
                .then(Commands.argument("amount", IntegerArgumentType.integer(0, MAX))
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                            BoneData.removeBones(p, amount);
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("Odebráno zlomených kostí: " + amount
                                            + " (nyní: " + BoneData.getBones(p) + ")"), false);
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
