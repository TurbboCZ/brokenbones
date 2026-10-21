package com.example.brokenbones;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server -> klient: celkový počet kostí + kolik se právě přičetlo (0 = nezobrazovat zelený text). */
public class SyncPacket {
    public final long total;
    public final int added;

    public SyncPacket(long total, int added) {
        this.total = total;
        this.added = added;
    }

    public static void encode(SyncPacket p, FriendlyByteBuf buf) {
        buf.writeLong(p.total);
        buf.writeInt(p.added);
    }

    public static SyncPacket decode(FriendlyByteBuf buf) {
        return new SyncPacket(buf.readLong(), buf.readInt());
    }

    public static void handle(SyncPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHud.update(p.total, p.added)));
        ctx.get().setPacketHandled(true);
    }
}
