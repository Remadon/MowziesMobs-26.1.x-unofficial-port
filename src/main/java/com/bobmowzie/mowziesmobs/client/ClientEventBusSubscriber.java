package com.bobmowzie.mowziesmobs.client;

import com.bobmowzie.mowziesmobs.client.gui.GuiSculptorTrade;
import com.bobmowzie.mowziesmobs.client.gui.GuiUmvuthanaTrade;
import com.bobmowzie.mowziesmobs.client.gui.GuiUmvuthiTrade;
import com.bobmowzie.mowziesmobs.client.render.block.GongRenderer;
import com.bobmowzie.mowziesmobs.client.render.entity.*;
import com.bobmowzie.mowziesmobs.server.block.entity.BlockEntityHandler;
import com.bobmowzie.mowziesmobs.server.entity.EntityHandler;
import com.bobmowzie.mowziesmobs.server.inventory.ContainerHandler;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class ClientEventBusSubscriber {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            EntityRenderers.register(EntityHandler.BABY_FOLIAATH.get(), RenderFoliaathBaby::new);
            EntityRenderers.register(EntityHandler.FOLIAATH.get(), RenderFoliaath::new);
            EntityRenderers.register(EntityHandler.WROUGHTNAUT.get(), RenderWroughtnaut::new);
            EntityRenderers.register(EntityHandler.UMVUTHI.get(), RenderUmvuthi::new);
            EntityRenderers.register(EntityHandler.UMVUTHANA_RAPTOR.get(), RenderUmvuthana::new);
            EntityRenderers.register(EntityHandler.UMVUTHANA_FOLLOWER_TO_RAPTOR.get(), RenderUmvuthana::new);
            EntityRenderers.register(EntityHandler.UMVUTHANA_MINION.get(), RenderUmvuthana::new);
            EntityRenderers.register(EntityHandler.UMVUTHANA_FOLLOWER_TO_PLAYER.get(), RenderUmvuthana::new);
            EntityRenderers.register(EntityHandler.UMVUTHANA_CRANE_TO_PLAYER.get(), RenderUmvuthana::new);
            EntityRenderers.register(EntityHandler.UMVUTHANA_CRANE.get(), RenderUmvuthana::new);
            EntityRenderers.register(EntityHandler.FROSTMAW.get(), RenderFrostmaw::new);
            EntityRenderers.register(EntityHandler.GROTTOL.get(), RenderGrottol::new);
            EntityRenderers.register(EntityHandler.LANTERN.get(), RenderLantern::new);
            EntityRenderers.register(EntityHandler.NAGA.get(), RenderNaga::new);
            EntityRenderers.register(EntityHandler.SCULPTOR.get(), RenderSculptor::new);
            EntityRenderers.register(EntityHandler.BLUFF.get(), RenderBluff::new);
            EntityRenderers.register(EntityHandler.ELOKOSA_FOLLOWER_TO_HOWLER.get(), RenderElokosa::new);
            EntityRenderers.register(EntityHandler.ELOKOSA_HOWLER.get(), RenderElokosa::new);

            EntityRenderers.register(EntityHandler.DART.get(), RenderDart::new);
            EntityRenderers.register(EntityHandler.SUNSTRIKE.get(), RenderSunstrike::new);
            EntityRenderers.register(EntityHandler.SOLAR_BEAM.get(), RenderSolarBeam::new);
            EntityRenderers.register(EntityHandler.BOULDER_PROJECTILE.get(), RenderBoulder::new);
            EntityRenderers.register(EntityHandler.BOULDER_SCULPTOR.get(), RenderBoulder::new);
            EntityRenderers.register(EntityHandler.BOULDER_SCULPTOR_CRUMBLING.get(), RenderBoulder::new);
            EntityRenderers.register(EntityHandler.PILLAR.get(), RenderPillar::new);
            EntityRenderers.register(EntityHandler.PILLAR_SCULPTOR.get(), RenderPillar::new);
            EntityRenderers.register(EntityHandler.PILLAR_PIECE.get(), RenderNothing::new);
            EntityRenderers.register(EntityHandler.AXE_ATTACK.get(), RenderAxeAttack::new);
            EntityRenderers.register(EntityHandler.POISON_BALL.get(), RenderPoisonBall::new);
            EntityRenderers.register(EntityHandler.ICE_BALL.get(), RenderIceBall::new);
            EntityRenderers.register(EntityHandler.ICE_BREATH.get(), RenderNothing::new);
            EntityRenderers.register(EntityHandler.FROZEN_CONTROLLER.get(), RenderNothing::new);
            EntityRenderers.register(EntityHandler.SUPER_NOVA.get(), RenderSuperNova::new);
            EntityRenderers.register(EntityHandler.FALLING_BLOCK.get(), RenderFallingBlock::new);
            EntityRenderers.register(EntityHandler.BLOCK_SWAPPER.get(), RenderNothing::new);
            EntityRenderers.register(EntityHandler.BLOCK_SWAPPER_TUNNELING.get(), RenderNothing::new);
            EntityRenderers.register(EntityHandler.CAMERA_SHAKE.get(), RenderNothing::new);
            EntityRenderers.register(EntityHandler.ROCK_SLING.get(), RenderRockSling::new);
            EntityRenderers.register(EntityHandler.FISSURE.get(), RenderNothing::new);
            EntityRenderers.register(EntityHandler.FISSURE_PIECE.get(), RenderFissurePiece::new);
            EntityRenderers.register(EntityHandler.EARTH_SPIKE.get(), RenderEarthSpike::new);

            BlockEntityRenderers.register(BlockEntityHandler.GONG_BLOCK_ENTITY.get(), GongRenderer::new);
        });
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ContainerHandler.CONTAINER_UMVUTHANA_TRADE.get(), GuiUmvuthanaTrade::new);
        event.register(ContainerHandler.CONTAINER_UMVUTHI_TRADE.get(), GuiUmvuthiTrade::new);
        event.register(ContainerHandler.CONTAINER_SCULPTOR_TRADE.get(), GuiSculptorTrade::new);
    }

    /* FIXME 1.21 -> 26.1.2 port: UNRESOLVED, flagged for follow-up (see client/MMModels.java's class javadoc for
        the full explanation). This used to be:
            @SubscribeEvent
            public static void onRegisterModels(ModelEvent.RegisterAdditional modelRegistryEvent) {
                for (String item : MMModels.HAND_MODEL_ITEMS) {
                    modelRegistryEvent.register(MMModels.prefixed(item + "_in_hand"));
                }
                for (MaskType type : MaskType.values()) {
                    modelRegistryEvent.register(MMModels.prefixed("umvuthana_mask_" + type.name + "_frame"));
                }
                modelRegistryEvent.register(MMModels.prefixed("sol_visage_frame"));
            }
        `ModelEvent.RegisterAdditional` no longer exists in NeoForge 26.1.2.95 (confirmed by reading the real
        net/neoforged/neoforge/client/event/ModelEvent.java source - only ModifyBakingResult, BakingCompleted,
        RegisterStandalone, and RegisterLoaders remain). The closest analog, RegisterStandalone, needs a
        StandaloneModelKey<T> + UnbakedStandaloneModel<T> per model and a different retrieval path
        (ModelManager#getStandaloneModel(key)) than the old "just ensure this model id gets baked so it's in the
        baking-result map" pattern this method relied on. Left unregistered rather than guessing at the redesign;
        see MMModels.java for the fuller writeup of why this whole feature needs a data-driven (item model JSON)
        redesign rather than a mechanical port.
    */
}