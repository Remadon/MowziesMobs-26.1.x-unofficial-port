package com.bobmowzie.mowziesmobs.client;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.client.gui.CustomBossBar;
import com.bobmowzie.mowziesmobs.client.render.block.SculptorBlockMarking;
import com.bobmowzie.mowziesmobs.client.render.entity.player.GeckoFirstPersonRenderer;
import com.bobmowzie.mowziesmobs.client.render.entity.player.GeckoPlayer;
import com.bobmowzie.mowziesmobs.client.render.entity.player.GeckoRenderPlayer;
import com.bobmowzie.mowziesmobs.client.sound.BossMusicPlayer;
import com.bobmowzie.mowziesmobs.server.capability.AbilityData;
import com.bobmowzie.mowziesmobs.server.capability.DataHandler;
import com.bobmowzie.mowziesmobs.server.capability.FrozenData;
import com.bobmowzie.mowziesmobs.server.capability.PlayerData;
import com.bobmowzie.mowziesmobs.server.config.ConfigHandler;
import com.bobmowzie.mowziesmobs.server.entity.effects.EntityCameraShake;
import com.bobmowzie.mowziesmobs.server.entity.frostmaw.EntityFrozenController;
import com.bobmowzie.mowziesmobs.server.item.ItemBlowgun;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class ClientEventHandler {
    private static final Identifier FROZEN_BLUR = Identifier.withDefaultNamespace("textures/misc/powder_snow_outline.png");

    // PORTING NOTE (1.21.1 -> 26.1.2): wiring for the player-render agent's GeckoFirstPersonRenderer/
    // GeckoRenderPlayer redesign (see those classes' own class javadocs in client/render/entity/player/ for the
    // full architecture writeup - this comment only covers the wiring performed here).
    // RenderHandEvent no longer exposes a MultiBufferSource (only getSubmitNodeCollector()), and
    // GeckoFirstPersonRenderer#renderHands is now a single call covering BOTH hands per frame (GeckoLib 5 no longer
    // lets a stale bone pose be read for a second, off-hand-only call - see that method's own javadoc) - so this
    // gates on MAIN_HAND to invoke it exactly once, while still cancelling vanilla rendering for both hands' events
    // when an ability is active.
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHandRender(RenderHandEvent event) {
        if (!ConfigHandler.CLIENT.customPlayerAnims.get()) return;

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        AbilityData abilityData = DataHandler.getData(player, DataHandler.ABILITY_DATA);
        if (abilityData == null || abilityData.getActiveAbility() == null) return;

        GeckoPlayer.GeckoPlayerFirstPerson geckoPlayerFirstPerson = GeckoFirstPersonRenderer.GECKO_PLAYER_FIRST_PERSON;
        if (geckoPlayerFirstPerson == null) return;
        GeoRenderer<GeckoPlayer, Void, GeoRenderState> renderer = geckoPlayerFirstPerson.getPlayerRenderer();
        if (!(renderer instanceof GeckoFirstPersonRenderer firstPersonRenderer)) return;

        event.setCanceled(true);

        if (event.getHand() == InteractionHand.MAIN_HAND) {
            CameraRenderState cameraState = Minecraft.getInstance().gameRenderer.getGameRenderState().levelRenderState.cameraRenderState;
            firstPersonRenderer.renderHands((AbstractClientPlayer) player, geckoPlayerFirstPerson, event.getInterpolatedPitch(), event.getPartialTick(), event.getPoseStack(), event.getSubmitNodeCollector(), cameraState, event.getPackedLight());
        }
    }

    // PORTING NOTE: subscribing to the player-specific RenderPlayerEvent.Pre (rather than the generic
    // RenderLivingEvent.Pre) gives a strongly-typed AvatarRenderState directly, with no live-entity back-reference
    // (confirmed against real 26.1.2.95 NeoForge source - both dropped getEntity()) - AvatarRenderState.id (set by
    // vanilla's own AvatarRenderer#extractRenderState) is resolved back to the live entity via
    // Minecraft.getInstance().level.getEntity(id), per GeckoRenderPlayer's class javadoc wiring note.
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void renderLivingEvent(RenderPlayerEvent.Pre<?> event) {
        if (!ConfigHandler.CLIENT.customPlayerAnims.get()) return;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        AvatarRenderState renderState = event.getRenderState();
        if (!(level.getEntity(renderState.id) instanceof Player player)) return;

        if (ConfigHandler.CLIENT.hidePlayerAnimsInFirstPerson.get()
                && player == Minecraft.getInstance().player
                && Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON) {
            return;
        }

        AbilityData abilityData = DataHandler.getData(player, DataHandler.ABILITY_DATA);
        if (abilityData == null || abilityData.getActiveAbility() == null) return;

        GeckoPlayer geckoPlayer = DataHandler.getData(player, DataHandler.PLAYER_DATA).getGeckoPlayer();
        if (geckoPlayer == null) return;
        GeoRenderer<GeckoPlayer, Void, GeoRenderState> renderer = geckoPlayer.getPlayerRenderer();
        if (!(renderer instanceof GeckoRenderPlayer geckoRenderPlayer)) return;

        event.setCanceled(true);
        CameraRenderState cameraState = Minecraft.getInstance().gameRenderer.getGameRenderState().levelRenderState.cameraRenderState;
        geckoRenderPlayer.render((AbstractClientPlayer) player, geckoPlayer, event.getPoseStack(), event.getSubmitNodeCollector(), cameraState, renderState.lightCoords, event.getPartialTick());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        if (player.level().isClientSide()) {
            GeckoPlayer geckoPlayer = DataHandler.getData(player, DataHandler.PLAYER_DATA).getGeckoPlayer();
            if (geckoPlayer != null) geckoPlayer.tick();
            if (player == Minecraft.getInstance().player) GeckoFirstPersonRenderer.GECKO_PLAYER_FIRST_PERSON.tick();
        }
//        if(player.getInventory().getArmor(3).is(ItemHandler.SOL_VISAGE.asItem())){
//            int tick = player.tickCount;
//            double orbitSpeed = 50;
//            double orbitSize = 0.6;
//            double xOffset = (Math.sin(tick * orbitSpeed) * orbitSize);
//            double zOffset= (Math.cos(tick * orbitSpeed) * orbitSize);
//            Vec3 particleVec = Vec3.ZERO.add(xOffset, 2.2f, zOffset).yRot((float)Math.toRadians(-player.getYHeadRot())).xRot((float) Math.toRadians(0f)).add(player.position());
//            Vec3 particleVec2 = Vec3.ZERO.add(-xOffset, 2.2f, -zOffset).yRot((float)Math.toRadians(-player.getYHeadRot())).xRot((float) Math.toRadians(0f)).add(player.position());
//
//            player.level.addParticle(ParticleTypes.SMALL_FLAME, particleVec.x, particleVec.y, particleVec.z, 0d, 0d, 0d);
//            player.level.addParticle(ParticleTypes.SMALL_FLAME, particleVec2.x, particleVec2.y, particleVec2.z, 0d, 0d, 0d);
//
//        }
    }

    @SubscribeEvent
    public static void onRenderTick(RenderFrameEvent.Pre event) {
        Player player = Minecraft.getInstance().player;

        if (player == null) {
            return;
        }

//        if (player != null) {
//            PlayerCapability.Capability playerCapability = CapabilityHandler.getCapability(player, CapabilityHandler.PLAYER_CAPABILITY);
//            if (playerCapability != null && playerCapability.getGeomancy().canUse(player) && playerCapability.getGeomancy().isSpawningBoulder() && playerCapability.getGeomancy().getSpawnBoulderCharge() > 2) {
//                Vector3d lookPos = playerCapability.getGeomancy().getLookPos();
//                Vector3d playerEyes = player.getEyePosition(Minecraft.getInstance().getRenderPartialTicks());
//                Vector3d vec = playerEyes.subtract(lookPos).normalize();
//                float yaw = (float) Math.atan2(vec.z, vec.x);
//                float pitch = (float) Math.asin(vec.y);
//                player.rotationYaw = (float) (yaw * 180f/Math.PI + 90);
//                player.rotationPitch = (float) (pitch * 180f/Math.PI);
//                player.rotationYawHead = player.rotationYaw;
//                player.prevRotationYaw = player.rotationYaw;
//                player.prevRotationPitch = player.rotationPitch;
//                player.prevRotationYawHead = player.rotationYawHead;
//            }
        FrozenData data = DataHandler.getData(player, DataHandler.FROZEN_DATA);
        if (data.getFrozen() && data.getPrevFrozen()) {
            player.setYRot(data.getFrozenYaw());
            player.setXRot(data.getFrozenPitch());
            player.yHeadRot = data.getFrozenYawHead();
            player.yRotO = player.getYRot();
            player.xRotO = player.getXRot();
            player.yHeadRotO = player.yHeadRot;
        }
    }

    /* FIXME 1.21 -> 26.1.2 port: UNRESOLVED, flagged for follow-up.
        RenderLivingEvent.Pre gained a 3rd type parameter (S extends LivingEntityRenderState) and, critically,
        DROPPED getEntity() entirely (see the big FIXME above onHandRender for the full writeup of this event's
        redesign, confirmed against the real NeoForge 26.1.2.95 source). This used to mutate the live entity's
        rotation/walk-animation/attack-swing fields directly right before vanilla read them for rendering, to
        visually "lock" a frozen entity's pose every frame.

        Rendering now reads from an already-captured LivingEntityRenderState snapshot (still a plain mutable POJO -
        confirmed its yRot/xRot/bodyRot/walkAnimationPos/walkAnimationSpeed fields are public and non-final, and
        ArmedEntityRenderState - the subclass covering humanoids/item-holding mobs - separately has a mutable
        attackTime field), so mutating THAT would be the natural fix... except this event provides no way to look
        up which live entity the snapshot belongs to (no entity ID/UUID field on EntityRenderState or its
        subclasses), and FrozenData is a per-entity NeoForge data attachment that can only be looked up via
        DataHandler.getData(Entity, ...) - i.e. from the live entity, which is unobtainable here. Checked for a
        dedicated "extract entity render state" event that might provide both the live entity AND a mutable state
        object (which would be the correct new hook point, mirroring net.neoforged.neoforge.client.event.
        ExtractLevelRenderStateEvent / ExtractBlockOutlineRenderStateEvent that exist for other render-state types)
        - no living-entity equivalent exists in NeoForge 26.1.2.95's client.event package.

        ACTION NEEDED: the likely correct redesign is to move this "pin rotation/animation while frozen" logic out
        of a render event entirely and into a per-tick hook on the live entity (mirroring how onRenderTick already
        does exactly this for the local player, just generalized to arbitrary LivingEntity) - set both the current
        AND previous-tick rotation/animation fields identically every tick so vanilla's own partial-tick
        interpolation naturally produces a frozen, non-interpolating pose by the time extractRenderState() captures
        it, without needing this event at all. Not implemented here since it changes the timing/hook entirely
        (tick-rate vs. every render frame) and needs verification for interpolation smoothness - flagging rather
        than guessing. Left as a no-op.
    */
    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ? extends LivingEntityRenderState, ?> event) {
        // See FIXME above - no longer possible to resolve the live entity from this event to look up FrozenData.
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiLayerEvent.Post event) {
        if (event.getName() == VanillaGuiLayers.CAMERA_OVERLAYS) {
            if (Minecraft.getInstance().player != null) {
                FrozenData data = DataHandler.getData(Minecraft.getInstance().player, DataHandler.FROZEN_DATA);

                if (data.getFrozen() && Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON) {
                    GuiGraphicsExtractor graphics = event.getGuiGraphics();
                    // PORTING NOTE (1.21.1 -> 26.1.2): GuiGraphics -> GuiGraphicsExtractor, and blit(...) now always
                    // requires an explicit RenderPipeline - mirrors vanilla's own full-screen texture overlay helper,
                    // Gui#extractTextureOverlay(GuiGraphicsExtractor, Identifier, float) in the real 26.1.2 source
                    // (used for the vanilla powder-snow screen outline - the exact same texture this reuses).
                    graphics.blit(RenderPipelines.GUI_TEXTURED, FROZEN_BLUR, 0, 0, 0.0F, 0.0F, graphics.guiWidth(), graphics.guiHeight(), graphics.guiWidth(), graphics.guiHeight(), -1);
                }
            }
        }
    }

    // Remove frozen overlay
    @SubscribeEvent
    public static void onRenderHUD(RenderGuiLayerEvent.Pre event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.isPassenger()) {
            if (player.getVehicle() instanceof EntityFrozenController) {
                if (event.getName() == VanillaGuiLayers.VEHICLE_HEALTH) {
                    event.setCanceled(true);
                }
                Minecraft.getInstance().gui.setOverlayMessage(Component.empty(), false);
            }
        }
    }

    @SubscribeEvent
    public static void updateFOV(ComputeFovModifierEvent event) {
        Player player = event.getPlayer();
        if (player.isUsingItem() && player.getUseItem().getItem() instanceof ItemBlowgun) {
            int i = player.getTicksUsingItem();
            float f1 = (float)i / 5.0F;
            if (f1 > 1.0F) {
                f1 = 1.0F;
            } else {
                f1 = f1 * f1;
            }

            event.setNewFovModifier(1.0F - f1 * 0.15F);
        }
    }

    @SubscribeEvent
    public static void onSetupCamera(ViewportEvent.ComputeCameraAngles event) {
        Player player = Minecraft.getInstance().player;
        float delta = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float ticksExistedDelta = player.tickCount + delta;

        if (ConfigHandler.CLIENT.doCameraShakes.get() && !Minecraft.getInstance().isPaused()) {
            float shakeAmplitude = 0;
            for (EntityCameraShake cameraShake : player.level().getEntitiesOfClass(EntityCameraShake.class, player.getBoundingBox().inflate(20, 20, 20))) {
                if (cameraShake.distanceTo(player) < cameraShake.getRadius()) {
                    shakeAmplitude += cameraShake.getShakeAmount(player, delta);
                }
            }
            if (shakeAmplitude > 1.0f) shakeAmplitude = 1.0f;
            event.setPitch((float) (event.getPitch() + shakeAmplitude * Math.cos(ticksExistedDelta * 3 + 2) * 25));
            event.setYaw((float) (event.getYaw() + shakeAmplitude * Math.cos(ticksExistedDelta * 5 + 1) * 25));
            event.setRoll((float) (event.getRoll() + shakeAmplitude * Math.cos(ticksExistedDelta * 4) * 25));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderBossBar(CustomizeGuiOverlayEvent.BossEventProgress event){
        if (!ConfigHandler.CLIENT.customBossBars.get()) return;
        Identifier bossRegistryName = ClientProxy.bossBarRegistryNames.getOrDefault(event.getBossEvent().getId(), null);
        if (bossRegistryName == null) return;
        CustomBossBar customBossBar = CustomBossBar.customBossBars.getOrDefault(bossRegistryName, null);
        if (customBossBar == null) return;

        event.setCanceled(true);
        customBossBar.renderBossBar(event);
    }

    private static Identifier SCULPTOR_BLOCK_GLOW = Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/entity/sculptor_highlight.png");

    /* FIXME 1.21 -> 26.1.2 port: UNRESOLVED, flagged for follow-up - this feature's rendering technique is gone.
        This used to render a glowing decal overlay (via SheetedDecalTextureGenerator wrapping a custom highlight
        VertexConsumer, MMRenderType.highlight(...)) UV-projected directly onto the real block model geometry of
        each sculptor-marked block, by manually re-tesselating that block's BakedModel through
        BlockRenderDispatcher#getModelRenderer()#tesselateBlock(...).

        None of BlockRenderDispatcher, BakedModel, or that tesselateBlock(...) overload exist anymore (confirmed by
        reading the real decompiled 26.1.2 source - net/minecraft/client/renderer/block/ has no BlockRenderDispatcher
        or BakedModel at all now). Block model rendering was rewritten around a much lower-level, GPU-buffer-facing
        pipeline: net.minecraft.client.renderer.block.BlockModelResolver resolves a BlockState to a
        BlockModelRenderState (a render-state snapshot, matching the same architecture pattern as GuiGraphics ->
        GuiGraphicsExtractor and AnimationState -> AnimationTest elsewhere in this port), and
        ModelBlockRenderer#tesselateBlock(...) now writes into a BlockQuadOutput
        (`void put(float x, float y, float z, BakedQuad quad, QuadInstance instance)`) instead of a VertexConsumer -
        there is no drop-in adapter from that back to a VertexConsumer-based decal generator like
        SheetedDecalTextureGenerator used here. Reproducing this decal effect correctly would require understanding
        QuadInstance/BlockQuadOutput internals well enough to bridge them to immediate-mode vertex emission, which
        was not attempted here to avoid guessing at unverified internals (against this porting pass's rules).

        Also note: net.neoforged.neoforge.client.model.data (ModelData / ModelDataManager, used here to fetch
        per-block-entity model data for the tesselation call) no longer exists in NeoForge 26.1.2.95 at all - grep
        confirmed it's gone from the neoforge jar entirely, so `level.getModelDataManager()` has no replacement to
        port to either.

        The PoseStack push/pop/translate bookkeeping and the SculptorBlockMarking iteration below are left intact
        (those still compile and still work) - only the final "draw the glow onto the block" step is stubbed out.
        ACTION NEEDED: redesign this effect for the new pipeline (e.g. investigate whether NeoForge's
        BlockQuadOutput-based renderer exposes a simpler decal-friendly overload, or switch to a different technique
        entirely such as a translucent box/outline render instead of projecting onto real block geometry).
    */
    // PORTING NOTE (1.21.1 -> 26.1.2): RenderLevelStageEvent lost its Stage enum + getStage()/getCamera() in favor
    // of one concrete subclass per stage (confirmed via javap against the real 26.1.2.95 neoforge jar - no
    // "AFTER_BLOCK_ENTITIES" stage survives as such). Block entities are now submitted just before the
    // AfterOpaqueFeatures stage fires (see LevelRenderer#renderLevel: submitBlockEntities(...) then
    // featureRenderDispatcher.renderSolidFeatures() then the AfterOpaqueFeatures post), which is the closest
    // equivalent timing to the old AFTER_BLOCK_ENTITIES stage. Camera position now comes from
    // event.getLevelRenderState().cameraRenderState.pos (no more getCamera()).
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterOpaqueFeatures event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (Minecraft.getInstance().player != null && level != null) {
            Vec3 cameraPos = event.getLevelRenderState().cameraRenderState.pos;
            double d0 = cameraPos.x();
            double d1 = cameraPos.y();
            double d2 = cameraPos.z();
            for (Long2ObjectMap.Entry<SculptorBlockMarking> entry : ClientProxy.sculptorMarkedBlocks.long2ObjectEntrySet()) {
                BlockPos blockpos2 = BlockPos.of(entry.getLongKey());
                event.getPoseStack().pushPose();
                event.getPoseStack().translate((double) blockpos2.getX() - d0, (double) blockpos2.getY() - d1, (double) blockpos2.getZ() - d2);
                // See FIXME above the method - the glow decal render step used to happen here and is
                // temporarily removed pending a redesign for the new block-model-rendering pipeline.
                event.getPoseStack().popPose();
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide()) {
            MMCommon.PROXY.updateMarkedBlocks();
            BossMusicPlayer.tick();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onInteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        if (event.getKeyMapping() == Minecraft.getInstance().options.keyAttack) {
            PlayerData data = DataHandler.getData(player, DataHandler.PLAYER_DATA);
            if (data != null) {
                data.pressedAttackKey(player);
            }
        } else if (event.getKeyMapping() == Minecraft.getInstance().options.keyUse) {
            PlayerData data = DataHandler.getData(player, DataHandler.PLAYER_DATA);
            if (data != null) {
                data.pressedUseKey(player);
            }
        }
    }
}
