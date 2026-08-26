package com.bobmowzie.mowziesmobs.server.entity;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.server.entity.bluff.EntityBluff;
import com.bobmowzie.mowziesmobs.server.entity.effects.*;
import com.bobmowzie.mowziesmobs.server.entity.effects.geomancy.*;
import com.bobmowzie.mowziesmobs.server.entity.elokosa.EntityElokosa;
import com.bobmowzie.mowziesmobs.server.entity.elokosa.EntityElokosaFollowerToHowler;
import com.bobmowzie.mowziesmobs.server.entity.elokosa.EntityElokosaHowler;
import com.bobmowzie.mowziesmobs.server.entity.foliaath.EntityBabyFoliaath;
import com.bobmowzie.mowziesmobs.server.entity.foliaath.EntityFoliaath;
import com.bobmowzie.mowziesmobs.server.entity.frostmaw.EntityFrostmaw;
import com.bobmowzie.mowziesmobs.server.entity.frostmaw.EntityFrozenController;
import com.bobmowzie.mowziesmobs.server.entity.grottol.EntityGrottol;
import com.bobmowzie.mowziesmobs.server.entity.lantern.EntityLantern;
import com.bobmowzie.mowziesmobs.server.entity.naga.EntityNaga;
import com.bobmowzie.mowziesmobs.server.entity.sculptor.EntitySculptor;
import com.bobmowzie.mowziesmobs.server.entity.umvuthana.*;
import com.bobmowzie.mowziesmobs.server.entity.umvuthana.trade.Trade;
import com.bobmowzie.mowziesmobs.server.entity.wroughtnaut.EntityWroughtnaut;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Optional;

@EventBusSubscriber
public class EntityHandler {
    public static final DeferredRegister<EntityDataSerializer<?>> SERIALIZER_REG = DeferredRegister.create(NeoForgeRegistries.ENTITY_DATA_SERIALIZERS, MMCommon.MODID);
    public static DeferredHolder<EntityDataSerializer<?>, EntityDataSerializer<Optional<Trade>>> OPTIONAL_TRADE = SERIALIZER_REG.register("optional_trade", () -> EntityDataSerializer.forValueType(Trade.STREAM_CODEC.apply(ByteBufCodecs::optional)));
    // PORTING NOTE (1.21.1 -> 26.1.2): EntityDataSerializer.forValueType(...) constructs an unregistered instance -
    // SynchedEntityData now requires every serializer used in defineSynchedData(...) to be looked up from the
    // NeoForgeRegistries.ENTITY_DATA_SERIALIZERS registry (confirmed at runtime: "Unregistered serializer ... for 8!"
    // IllegalArgumentException crash when an ad-hoc instance was used directly, e.g. in EntityMagicEffect). These two
    // shared serializers replace the several ad-hoc per-file instances that used to exist for the same value types.
    public static final DeferredHolder<EntityDataSerializer<?>, EntityDataSerializer<Optional<java.util.UUID>>> OPTIONAL_UUID = SERIALIZER_REG.register("optional_uuid", () -> EntityDataSerializer.forValueType(ByteBufCodecs.optional(net.minecraft.core.UUIDUtil.STREAM_CODEC)));
    public static final DeferredHolder<EntityDataSerializer<?>, EntityDataSerializer<net.minecraft.nbt.CompoundTag>> COMPOUND_TAG = SERIALIZER_REG.register("compound_tag", () -> EntityDataSerializer.forValueType(ByteBufCodecs.TRUSTED_COMPOUND_TAG));

    public static final DeferredRegister<EntityType<?>> REG = DeferredRegister.create(Registries.ENTITY_TYPE, MMCommon.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<EntityFoliaath>> FOLIAATH = REG.register("foliaath", () -> EntityType.Builder.of(EntityFoliaath::new, MobCategory.MONSTER).sized(0.5f, 2.5f).clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("foliaath"))));
    public static final DeferredHolder<EntityType<?>, EntityType<EntityBabyFoliaath>> BABY_FOLIAATH = REG.register("baby_foliaath", () -> EntityType.Builder.of(EntityBabyFoliaath::new, MobCategory.MONSTER).clientTrackingRange(8).sized(0.4f, 0.4f).clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("baby_foliaath"))));
    public static final DeferredHolder<EntityType<?>, EntityType<EntityWroughtnaut>> WROUGHTNAUT = REG.register("ferrous_wroughtnaut", () -> EntityType.Builder.of(EntityWroughtnaut::new, MobCategory.MONSTER).clientTrackingRange(8).sized(2.5f, 3.5f).clientTrackingRange(8).setUpdateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("ferrous_wroughtnaut"))));
    private static EntityType.Builder<EntityUmvuthanaFollowerToRaptor> umvuthanaFollowerToRaptorBuilder() {
        return EntityType.Builder.of(EntityUmvuthanaFollowerToRaptor::new, MobCategory.MONSTER);
    }
    public static final DeferredHolder<EntityType<?>, EntityType<EntityUmvuthanaFollowerToRaptor>> UMVUTHANA_FOLLOWER_TO_RAPTOR = REG.register("umvuthana_follower_raptor", () -> umvuthanaFollowerToRaptorBuilder().sized(MaskType.FEAR.entityWidth, MaskType.FEAR.entityHeight).clientTrackingRange(8).setUpdateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("umvuthana_follower_raptor"))));
    private static EntityType.Builder<EntityUmvuthanaFollowerToPlayer> umvuthanaFollowerToPlayerBuilder() {
        return EntityType.Builder.of(EntityUmvuthanaFollowerToPlayer::new, MobCategory.MONSTER);
    }
    public static final DeferredHolder<EntityType<?>, EntityType<EntityUmvuthanaFollowerToPlayer>> UMVUTHANA_FOLLOWER_TO_PLAYER = REG.register("umvuthana_follower_player", () -> umvuthanaFollowerToPlayerBuilder().sized(MaskType.FEAR.entityWidth, MaskType.FEAR.entityHeight).clientTrackingRange(8).setUpdateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("umvuthana_follower_player"))));
    private static EntityType.Builder<EntityUmvuthanaCraneToPlayer> umvuthanaCraneToPlayerBuilder() {
        return EntityType.Builder.of(EntityUmvuthanaCraneToPlayer::new, MobCategory.MONSTER);
    }
    public static final DeferredHolder<EntityType<?>, EntityType<EntityUmvuthanaCraneToPlayer>> UMVUTHANA_CRANE_TO_PLAYER = REG.register("umvuthana_crane_player", () -> umvuthanaCraneToPlayerBuilder().sized(MaskType.FAITH.entityWidth, MaskType.FAITH.entityHeight).clientTrackingRange(8).setUpdateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("umvuthana_crane_player"))));
    public static final DeferredHolder<EntityType<?>, EntityType<EntityUmvuthanaMinion>> UMVUTHANA_MINION = REG.register("umvuthana", () -> EntityType.Builder.of(EntityUmvuthanaMinion::new, MobCategory.MONSTER).sized(MaskType.FEAR.entityWidth, MaskType.FEAR.entityHeight).clientTrackingRange(8).setUpdateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("umvuthana"))));
    public static final DeferredHolder<EntityType<?>, EntityType<EntityUmvuthanaRaptor>> UMVUTHANA_RAPTOR = REG.register("umvuthana_raptor", () -> EntityType.Builder.of(EntityUmvuthanaRaptor::new, MobCategory.MONSTER).sized(MaskType.FURY.entityWidth, MaskType.FURY.entityHeight).clientTrackingRange(8).setUpdateInterval(1).notInPeaceful().build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("umvuthana_raptor"))));
    public static final DeferredHolder<EntityType<?>, EntityType<EntityUmvuthanaCrane>> UMVUTHANA_CRANE = REG.register("umvuthana_crane", () -> EntityType.Builder.of(EntityUmvuthanaCrane::new, MobCategory.MONSTER).sized(MaskType.FEAR.entityWidth, MaskType.FEAR.entityHeight).clientTrackingRange(8).setUpdateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("umvuthana_crane"))));
    public static final DeferredHolder<EntityType<?>, EntityType<EntityUmvuthi>> UMVUTHI = REG.register("umvuthi", () -> EntityType.Builder.of(EntityUmvuthi::new, MobCategory.MONSTER).sized(1.5f, 3.2f).clientTrackingRange(10).setUpdateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("umvuthi"))));
    public static final DeferredHolder<EntityType<?>, EntityType<EntityFrostmaw>> FROSTMAW = REG.register("frostmaw", () -> EntityType.Builder.of(EntityFrostmaw::new, MobCategory.MONSTER).sized(4f, 4f).clientTrackingRange(10).setUpdateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("frostmaw"))));
    public static final DeferredHolder<EntityType<?>, EntityType<EntityGrottol>> GROTTOL = REG.register("grottol", () -> EntityType.Builder.of(EntityGrottol::new, MobCategory.MONSTER).sized(0.9F, 1.2F).clientTrackingRange(8).setUpdateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("grottol"))));
    public static final DeferredHolder<EntityType<?>, EntityType<EntityLantern>> LANTERN = REG.register("lantern", () -> EntityType.Builder.of(EntityLantern::new, MobCategory.AMBIENT).sized(1.0f, 1.0f).clientTrackingRange(8).setUpdateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("lantern"))));
    public static final DeferredHolder<EntityType<?>, EntityType<EntityNaga>> NAGA = REG.register("naga", () -> EntityType.Builder.of(EntityNaga::new, MobCategory.MONSTER).sized(3.0f, 1.0f).clientTrackingRange(13).canSpawnFarFromPlayer().setUpdateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("naga"))));
    public static final DeferredHolder<EntityType<?>, EntityType<EntitySculptor>> SCULPTOR = REG.register("sculptor", () -> EntityType.Builder.of(EntitySculptor::new, MobCategory.MISC).sized(1.0f, 2.3f).clientTrackingRange(8).setUpdateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("sculptor"))));
    public static final DeferredHolder<EntityType<?>, EntityType<EntityBluff>> BLUFF = REG.register("bluff", () -> EntityType.Builder.of(EntityBluff::new, MobCategory.MONSTER).sized(0.6F, 1.8F).clientTrackingRange(8).setUpdateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("bluff"))));
    private static EntityType.Builder<EntityElokosaFollowerToHowler> elokosaFollowerToHowlerBuilder() {
        return EntityType.Builder.of(EntityElokosaFollowerToHowler::new, MobCategory.MONSTER);
    }
    public static final DeferredHolder<EntityType<?>, EntityType<EntityElokosaFollowerToHowler>> ELOKOSA_FOLLOWER_TO_HOWLER = REG.register("elokosa_follower_howler", () -> elokosaFollowerToHowlerBuilder().sized(0.9F, 1.6F).clientTrackingRange(8).setUpdateInterval(1).notInPeaceful().build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("elokosa_follower_howler"))));
    public static final DeferredHolder<EntityType<?>, EntityType<EntityElokosaHowler>> ELOKOSA_HOWLER = REG.register("elokosa_howler", () -> EntityType.Builder.of(EntityElokosaHowler::new, MobCategory.MONSTER).sized(0.95F, 1.7F).clientTrackingRange(8).setUpdateInterval(1).notInPeaceful().build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("elokosa_howler"))));

    private static EntityType.Builder<EntitySunstrike> sunstrikeBuilder() {
        return EntityType.Builder.of(EntitySunstrike::new, MobCategory.MISC);
    }
    public static final DeferredHolder<EntityType<?>, EntityType<EntitySunstrike>> SUNSTRIKE = REG.register("sunstrike", () -> sunstrikeBuilder().sized(0.1F, 0.1F).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("sunstrike"))));
    private static EntityType.Builder<EntitySolarBeam> solarBeamBuilder() {
        return EntityType.Builder.of(EntitySolarBeam::new, MobCategory.MISC);
    }
    public static final DeferredHolder<EntityType<?>, EntityType<EntitySolarBeam>> SOLAR_BEAM = REG.register("solar_beam", () -> solarBeamBuilder().sized(0.1F, 0.1F).setUpdateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("solar_beam"))));
    private static EntityType.Builder<EntityBoulderProjectile> boulderProjectileBuilder() {
        return EntityType.Builder.of(EntityBoulderProjectile::new, MobCategory.MISC);
    }
    public static final DeferredHolder<EntityType<?>, EntityType<EntityBoulderProjectile>> BOULDER_PROJECTILE = REG.register("boulder_projectile", () -> boulderProjectileBuilder().sized(1, 1).setUpdateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("boulder_projectile"))));
    private static EntityType.Builder<EntityRockSling> rockSlingBuilder() {
        return EntityType.Builder.of(EntityRockSling::new, MobCategory.MISC);
    }
    public static final DeferredHolder<EntityType<?>, EntityType<EntityRockSling>> ROCK_SLING = REG.register("rock_sling", () -> rockSlingBuilder().sized(0.5f, 0.5f).setUpdateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("rock_sling"))));
    private static EntityType.Builder<EntityBoulderSculptor> boulderPlatformBuilder() {
        return EntityType.Builder.of(EntityBoulderSculptor::new, MobCategory.MISC);
    }
    public static final DeferredHolder<EntityType<?>, EntityType<EntityBoulderSculptor>> BOULDER_SCULPTOR = REG.register("boulder_platform", () -> boulderPlatformBuilder().sized(1, 1).setUpdateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("boulder_platform"))));
    private static EntityType.Builder<EntityBoulderSculptor.EntityBoulderSculptorCrumbling> boulderPlatformCrumblingBuilder() {
        return EntityType.Builder.of(EntityBoulderSculptor.EntityBoulderSculptorCrumbling::new, MobCategory.MISC);
    }
    public static final DeferredHolder<EntityType<?>, EntityType<EntityBoulderSculptor.EntityBoulderSculptorCrumbling>> BOULDER_SCULPTOR_CRUMBLING = REG.register("boulder_platform_crumbling", () -> boulderPlatformCrumblingBuilder().sized(1, 1).setUpdateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("boulder_platform_crumbling"))));
    private static EntityType.Builder<EntityPillar> pillarBuilder() {
        return EntityType.Builder.of(EntityPillar::new, MobCategory.MISC);
    }
    public static final DeferredHolder<EntityType<?>, EntityType<EntityPillar>> PILLAR = REG.register("pillar", () -> pillarBuilder().sized(1f, 1f).setUpdateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("pillar"))));
    private static EntityType.Builder<EntityPillar.EntityPillarSculptor> sculptorPillarBuilder() {
        return EntityType.Builder.of(EntityPillar.EntityPillarSculptor::new, MobCategory.MISC);
    }
    public static final DeferredHolder<EntityType<?>, EntityType<EntityPillar.EntityPillarSculptor>> PILLAR_SCULPTOR = REG.register("pillar_sculptor", () -> sculptorPillarBuilder().sized(1f, 1f).clientTrackingRange(8).setUpdateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("pillar_sculptor"))));
    private static EntityType.Builder<EntityPillarPiece> pillarPieceBuilder() {
        return EntityType.Builder.of(EntityPillarPiece::new, MobCategory.MISC);
    }
    public static final DeferredHolder<EntityType<?>, EntityType<EntityPillarPiece>> PILLAR_PIECE = REG.register("pillar_piece", () -> pillarPieceBuilder().sized(1f, 1f).setUpdateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("pillar_piece"))));

    private static EntityType.Builder<EntityAxeAttack> axeAttackBuilder() {
        return EntityType.Builder.of(EntityAxeAttack::new, MobCategory.MISC);
    }
    public static final DeferredHolder<EntityType<?>, EntityType<EntityAxeAttack>> AXE_ATTACK = REG.register("axe_attack", () -> axeAttackBuilder().sized(1f, 1f).setUpdateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("axe_attack"))));
    private static EntityType.Builder<EntityIceBreath> iceBreathBuilder() {
        return EntityType.Builder.of(EntityIceBreath::new, MobCategory.MISC);
    }
    public static final DeferredHolder<EntityType<?>, EntityType<EntityIceBreath>> ICE_BREATH = REG.register("ice_breath", () -> iceBreathBuilder().sized(0F, 0F).setUpdateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("ice_breath"))));
    private static EntityType.Builder<EntityIceBall> iceBallBuilder() {
        return EntityType.Builder.of(EntityIceBall::new, MobCategory.MISC);
    }
    public static final DeferredHolder<EntityType<?>, EntityType<EntityIceBall>> ICE_BALL = REG.register("ice_ball", () -> iceBallBuilder().sized(0.5F, 0.5F).setUpdateInterval(20).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("ice_ball"))));
    private static EntityType.Builder<EntityFrozenController> frozenControllerBuilder() {
        return EntityType.Builder.of(EntityFrozenController::new, MobCategory.MISC);
    }
    public static final DeferredHolder<EntityType<?>, EntityType<EntityFrozenController>> FROZEN_CONTROLLER = REG.register("frozen_controller", () -> frozenControllerBuilder().noSummon().sized(0, 0).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("frozen_controller"))));
    private static EntityType.Builder<EntityDart> dartBuilder() {
        return EntityType.Builder.of(EntityDart::new, MobCategory.MISC);
    }
    public static final DeferredHolder<EntityType<?>, EntityType<EntityDart>> DART = REG.register("dart", () -> dartBuilder().noSummon().sized(0.5F, 0.5F).setUpdateInterval(20).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("dart"))));
    private static EntityType.Builder<EntityPoisonBall> poisonBallBuilder() {
        return EntityType.Builder.of(EntityPoisonBall::new, MobCategory.MISC);
    }
    public static final DeferredHolder<EntityType<?>, EntityType<EntityPoisonBall>> POISON_BALL = REG.register("poison_ball", () -> poisonBallBuilder().sized(0.5F, 0.5F).setUpdateInterval(20).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("poison_ball"))));
    private static EntityType.Builder<EntitySuperNova> superNovaBuilder() {
        return EntityType.Builder.of(EntitySuperNova::new, MobCategory.MISC);
    }
    public static final DeferredHolder<EntityType<?>, EntityType<EntitySuperNova>> SUPER_NOVA = REG.register("super_nova", () -> superNovaBuilder().sized(1, 1).setUpdateInterval(Integer.MAX_VALUE).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("super_nova"))));
    private static EntityType.Builder<EntityFallingBlock> fallingBlockBuilder() {
        return EntityType.Builder.of(EntityFallingBlock::new, MobCategory.MISC);
    }
    public static final DeferredHolder<EntityType<?>, EntityType<EntityFallingBlock>> FALLING_BLOCK = REG.register("falling_block", () -> fallingBlockBuilder().sized(1, 1).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("falling_block"))));
    private static EntityType.Builder<EntityBlockSwapper> blockSwapperBuilder() {
        return EntityType.Builder.of(EntityBlockSwapper::new, MobCategory.MISC);
    }
    public static final DeferredHolder<EntityType<?>, EntityType<EntityBlockSwapper>> BLOCK_SWAPPER = REG.register("block_swapper", () -> blockSwapperBuilder().noSummon().sized(1, 1).setUpdateInterval(Integer.MAX_VALUE).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("block_swapper"))));
    private static EntityType.Builder<EntityBlockSwapper.EntityBlockSwapperTunneling> blockSwapperTunnelingBuilder() {
        return EntityType.Builder.of(EntityBlockSwapper.EntityBlockSwapperTunneling::new, MobCategory.MISC);
    }
    public static final DeferredHolder<EntityType<?>, EntityType<EntityBlockSwapper.EntityBlockSwapperTunneling>> BLOCK_SWAPPER_TUNNELING = REG.register("block_swapper_tunneling", () -> blockSwapperTunnelingBuilder().noSummon().sized(1, 1).setUpdateInterval(Integer.MAX_VALUE).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("block_swapper_tunneling"))));
    private static EntityType.Builder<EntityCameraShake> cameraShakeBuilder() {
        return EntityType.Builder.of(EntityCameraShake::new, MobCategory.MISC);
    }
    public static final DeferredHolder<EntityType<?>, EntityType<EntityCameraShake>> CAMERA_SHAKE = REG.register("camera_shake", () -> cameraShakeBuilder().sized(1, 1).setUpdateInterval(Integer.MAX_VALUE).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("camera_shake"))));
    private static EntityType.Builder<EntityFissure> fissureBuilder() {
        return EntityType.Builder.of(EntityFissure::new, MobCategory.MISC);
    }
    public static final DeferredHolder<EntityType<?>, EntityType<EntityFissure>> FISSURE = REG.register("fissure", () -> fissureBuilder().sized(1f, 1f).setUpdateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("fissure"))));
    private static EntityType.Builder<EntityFissurePiece> fissurePieceBuilder() {
        return EntityType.Builder.of(EntityFissurePiece::new, MobCategory.MISC);
    }
    public static final DeferredHolder<EntityType<?>, EntityType<EntityFissurePiece>> FISSURE_PIECE = REG.register("fissure_piece", () -> fissurePieceBuilder().sized(EntityFissurePiece.PIECE_SIZE, 0.1f).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("fissure_piece"))));
    private static EntityType.Builder<EntityEarthSpike> earthSpikeBuilder() {
        return EntityType.Builder.of(EntityEarthSpike::new, MobCategory.MISC);
    }
    public static final DeferredHolder<EntityType<?>, EntityType<EntityEarthSpike>> EARTH_SPIKE = REG.register("earth_spike", () -> earthSpikeBuilder().sized(1f, 1f).build(ResourceKey.create(Registries.ENTITY_TYPE, MMCommon.resource("earth_spike"))));

    @SubscribeEvent
    public static void onCreateAttributes(EntityAttributeCreationEvent event) {
        event.put(EntityHandler.FOLIAATH.get(), EntityFoliaath.createAttributes().build());
        event.put(EntityHandler.BABY_FOLIAATH.get(), EntityBabyFoliaath.createAttributes().build());
        event.put(EntityHandler.WROUGHTNAUT.get(), EntityWroughtnaut.createAttributes().build());
        event.put(EntityHandler.UMVUTHANA_RAPTOR.get(), EntityUmvuthanaRaptor.createAttributes().build());
        event.put(EntityHandler.UMVUTHANA_MINION.get(), EntityUmvuthana.createAttributes().build());
        event.put(EntityHandler.UMVUTHANA_FOLLOWER_TO_PLAYER.get(), EntityUmvuthanaFollowerToPlayer.createAttributes().build());
        event.put(EntityHandler.UMVUTHANA_CRANE_TO_PLAYER.get(), EntityUmvuthanaFollowerToPlayer.createAttributes().build());
        event.put(EntityHandler.UMVUTHANA_FOLLOWER_TO_RAPTOR.get(), EntityUmvuthana.createAttributes().build());
        event.put(EntityHandler.UMVUTHANA_CRANE.get(), EntityUmvuthana.createAttributes().build());
        event.put(EntityHandler.UMVUTHI.get(), EntityUmvuthi.createAttributes().build());
        event.put(EntityHandler.FROSTMAW.get(), EntityFrostmaw.createAttributes().build());
        event.put(EntityHandler.NAGA.get(), EntityNaga.createAttributes().build());
        event.put(EntityHandler.LANTERN.get(), EntityLantern.createAttributes().build());
        event.put(EntityHandler.GROTTOL.get(), EntityGrottol.createAttributes().build());
        event.put(EntityHandler.SCULPTOR.get(), EntitySculptor.createAttributes().build());
        event.put(EntityHandler.BLUFF.get(), EntityBluff.createAttributes().build());
        event.put(EntityHandler.ELOKOSA_FOLLOWER_TO_HOWLER.get(), EntityElokosa.createAttributes().build());
        event.put(EntityHandler.ELOKOSA_HOWLER.get(), EntityElokosaHowler.createAttributes().build());
    }
}
