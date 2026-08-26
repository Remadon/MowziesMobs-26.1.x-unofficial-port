package com.bobmowzie.mowziesmobs.client.render;

import com.bobmowzie.mowziesmobs.client.model.tools.MathUtils;
import com.bobmowzie.mowziesmobs.client.model.tools.ModelPartMatrix;
import com.bobmowzie.mowziesmobs.client.model.tools.geckolib.MowzieGeoBone;
import com.ilexiconn.llibrary.client.model.tools.AdvancedModelRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import com.geckolib.cache.model.cuboid.GeoCube;

/**
 * PORTING NOTE: all methods here that used to take a raw GeckoLib `GeoBone` now take a `MowzieGeoBone` instead.
 * GeckoLib 5's `GeoBone` is a shared, structurally-immutable singleton with no per-instance pos/rot/scale
 * getters/setters any more (see PORTING_NOTES.md "MowzieGeoBone is now a WRAPPER" section) - only `MowzieGeoBone`
 * (the mod's own wrapper around a `GeoBone` + its live `BoneSnapshot`) still exposes `getPivotX/Y/Z`,
 * `getRotX/Y/Z`, `getPosX/Y/Z`, `getScaleX/Y/Z`, and `getParent()` (returning another `MowzieGeoBone`). Callers
 * that used to pass a raw `GeoBone` (e.g. from `model.getBone(name)`) now need to wrap it first:
 * `new MowzieGeoBone(rawGeoBone)`.
 */
public class MowzieRenderUtils {
    public static void matrixStackFromModel(PoseStack matrixStack, AdvancedModelRenderer modelRenderer) {
        AdvancedModelRenderer parent = modelRenderer.getParent();
        if (parent != null) matrixStackFromModel(matrixStack, parent);
        modelRenderer.translateRotate(matrixStack);
    }

    public static Vec3 getWorldPosFromModel(Entity entity, float entityYaw, AdvancedModelRenderer modelRenderer) {
        PoseStack matrixStack = new PoseStack();
        matrixStack.translate(entity.getX(), entity.getY(), entity.getZ());
        matrixStack.mulPose(MathUtils.quatFromRotationXYZ(0, -entityYaw + 180, 0, true));
        matrixStack.scale(-1, -1, 1);
        matrixStack.translate(0, -1.5f, 0);
        MowzieRenderUtils.matrixStackFromModel(matrixStack, modelRenderer);
        PoseStack.Pose matrixEntry = matrixStack.last();
        Matrix4f matrix4f = matrixEntry.pose();

        Vector4f vec = new Vector4f(0, 0, 0, 1);
        vec.mul(matrix4f);
        return new Vec3(vec.x(), vec.y(), vec.z());
    }

    public static void translateRotateGeckolib(MowzieGeoBone bone, PoseStack matrixStackIn) {
        matrixStackIn.translate((double)(bone.getPivotX() / 16.0F), (double)(bone.getPivotY() / 16.0F), (double)(bone.getPivotZ() / 16.0F));
        if (bone.getRotZ() != 0.0F) {
            matrixStackIn.mulPose(Axis.ZP.rotation(bone.getRotZ()));
        }

        if (bone.getRotY() != 0.0F) {
            matrixStackIn.mulPose(Axis.YP.rotation(bone.getRotY()));
        }

        if (bone.getRotX() != 0.0F) {
            matrixStackIn.mulPose(Axis.XP.rotation(bone.getRotX()));
        }

        matrixStackIn.scale(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ());
    }

    public static void matrixStackFromModel(PoseStack matrixStack, MowzieGeoBone geoBone) {
        MowzieGeoBone parent = geoBone.getParent();
        if (parent != null) matrixStackFromModel(matrixStack, parent);
        translateRotateGeckolib(geoBone, matrixStack);
    }

    public static Vec3 getWorldPosFromModel(Entity entity, float entityYaw, MowzieGeoBone geoBone) {
        PoseStack matrixStack = new PoseStack();
        matrixStack.translate(entity.getX(), entity.getY(), entity.getZ());
        matrixStack.mulPose(MathUtils.quatFromRotationXYZ(0, -entityYaw + 180, 0, true));
        matrixStack.scale(-1, -1, 1);
        matrixStack.translate(0, -1.5f, 0);
        MowzieRenderUtils.matrixStackFromModel(matrixStack, geoBone);
        PoseStack.Pose matrixEntry = matrixStack.last();
        Matrix4f matrix4f = matrixEntry.pose();

        Vector4f vec = new Vector4f(0, 0, 0, 1);
        vec.mul(matrix4f);
        return new Vec3(vec.x(), vec.y(), vec.z());
    }

    // Mirrored render utils
    public static void moveToPivotMirror(PoseStack stack, GeoCube cube) {
        Vec3 pivot = cube.pivot();
        stack.translate((double)(-pivot.x() / 16.0F), (double)(pivot.y() / 16.0F), (double)(pivot.z() / 16.0F));
    }

    public static void translateAwayFromPivotPointMirror(PoseStack stack, GeoCube cube) {
        Vec3 pivot = cube.pivot();
        stack.translate((double)(pivot.x() / 16.0F), (double)(-pivot.y() / 16.0F), (double)(-pivot.z() / 16.0F));
    }

    public static void moveToPivotMirror(PoseStack stack, MowzieGeoBone bone) {
        stack.translate((double)(-bone.getPivotX() / 16.0F), (double)(bone.getPivotY() / 16.0F), (double)(bone.getPivotZ() / 16.0F));
    }

    public static void translateAwayFromPivotPointMirror(PoseStack stack, MowzieGeoBone bone) {
        stack.translate((double)(bone.getPivotX() / 16.0F), (double)(-bone.getPivotY() / 16.0F), (double)(-bone.getPivotZ() / 16.0F));
    }

    public static void translateMirror(PoseStack stack, MowzieGeoBone bone) {
        stack.translate((double)(bone.getPosX() / 16.0F), (double)(bone.getPosY() / 16.0F), (double)(bone.getPosZ() / 16.0F));
    }

    public static void rotateMirror(PoseStack stack, MowzieGeoBone bone) {
        if (bone.getRotZ() != 0.0F) {
            stack.mulPose(Axis.ZP.rotation(-bone.getRotZ()));
        }

        if (bone.getRotY() != 0.0F) {
            stack.mulPose(Axis.YP.rotation(-bone.getRotY()));
        }

        if (bone.getRotX() != 0.0F) {
            stack.mulPose(Axis.XP.rotation(bone.getRotX()));
        }

    }

    // Used for elytra layer, parrot layer, cape layer
    public static void transformStackToModelPart(PoseStack stack, ModelPartMatrix part) {
        stack.last().pose().identity();
        stack.last().normal().identity();
        stack.pushPose();
        stack.last().pose().mul(part.getWorldXform());
        stack.last().normal().mul(part.getWorldNormal());
    }
}
