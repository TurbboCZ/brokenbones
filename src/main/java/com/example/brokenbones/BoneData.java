package com.example.brokenbones;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

/** Data se ukládají do persistent dat hráče (přežijí restart i smrt). */
public class BoneData {
    private static CompoundTag tag(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG, 10)) {
            root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(BrokenBonesMod.MODID, 10)) {
            persisted.put(BrokenBonesMod.MODID, new CompoundTag());
        }
        return persisted.getCompound(BrokenBonesMod.MODID);
    }

    public static long getBones(Player p) {
        return tag(p).getLong("bones");
    }

    public static int getPerHit(Player p) {
        CompoundTag t = tag(p);
        return t.contains("perHit") ? t.getInt("perHit") : 1;
    }

    public static void setPerHit(Player p, int value) {
        tag(p).putInt("perHit", value);
    }

    public static void addBones(ServerPlayer p, int amount) {
        tag(p).putLong("bones", getBones(p) + amount);
        sync(p, amount);
    }

    public static void removeBones(ServerPlayer p, int amount) {
        tag(p).putLong("bones", Math.max(0L, getBones(p) - amount));
        sync(p, 0);
    }

    public static void sync(ServerPlayer p, int added) {
        BrokenBonesMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                new SyncPacket(getBones(p), added));
    }
}
