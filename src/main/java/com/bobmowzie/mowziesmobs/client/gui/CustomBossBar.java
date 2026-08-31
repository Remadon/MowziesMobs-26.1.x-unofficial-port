package com.bobmowzie.mowziesmobs.client.gui;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.server.entity.EntityHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.BossEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * PORTING NOTE: `net.minecraft.client.gui.GuiGraphics` renamed to `GuiGraphicsExtractor` as part of the broader
 * "render state extraction" overhaul (see PORTING_NOTES.md) - `CustomizeGuiOverlayEvent#getGuiGraphics()` (verified
 * via javap against the NeoForge 26.1.2.95 universal jar, no sources jar was available) now returns
 * `GuiGraphicsExtractor` directly, confirming this event itself was updated for the new architecture too.
 * `drawString(...)` -> `text(...)`; `blit(Identifier, x, y, u, v, w, h, texW, texH)` -> `blit(RenderPipeline,
 * Identifier, x, y, u, v, w, h, texW, texH)` (u/v now floats, and a `RenderPipeline` - `RenderPipelines.GUI_TEXTURED`
 * for a plain textured quad - is now required as the first argument; see `ContainerScreen.java` in the vanilla
 * source tree for a concrete example of the new `blit` call shape). The old `RenderSystem.setShaderColor`/
 * `setShaderTexture` calls before each blit are no longer meaningful/necessary in the new deferred extraction model
 * (the blit call itself carries the texture reference and is resolved later during actual GPU submission) and have
 * been removed.
 */
public class CustomBossBar {
    public static Map<Identifier, CustomBossBar> customBossBars = new HashMap<>();
    static {
        customBossBars.put(BuiltInRegistries.ENTITY_TYPE.getKey(EntityHandler.UMVUTHI.get()), new CustomBossBar(
                MMCommon.resource("textures/gui/boss_bar/umvuthi_bar_base.png"),
                MMCommon.resource("textures/gui/boss_bar/umvuthi_bar_overlay.png"),
                4, 8, 5, -5, -6, 256, 16, 25, ChatFormatting.GOLD));
        customBossBars.put(BuiltInRegistries.ENTITY_TYPE.getKey(EntityHandler.FROSTMAW.get()), new CustomBossBar(
                MMCommon.resource("textures/gui/boss_bar/frostmaw_bar_base.png"),
                MMCommon.resource("textures/gui/boss_bar/frostmaw_bar_overlay.png"),
                10, 32, 2, -4, -3, 256, 32, 25, ChatFormatting.WHITE));
        customBossBars.put(BuiltInRegistries.ENTITY_TYPE.getKey(EntityHandler.WROUGHTNAUT.get()), new CustomBossBar(
                MMCommon.resource("textures/gui/boss_bar/wroughtnaut_bar_base.png"),
                MMCommon.resource("textures/gui/boss_bar/wroughtnaut_bar_overlay.png"),
                4, 8, 5, -5, -6, 256, 16, 25, ChatFormatting.RED));
    }

    private final Identifier baseTexture;
    private final Identifier overlayTexture;
    private final boolean hasOverlay;

    private final int baseHeight;
    private final int baseTextureHeight;
    private final int baseOffsetY;
    private final int overlayOffsetX;
    private final int overlayOffsetY;
    private final int overlayWidth;
    private final int overlayHeight;

    private final int verticalIncrement;

    private final ChatFormatting textColor;

    public CustomBossBar(Identifier baseTexture, Identifier overlayTexture, int baseHeight, int baseTextureHeight, int baseOffsetY, int overlayOffsetX, int overlayOffsetY, int overlayWidth, int overlayHeight, int verticalIncrement, ChatFormatting textColor) {
        this.baseTexture = baseTexture;
        this.overlayTexture = overlayTexture;
        this.hasOverlay = overlayTexture != null;
        this.baseHeight = baseHeight;
        this.baseTextureHeight = baseTextureHeight;
        this.baseOffsetY = baseOffsetY;
        this.overlayOffsetX = overlayOffsetX;
        this.overlayOffsetY = overlayOffsetY;
        this.overlayWidth = overlayWidth;
        this.overlayHeight = overlayHeight;
        this.verticalIncrement = verticalIncrement;
        this.textColor = textColor;
    }

    public Identifier getBaseTexture() {
        return baseTexture;
    }

    public Identifier getOverlayTexture() {
        return overlayTexture;
    }

    public boolean hasOverlay() {
        return hasOverlay;
    }

    public int getBaseHeight() {
        return baseHeight;
    }

    public int getBaseTextureHeight() {
        return baseTextureHeight;
    }

    public int getBaseOffsetY() {
        return baseOffsetY;
    }

    public int getOverlayOffsetX() {
        return overlayOffsetX;
    }

    public int getOverlayOffsetY() {
        return overlayOffsetY;
    }

    public int getOverlayWidth() {
        return overlayWidth;
    }

    public int getOverlayHeight() {
        return overlayHeight;
    }

    public int getVerticalIncrement() {
        return verticalIncrement;
    }

    public ChatFormatting getTextColor() {
        return textColor;
    }

    public void renderBossBar(CustomizeGuiOverlayEvent.BossEventProgress event, int y) {
        int baseYOffset = getBaseOffsetY();

        GuiGraphicsExtractor guiGraphics = event.getGuiGraphics();
        int i = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int j = y - 9;
        int k = i / 2 - 91;
        net.minecraft.util.profiling.Profiler.get().push("customBossBarBase");

        drawBar(guiGraphics, event.getX() + 1, y + baseYOffset, event.getBossEvent());
        Component component = event.getBossEvent().getName().copy().withStyle(getTextColor());
        net.minecraft.util.profiling.Profiler.get().pop();

        int l = Minecraft.getInstance().font.width(component);
        int i1 = i / 2 - l / 2;
        int j1 = j;
        // PORTING NOTE: was 16777215 (0x00FFFFFF) - opaque white with the alpha byte left at 0. NeoForge 26.1.2's
        // GuiGraphicsExtractor#text guards on ARGB.alpha(color) != 0 and silently skips queuing the draw entirely
        // when it's 0, so this text never actually rendered. -1 (0xFFFFFFFF) is the correct fully-opaque white,
        // matching what vanilla's own BossHealthOverlay uses for the exact same boss-name text.
        guiGraphics.text(Minecraft.getInstance().font, component, i1, j1, -1);

        if (hasOverlay()) {
            net.minecraft.util.profiling.Profiler.get().push("customBossBarOverlay");
            event.getGuiGraphics().blit(RenderPipelines.GUI_TEXTURED, getOverlayTexture(), event.getX() + 1 + getOverlayOffsetX(), y + getOverlayOffsetY() + baseYOffset, 0.0F, 0.0F, getOverlayWidth(), getOverlayHeight(), getOverlayWidth(), getOverlayHeight());
            net.minecraft.util.profiling.Profiler.get().pop();
        }
    }

    private void drawBar(GuiGraphicsExtractor guiGraphics, int x, int y, BossEvent event) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, getBaseTexture(), x, y, 0.0F, 0.0F, 182, getBaseHeight(), 256, getBaseTextureHeight());
        int i = (int)(event.getProgress() * 183.0F);
        if (i > 0) {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, getBaseTexture(), x, y, 0.0F, (float) getBaseHeight(), i, getBaseHeight(), 256, getBaseTextureHeight());
        }
    }
}
