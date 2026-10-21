package com.example.brokenbones;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod(BrokenBonesMod.MODID)
public class BrokenBonesMod {
    public static final String MODID = "brokenbones";
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public BrokenBonesMod() {
        CHANNEL.registerMessage(0, SyncPacket.class,
                SyncPacket::encode, SyncPacket::decode, SyncPacket::handle);
    }
}
