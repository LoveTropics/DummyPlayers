package com.tterrag.dummyplayers.client.renderer.dummy;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.Rotations;

public class DummyPlayerRenderState extends HumanoidRenderState {
	private static final Rotations NO_ROTATION = new Rotations(0.0f, 0.0f, 0.0f);

	public PlayerSkin skin = DefaultPlayerSkin.getDefaultSkin();

	public Rotations headPose = NO_ROTATION;
	public Rotations bodyPose = NO_ROTATION;
	public Rotations leftArmPose = NO_ROTATION;
	public Rotations rightArmPose = NO_ROTATION;
	public Rotations leftLegPose = NO_ROTATION;
	public Rotations rightLegPose = NO_ROTATION;

	public boolean showBasePlate;
}
