package com.example.brokenbones;

import java.util.Collection;
import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
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

    // ---------------------------------------------------------------- příkazy

    private static String names(Collection<ServerPlayer> players) {
        if (players.size() == 1) return players.iterator().next().getGameProfile().getName();
        return players.size() + " hráčů";
    }

    private static int doAdd(CommandSourceStack src, Collection<ServerPlayer> players, int amount) {
        for (ServerPlayer p : players) BoneData.addBones(p, amount);
        src.sendSuccess(() -> Component.literal("Přičteno zlomených kostí: " + amount
                + " (" + names(players) + ")"), false);
        return players.size();
    }

    private static int doRemove(CommandSourceStack src, Collection<ServerPlayer> players, int amount) {
        for (ServerPlayer p : players) BoneData.removeBones(p, amount);
        src.sendSuccess(() -> Component.literal("Odebráno zlomených kostí: " + amount
                + " (" + names(players) + ")"), false);
        return players.size();
    }

    private static int doPerHit(CommandSourceStack src, Collection<ServerPlayer> players, int amount) {
        for (ServerPlayer p : players) BoneData.setPerHit(p, amount);
        src.sendSuccess(() -> Component.literal("Za každý hit se nyní přičte kostí: " + amount
                + " (" + names(players) + ")"), false);
        return players.size();
    }

    private interface Action {
        int run(CommandSourceStack src, Collection<ServerPlayer> players, int amount);
    }

    /**
     * /<name> <číslo>                  -> pro sebe (bez omezení)
     * /<name> <hráči> <číslo>          -> pro ostatní (jen operátoři, level 2)
     */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> build(
            String name, Action action) {
        return Commands.literal(name)
                .then(Commands.argument("amount", IntegerArgumentType.integer(0, MAX))
                        .executes(ctx -> action.run(ctx.getSource(),
                                List.of(ctx.getSource().getPlayerOrException()),
                                IntegerArgumentType.getInteger(ctx, "amount"))))
                .then(Commands.argument("players", EntityArgument.players())
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, MAX))
                                .executes(ctx -> action.run(ctx.getSource(),
                                        EntityArgument.getPlayers(ctx, "players"),
                                        IntegerArgumentType.getInteger(ctx, "amount")))));
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) {
        e.getDispatcher().register(build("addbones", CommonEvents::doAdd));
        e.getDispatcher().register(build("removebones", CommonEvents::doRemove));
        e.getDispatcher().register(build("bonesperhit", CommonEvents::doPerHit));
    }
}
