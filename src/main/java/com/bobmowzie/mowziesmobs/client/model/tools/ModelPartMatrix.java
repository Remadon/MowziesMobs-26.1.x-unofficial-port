package com.bobmowzie.mowziesmobs.client.model.tools;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ModelPartMatrix extends ModelPart {
    private Matrix4f worldXform;
    private Matrix3f worldNormal;

    private boolean resetUseMatrixMode;
    private boolean useMatrixMode;

    // For debugging
    private String name;

    public ModelPartMatrix(ModelPart original) {
        this(original, true);
    }

    public ModelPartMatrix(ModelPart original, boolean resetUseMatrixMode) {
        super(original.cubes, original.children);
        copyFrom(original);

        worldNormal = new Matrix3f();
        worldNormal.identity();
        worldXform = new Matrix4f();
        worldXform.identity();

        useMatrixMode = true;
        this.resetUseMatrixMode = resetUseMatrixMode;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void translateAndRotate(PoseStack matrixStackIn) {
        if (!useMatrixMode || getWorldNormal() == null || getWorldXform() == null) {
            super.translateAndRotate(matrixStackIn);
        }
        else {
            PoseStack.Pose last = matrixStackIn.last();
            last.pose().identity();
            last.normal().identity();
            last.pose().mul(getWorldXform());
            last.normal().mul(getWorldNormal());
        }
        if (resetUseMatrixMode) useMatrixMode = false;
    }

    // PORTING NOTE: vanilla ModelPart#copyFrom(ModelPart) was removed entirely in the 26.1.2 rewrite (cubes/children
    // are now constructor-only final fields, and there's no more bulk-pose-copy helper on the base class). No longer
    // @Override - this is now a plain method that copies the pose fields ModelPart still exposes as public mutable
    // fields (x/y/z/xRot/yRot/zRot/xScale/yScale/zScale/visible/skipDraw), preserving the old behavior manually.
    public void copyFrom(ModelPart modelRendererIn) {
        if (modelRendererIn instanceof ModelPartMatrix) {
            ModelPartMatrix other = (ModelPartMatrix) modelRendererIn;
            this.setWorldNormal(other.getWorldNormal());
            this.setWorldXform(other.getWorldXform());
        }
        this.x = modelRendererIn.x;
        this.y = modelRendererIn.y;
        this.z = modelRendererIn.z;
        this.xRot = modelRendererIn.xRot;
        this.yRot = modelRendererIn.yRot;
        this.zRot = modelRendererIn.zRot;
        this.xScale = modelRendererIn.xScale;
        this.yScale = modelRendererIn.yScale;
        this.zScale = modelRendererIn.zScale;
        this.visible = modelRendererIn.visible;
        this.skipDraw = modelRendererIn.skipDraw;
    }

    public Matrix3f getWorldNormal() {
        return worldNormal;
    }

    public void setWorldNormal(Matrix3f worldNormal) {
        this.worldNormal = worldNormal;
    }

    public Matrix4f getWorldXform() {
        return worldXform;
    }

    public void setWorldXform(Matrix4f worldXform) {
        this.worldXform = worldXform;
        eulerFromMatrix(worldXform);
    }

    public void setUseMatrixMode(boolean useMatrixMode) {
        this.useMatrixMode = useMatrixMode;
    }

    public boolean isUseMatrixMode() {
        return useMatrixMode;
    }

    private void eulerFromMatrix(Matrix4f worldXform) {
//        xRot = (float) Math.atan2(worldXform.m21(), worldXform.m22());
//        yRot = (float) -Math.asin(worldXform.m20());
//        zRot = (float) Math.atan2(worldXform.m10(), worldXform.m00());
        Vector3f vec = RigUtils.eulerAnglesXYZFromMatrix(worldXform);
        xRot = -vec.x();
        yRot = -vec.y();
        zRot = vec.z();
        x = worldXform.m30();
        y = worldXform.m31();
        z = worldXform.m32();
    }
}
