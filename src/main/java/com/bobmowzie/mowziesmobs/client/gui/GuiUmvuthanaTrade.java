package com.bobmowzie.mowziesmobs.client.gui;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.server.entity.umvuthana.EntityUmvuthanaMinion;
import com.bobmowzie.mowziesmobs.server.entity.umvuthana.trade.Trade;
import com.bobmowzie.mowziesmobs.server.inventory.ContainerUmvuthanaTrade;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * PORTING NOTE: see GuiSculptorTrade.java for the full explanation of the GuiGraphics -> GuiGraphicsExtractor /
 * renderBg->extractBackground / renderLabels->extractLabels / render->extractRenderState conversion pattern applied
 * here (same pattern, this file follows it identically).
 */
public final class GuiUmvuthanaTrade extends AbstractContainerScreen<ContainerUmvuthanaTrade> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/gui/container/umvuthana.png");

    private final EntityUmvuthanaMinion umvuthana;

    public GuiUmvuthanaTrade(ContainerUmvuthanaTrade screenContainer, Inventory inv, Component titleIn) {
        super(screenContainer, inv, titleIn);
        this.umvuthana = screenContainer.getUmvuthana();
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
//        if (barakoaya.getAnimation() == IAnimatedEntity.NO_ANIMATION) {
//            if (isHovering(13, 23, 8, 14, mouseX, mouseY)) {
//                barakoaya.setAnimation(EntityBarakoaVillager.ATTACK_ANIMATION);
//                barakoaya.setAnimationTick(3);
//            }
//        } TODO
        return super.mouseReleased(event);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int x, int y, float partialTicks) {
        super.extractBackground(guiGraphics, x, y, partialTicks);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, imageWidth, imageHeight);
        if (umvuthana != null) {
            umvuthana.renderingInGUI = true;
            // x and y values are chosen as the first and last pixel of the black (entity) box of the gui texture
            // The two x and y values determine the size for the 'GuiGraphicsExtractor#enableScissor' call (their middle point is also where the entity will be rendered)
            InventoryScreen.extractEntityInInventoryFollowsMouse(guiGraphics, leftPos + 8, topPos + 8, leftPos + 59, topPos + 69, 20, 0.25f, x, y, umvuthana);
            umvuthana.renderingInGUI = false;
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int x, int y) {
        String title = I18n.get("entity.mowziesmobs.umvuthana.trade");
        guiGraphics.text(font, title, (int) (imageWidth / 2f - font.width(title) / 2f + 26), 6, 4210752, false);
        guiGraphics.text(font, I18n.get("container.inventory"), 8, imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
        if (umvuthana != null && umvuthana.isOfferingTrade()) {
            Trade trade = umvuthana.getOfferingTrade();
            ItemStack input = trade.getInput();
            ItemStack output = trade.getOutput();
            guiGraphics.pose().pushMatrix();

            guiGraphics.item(input, leftPos + 80, topPos + 24);
            guiGraphics.itemDecorations(font, input, leftPos + 80, topPos + 24);
            guiGraphics.item(output, leftPos + 134, topPos + 24);
            guiGraphics.itemDecorations(font, output, leftPos + 134, topPos + 24);

            if (isHovering(80, 24, 16, 16, mouseX, mouseY)) {
                guiGraphics.setTooltipForNextFrame(font, input, mouseX, mouseY);
            } else if (isHovering(134, 24, 16, 16, mouseX, mouseY)) {
                guiGraphics.setTooltipForNextFrame(font, output, mouseX, mouseY);
            }
            guiGraphics.pose().popMatrix();
        }
    }
}
