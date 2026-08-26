package com.bobmowzie.mowziesmobs.client.gui;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.server.entity.sculptor.EntitySculptor;
import com.bobmowzie.mowziesmobs.server.inventory.ContainerSculptorTrade;
import com.bobmowzie.mowziesmobs.server.inventory.InventorySculptor;
import com.bobmowzie.mowziesmobs.server.item.ItemHandler;
import com.bobmowzie.mowziesmobs.server.message.MessageSculptorTrade;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * PORTING NOTE (see PORTING_NOTES.md GuiGraphicsExtractor section and `client/gui/screens/inventory/ContainerScreen.java`
 * / `InventoryScreen.java` in the vanilla tree for the concrete "before/after" patterns this file follows):
 * - `renderBg(GuiGraphics, float, int, int)` -> `extractBackground(GuiGraphicsExtractor, int mouseX, int mouseY, float a)`
 *   (note the parameter ORDER changed too - partial tick moved to last).
 * - `renderLabels(GuiGraphics, int, int)` -> `extractLabels(GuiGraphicsExtractor, int, int)`.
 * - `render(GuiGraphics, int, int, float)` -> `extractRenderState(GuiGraphicsExtractor, int, int, float)`; the old
 *   explicit `this.renderTooltip(guiGraphics, mouseX, mouseY)` call is gone because
 *   `AbstractContainerScreen#extractRenderState` already calls `extractTooltip(...)` internally now.
 * - `GuiGraphics#blit(Identifier, ...)` -> `GuiGraphicsExtractor#blit(RenderPipeline, Identifier, ...)` (u/v are now
 *   floats); `drawString`->`text`; `renderItem`->`item`; `renderItemDecorations`->`itemDecorations`;
 *   `renderTooltip`->`setTooltipForNextFrame`; `renderComponentHoverEffect`->`componentHoverEffect`;
 *   `pose().pushPose()/popPose()`->`pose().pushMatrix()/popMatrix()` (the pose stack is now a 2D `Matrix3x2fStack`,
 *   no Z component - the old `.translate(0, 0, 100)` Z-layering hack has no equivalent and isn't needed any more,
 *   since draw ordering in the new deferred render-state list already determines layering).
 * - `InventoryScreen.renderEntityInInventoryFollowsMouse(...)` -> `InventoryScreen.extractEntityInInventoryFollowsMouse(...)`
 *   (same parameters, just renamed + GuiGraphicsExtractor).
 * - The old `RenderSystem.colorMask`/`setShader`/`setShaderColor` calls before the background blit are no longer
 *   meaningful in the new deferred pipeline (the blit call itself carries the RenderPipeline/texture) - removed.
 */
public final class GuiSculptorTrade extends AbstractContainerScreen<ContainerSculptorTrade> implements InventorySculptor.ChangeListener {
    private static final Identifier TEXTURE_TRADE = Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/gui/container/umvuthi_trade.png");

    private final EntitySculptor sculptor;
    private final InventorySculptor inventory;

    private final ItemStack output = new ItemStack(ItemHandler.EARTHREND_GAUNTLET.get());

    private Button beginButton;
    private boolean prevBlocked;

    public GuiSculptorTrade(ContainerSculptorTrade screenContainer, Inventory inv, Component titleIn) {
        super(screenContainer, inv, titleIn);
        this.sculptor = screenContainer.getSculptor();
        this.inventory = screenContainer.getInventorySculptor();
        inventory.addListener(this);
    }

    @Override
    protected void init() {
        super.init();
        String text = I18n.get("entity.mowziesmobs.sculptor.trade.button.text");
        beginButton = addRenderableWidget(Button.builder(Component.translatable(text), this::actionPerformed).pos(leftPos + 115, topPos + 52).size(56, 20).build());
        updateButton();
    }

    private void actionPerformed(Button button) {
    	if (button == beginButton) {
            ClientPacketDistributor.sendToServer(new MessageSculptorTrade(sculptor.getId()));
    	}
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int x, int y, float partialTicks) {
        super.extractBackground(guiGraphics, x, y, partialTicks);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE_TRADE, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, imageWidth, imageHeight);
        if (sculptor != null) {
            sculptor.renderingInGUI = true;
            // x and y values are chosen as the first and last pixel of the black (entity) box of the gui texture
            // The two x and y values determine the size for the 'GuiGraphicsExtractor#enableScissor' call (their middle point is also where the entity will be rendered)
            InventoryScreen.extractEntityInInventoryFollowsMouse(guiGraphics, leftPos + 8, topPos + 8, leftPos + 59, topPos + 69, 14, 0, x, y, sculptor);
            sculptor.renderingInGUI = false;
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int x, int y) {
        guiGraphics.text(font, title, (int) (imageWidth / 2f - font.width(title) / 2f) + 30, 6, 0x404040, false);
        guiGraphics.text(font, I18n.get("container.inventory"), 8, imageHeight - 96 + 2, 0x404040, false);
        if (sculptor != null) {
            if (sculptor.isTestObstructed()) {
                String blocked = I18n.get("entity.mowziesmobs.sculptor.trade.blocked");
                guiGraphics.text(font, blocked, (int) (imageWidth / 2f - font.width(blocked) / 2f) + 30, 42, 0x404040, false);
            }
            if (prevBlocked != sculptor.isTestObstructed()) {
                onChange(inventory);
            }
            prevBlocked = sculptor.isTestObstructed();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
        ItemStack inSlot = inventory.getItem(0);
        guiGraphics.pose().pushMatrix();

        guiGraphics.item(sculptor.getDesires(), leftPos + 68, topPos + 24);
        guiGraphics.itemDecorations(font, sculptor.getDesires(), leftPos + 68, topPos + 24);
        guiGraphics.item(output, leftPos + 134, topPos + 24);
        guiGraphics.itemDecorations(font, output, leftPos + 134, topPos + 24);
        if (isHovering(68, 24, 16, 16, mouseX, mouseY)) {
            guiGraphics.setTooltipForNextFrame(font, sculptor.getDesires(), mouseX, mouseY);
        } else if (isHovering(134, 24, 16, 16, mouseX, mouseY)) {
            guiGraphics.setTooltipForNextFrame(font, output, mouseX, mouseY);
        }

        if (beginButton.isMouseOver(mouseX, mouseY)) {
            guiGraphics.componentHoverEffect(font, getHoverText(), mouseX, mouseY);
        }

        guiGraphics.pose().popMatrix();
    }

    @Override
	public void onChange(Container inv) {
        beginButton.active = sculptor.doesItemSatisfyDesire(inv.getItem(0)) && !sculptor.isTestObstructed();
	}

    private void updateButton() {
        beginButton.setMessage(Component.translatable(I18n.get("entity.mowziesmobs.sculptor.trade.button.text")));
    }

    private Style getHoverText() {
        MutableComponent text = Component.translatable(I18n.get("entity.mowziesmobs.sculptor.trade.button.hover"));
        return text.getStyle().withHoverEvent(new HoverEvent.ShowText(text));
    }
}
