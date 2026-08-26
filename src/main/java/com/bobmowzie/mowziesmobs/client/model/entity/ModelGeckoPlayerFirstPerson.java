package com.bobmowzie.mowziesmobs.client.model.entity;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.client.model.tools.geckolib.MowzieGeoBone;
import com.bobmowzie.mowziesmobs.client.model.tools.geckolib.MowzieGeoModel;
import com.bobmowzie.mowziesmobs.client.render.entity.player.GeckoPlayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.renderer.base.GeoRenderState;

public class ModelGeckoPlayerFirstPerson extends MowzieGeoModel<GeckoPlayer> {
	private Identifier textureLocation;

	public HumanoidModel.ArmPose leftArmPose = HumanoidModel.ArmPose.EMPTY;
	public HumanoidModel.ArmPose rightArmPose = HumanoidModel.ArmPose.EMPTY;

	protected boolean useSmallArms;

	@Override
	public Identifier getAnimationResource(GeckoPlayer animatable) {
		return Identifier.fromNamespaceAndPath(MMCommon.MODID, "animated_player_first_person");
	}

	@Override
	public Identifier getModelResource(GeoRenderState renderState) {
		return Identifier.fromNamespaceAndPath(MMCommon.MODID, "animated_player_first_person");
	}

	@Override
	public Identifier getTextureResource(GeoRenderState renderState) {
		return textureLocation;
	}

	public void setUseSmallArms(boolean useSmallArms) {
		this.useSmallArms = useSmallArms;
	}

	public boolean isUsingSmallArms() {
		return useSmallArms;
	}

	// PORTING NOTE: no longer @Override - see MowzieGeoModel's class javadoc.
	public void setCustomAnimations(GeckoPlayer animatable, long instanceId, AnimationTest<GeckoPlayer> animationState) {
		if (isInitialized()) {
			MowzieGeoBone rightArmLayerClassic = getMowzieBone("RightArmLayerClassic");
			MowzieGeoBone leftArmLayerClassic = getMowzieBone("LeftArmLayerClassic");
			MowzieGeoBone rightArmLayerSlim = getMowzieBone("RightArmLayerSlim");
			MowzieGeoBone leftArmLayerSlim = getMowzieBone("LeftArmLayerSlim");
			MowzieGeoBone rightArmClassic = getMowzieBone("RightArmClassic");
			MowzieGeoBone leftArmClassic = getMowzieBone("LeftArmClassic");
			MowzieGeoBone rightArmSlim = getMowzieBone("RightArmSlim");
			MowzieGeoBone leftArmSlim = getMowzieBone("LeftArmSlim");
			getMowzieBone("LeftHeldItem").setHidden(true);
			getMowzieBone("RightHeldItem").setHidden(true);
			rightArmClassic.setHidden(true);
			leftArmClassic.setHidden(true);
			rightArmLayerClassic.setHidden(true);
			leftArmLayerClassic.setHidden(true);
			rightArmSlim.setHidden(true);
			leftArmSlim.setHidden(true);
			rightArmLayerSlim.setHidden(true);
			leftArmLayerSlim.setHidden(true);
		}
	}

	public void setTextureFromPlayer(AbstractClientPlayer player) {
		this.textureLocation = player.getSkin().body().texturePath();
	}
}