package com.bobmowzie.mowziesmobs.client.render.block;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.client.model.LayerHandler;
import com.bobmowzie.mowziesmobs.client.model.tools.MathUtils;
import com.bobmowzie.mowziesmobs.server.block.entity.GongBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

public class GongRenderer implements BlockEntityRenderer<GongBlockEntity, GongRenderer.GongRenderState> {
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/block/gong.png");
    private final ModelPart gongBase;
    private final ModelPart chain;

    public GongRenderer(BlockEntityRendererProvider.Context context) {
        ModelPart modelpart = context.bakeLayer(LayerHandler.GONG_LAYER);
        this.gongBase = modelpart.getChild("root");
        this.chain = gongBase.getChild("chain");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create().texOffs(88, 46).addBox(-35.75F, -23.25F, 5.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(112, 16).addBox(0.25F, -57.25F, 7.0F, 4.0F, 34.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(48, 32).addBox(-1.75F, -59.25F, 5.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(88, 46).addBox(-1.75F, -23.25F, 5.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(112, 16).addBox(-33.75F, -57.25F, 7.0F, 4.0F, 34.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(48, 32).addBox(-35.75F, -59.25F, 5.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(0, 16).addBox(-37.75F, -56.25F, 7.5F, 46.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(-38.75F, -63.25F, 3.0F, 48.0F, 4.0F, 12.0F, new CubeDeformation(0.0F))
                .texOffs(48, 21).addBox(-27.75F, -59.25F, 8.5F, 26.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(14.75F, 39.25F, -8.5F));

        PartDefinition chain = root.addOrReplaceChild("chain", CubeListBuilder.create().texOffs(48, 24).addBox(-11.0F, 0.0F, 0.0F, 22.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(-14.75F, -54.25F, 8.5F));

        PartDefinition gong = chain.addOrReplaceChild("gong", CubeListBuilder.create().texOffs(0, 21).addBox(-11.75F, -11.75F, -1.0F, 22.0F, 22.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(108, 4).addBox(-3.75F, -3.75F, -1.0F, 6.0F, 6.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(48, 42).addBox(-9.75F, -9.75F, -1.0F, 18.0F, 18.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(0, 45).addBox(-9.75F, -9.75F, -0.5F, 18.0F, 18.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.75F, 19.75F, 0.0F));

        return LayerDefinition.create(meshdefinition, 128, 64);
    }

    @Override
    public GongRenderState createRenderState() {
        return new GongRenderState();
    }

    @Override
    public void extractRenderState(GongBlockEntity blockEntity, GongRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.ticks = (float) blockEntity.ticks + partialTicks;
        state.shaking = blockEntity.shaking;
        state.clickDirection = blockEntity.clickDirection;
        state.facingAxis = blockEntity.facing.getAxis();
    }

    @Override
    public void submit(GongRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5, 1.485, 0.5);
        poseStack.mulPose(MathUtils.quatFromRotationXYZ(0, 0, 180, true));
        if (state.facingAxis == Direction.Axis.X) {
            poseStack.mulPose(MathUtils.quatFromRotationXYZ(0, 90, 0, true));
        }

        float f1 = 0.0F;
        if (state.shaking) {
            float f3 = Mth.sin(state.ticks / (float)Math.PI) / (4.0F + state.ticks / 2.0F);
            if (state.clickDirection == Direction.NORTH) {
                f1 = f3;
            } else if (state.clickDirection == Direction.SOUTH) {
                f1 = -f3;
            } else if (state.clickDirection == Direction.EAST) {
                f1 = f3;
            } else if (state.clickDirection == Direction.WEST) {
                f1 = -f3;
            }
        }

        this.chain.xRot = f1;

        RenderType renderType = RenderTypes.entityCutout(TEXTURE);
        submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, vertexconsumer) -> {
            poseStack.pushPose();
            poseStack.last().set(pose);
            this.gongBase.render(poseStack, vertexconsumer, state.lightCoords, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        });
        poseStack.popPose();
    }


    @Override // Can be debugged with the '/neoforge debug_blockentity_renderbounds true' command
    public @NotNull AABB getRenderBoundingBox(@NotNull GongBlockEntity gong) {
        AABB bounds = new AABB(gong.getBlockPos());
        bounds = bounds.expandTowards(new Vec3(gong.facing.getClockWise().step()));
        bounds = bounds.expandTowards(new Vec3(gong.facing.getCounterClockWise().step()));
        bounds = bounds.expandTowards(0, 2, 0);
        return bounds;
    }

    public static class GongRenderState extends BlockEntityRenderState {
        public float ticks;
        public boolean shaking;
        public Direction clickDirection;
        public Direction.Axis facingAxis;
    }
}
