package com.bobmowzie.mowziesmobs.client.gui;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.server.entity.umvuthana.EntityUmvuthi;
import com.bobmowzie.mowziesmobs.server.inventory.ContainerUmvuthiTrade;
import com.bobmowzie.mowziesmobs.server.inventory.InventoryUmvuthi;
import com.bobmowzie.mowziesmobs.server.item.ItemHandler;
import com.bobmowzie.mowziesmobs.server.message.MessageUmvuthiTrade;
import net.minecraft.client.Minecraft;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * PORTING NOTE: see GuiSculptorTrade.java for the full explanation of the GuiGraphics -> GuiGraphicsExtractor /
 * renderBg->extractBackground / renderLabels->extractLabels / render->extractRenderState conversion pattern applied
 * here (same pattern, this file follows it identically).
 */
public final class GuiUmvuthiTrade extends AbstractContainerScreen<ContainerUmvuthiTrade> implements InventoryUmvuthi.ChangeListener {
    private static final Identifier TEXTURE_TRADE = Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/gui/container/umvuthi_trade.png");
    private static final Identifier TEXTURE_REPLENISH = Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/gui/container/umvuthi_replenish.png");

    private final EntityUmvuthi umvuthi;
    private final Player player;

    private final InventoryUmvuthi inventory;

    private final ItemStack output = new ItemStack(ItemHandler.GRANT_SUNS_BLESSING.get());

    private Button grantButton;

    private boolean hasTraded;

    public GuiUmvuthiTrade(ContainerUmvuthiTrade screenContainer, Inventory inv, Component titleIn) {
        super(screenContainer, inv, titleIn);
        this.umvuthi = screenContainer.getUmvuthi();
        this.player = inv.player;
        this.inventory = screenContainer.getInventoryUmvuthi();
        this.hasTraded = umvuthi.hasTradedWith(inv.player);
        inventory.addListener(this);
    }

    @Override
    protected void init() {
        super.init();
        String text = I18n.get(hasTraded ? "entity.mowziesmobs.umvuthi.replenish.button.text" : "entity.mowziesmobs.umvuthi.trade.button.text");
        grantButton = addRenderableWidget(Button.builder(Component.translatable(text), this::actionPerformed).width(204).pos(leftPos + 115, topPos + 52).size(56, 20).build());
        grantButton.active = hasTraded;
        updateButton();
    }

    protected void actionPerformed(Button button) {
    	if (button == grantButton) {
            hasTraded = true;
            updateButton();
            ClientPacketDistributor.sendToServer(new MessageUmvuthiTrade(umvuthi.getId()));
            if (!Minecraft.getInstance().isLocalServer()) {
                boolean satisfied = umvuthi.hasTradedWith(player);
                if (!satisfied) {
                    if (umvuthi.fulfillDesire(menu.getSlot(0))) {
                        umvuthi.rememberTrade(player);
                        menu.broadcastChanges();
                    }
                }
            }
    	}
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int x, int y, float partialTicks) {
        super.extractBackground(guiGraphics, x, y, partialTicks);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, hasTraded ? TEXTURE_REPLENISH : TEXTURE_TRADE, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, imageWidth, imageHeight);
        if (umvuthi != null) {
            umvuthi.renderingInGUI = true;
            // x and y values are chosen as the first and last pixel of the black (entity) box of the gui texture
            // The two x and y values determine the size for the 'GuiGraphicsExtractor#enableScissor' call (their middle point is also where the entity will be rendered)
            InventoryScreen.extractEntityInInventoryFollowsMouse(guiGraphics, leftPos + 8, topPos + 8, leftPos + 59, topPos + 69, 20, 0.25f, x, y, umvuthi);
            umvuthi.renderingInGUI = false;
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int x, int y) {
        String title = I18n.get("entity.mowziesmobs.umvuthi.trade");
        guiGraphics.text(font, title, (int) (imageWidth / 2f - font.width(title) / 2f) + 30, 6, 0x404040, false);
        guiGraphics.text(font, I18n.get("container.inventory"), 8, imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
        ItemStack inSlot = inventory.getItem(0);
        guiGraphics.pose().pushMatrix();

        if (hasTraded) {
            guiGraphics.item(output, leftPos + 106, topPos + 24);
            guiGraphics.itemDecorations(font, output, leftPos + 106, topPos + 24);
            if (isHovering(106, 24, 16, 16, mouseX, mouseY)) {
                guiGraphics.setTooltipForNextFrame(font, output, mouseX, mouseY);
            }
        }
        else {
            guiGraphics.item(umvuthi.getDesires(), leftPos + 68, topPos + 24);
            guiGraphics.itemDecorations(font, umvuthi.getDesires(), leftPos + 68, topPos + 24);
            guiGraphics.item(output, leftPos + 134, topPos + 24);
            guiGraphics.itemDecorations(font, output, leftPos + 134, topPos + 24);
            if (isHovering(68, 24, 16, 16, mouseX, mouseY)) {
                guiGraphics.setTooltipForNextFrame(font, umvuthi.getDesires(), mouseX, mouseY);
            } else if (isHovering(134, 24, 16, 16, mouseX, mouseY)) {
                guiGraphics.setTooltipForNextFrame(font, output, mouseX, mouseY);
            }
        }

        if (grantButton.isMouseOver(mouseX, mouseY)) {
            guiGraphics.componentHoverEffect(font, getHoverText(), mouseX, mouseY);
        }
        guiGraphics.pose().popMatrix();
    }

    @Override
	public void onChange(Container inv) {
        grantButton.active = hasTraded || umvuthi.doesItemSatisfyDesire(inv.getItem(0));
	}

    private void updateButton() {
        if (hasTraded) {
            grantButton.setMessage(Component.translatable(I18n.get("entity.mowziesmobs.umvuthi.replenish.button.text")));
            grantButton.setWidth(108);
            grantButton.setPosition(leftPos + 63, grantButton.getY());
        }
        else {
            grantButton.setMessage(Component.translatable(I18n.get("entity.mowziesmobs.umvuthi.trade.button.text")));
        }
    }

    private Style getHoverText() {
        MutableComponent text = Component.translatable(I18n.get(hasTraded ? "entity.mowziesmobs.umvuthi.replenish.button.hover" : "entity.mowziesmobs.umvuthi.trade.button.hover"));
        return text.getStyle().withHoverEvent(new HoverEvent.ShowText(text));
    }
}
