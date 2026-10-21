package com.example.brokenbones;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BrokenBonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientHud {

    private static final long SHOW_MS = 3000;   // jak dlouho je vidět zelené +N

    private static long total = 0;
    private static int added = 0;
    private static long addedTime = 0;

    public static void update(long newTotal, int newAdded) {
        total = newTotal;
        if (newAdded > 0) {
            added = newAdded;
            addedTime = System.currentTimeMillis();
        }
    }

    @SubscribeEvent
    public static void registerOverlay(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("bones", (gui, g, partialTick, width, height) -> render(g));
    }

    private static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        Font f = mc.font;

        int x = 6, y = 6;
        String num = Long.toString(total);
        Component title = Component.translatable("hud.brokenbones.title");

        float numScale = 1.5f;
        float addScale = 0.75f;
        int numW = (int) (f.width(num) * numScale);

        long age = System.currentTimeMillis() - addedTime;
        boolean showAdded = added > 0 && age < SHOW_MS;
        String addStr = "+" + added;
        int addW = showAdded ? (int) (f.width(addStr) * addScale) + 4 : 0;

        int textW = Math.max(f.width(title), numW + addW);
        int w = 28 + textW + 8;
        int h = 34;

        // panel
        g.fill(x, y, x + w, y + h, 0xCC101010);
        g.renderOutline(x, y, w, h, 0xFF5A5A5A);

        // ikona kosti
        g.renderItem(new ItemStack(Items.BONE), x + 6, y + 9);

        // nadpis
        g.drawString(f, title, x + 28, y + 5, 0xB0B0B0, false);

        // číslo
        g.pose().pushPose();
        g.pose().translate(x + 28, y + 17, 0);
        g.pose().scale(numScale, numScale, 1f);
        g.drawString(f, num, 0, 0, 0xFFFFFF, false);
        g.pose().popPose();

        // zelený text s přičtenými kostmi (s odeznívající průhledností)
        if (showAdded) {
            float fade = age < SHOW_MS - 800 ? 1f : (SHOW_MS - age) / 800f;
            int alpha = Math.max(5, (int) (255 * fade));
            int color = (alpha << 24) | 0x55FF55;
            g.pose().pushPose();
            g.pose().translate(x + 28 + numW + 4, y + 19, 0);
            g.pose().scale(addScale, addScale, 1f);
            g.drawString(f, addStr, 0, 0, color, false);
            g.pose().popPose();
        }
    }
}
