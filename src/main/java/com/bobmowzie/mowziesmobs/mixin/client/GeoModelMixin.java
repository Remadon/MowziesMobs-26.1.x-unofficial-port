package com.bobmowzie.mowziesmobs.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import com.geckolib.model.GeoModel;

/* FIXME 1.21 -> 26.1.2 port: UNRESOLVED, flagged for follow-up.
    This mixin used to work around a GeckoLib 4.x bug where GUI/inventory rendering of a GeoModel entity
    (see MowzieEntity#renderingInGUI, set by client/gui/GuiUmvuthanaTrade, GuiUmvuthiTrade, GuiSculptorTrade)
    would corrupt the "is this a re-render of the same instance" bookkeeping GeckoLib used internally
    (GeoModel#handleAnimations(...) compared against a `lastRenderedInstance` long field), causing the
    in-world entity's animation state to flicker/diverge from the GUI-rendered one.

    In GeckoLib 5.5.2 (com.geckolib.model.GeoModel, see /tmp/geckolib-src/com/geckolib/model/GeoModel.java)
    BOTH the `handleAnimations` method and the `lastRenderedInstance` field are gone entirely - animation
    state is no longer tracked that way. GeckoLib 5's rendering pipeline was rewritten around capturing an
    immutable GeoRenderState snapshot per render pass (see AnimationTest/GeoRenderState in the porting
    notes), which may already eliminate the shared-mutable-state race this mixin worked around - but that
    has NOT been verified at runtime. There is no mechanical equivalent to port this mixin to.

    ACTION NEEDED: after the mod boots, test GUI-rendered Mowzie's Mobs entities (trade screens: Umvuthana,
    Umvuthi, Sculptor) for animation flicker / desync with their in-world counterparts. If the bug still
    reproduces, a fresh GeckoLib-5-specific fix will need to be designed from scratch (there's no equivalent
    field/method to hook anymore) - possibly by giving the GUI-rendered entity its own AnimatableManager
    instance instead of trying to detect "am I a duplicate render" after the fact.

    This mixin is left registered (empty, no injectors) so mowziesmobs.mixins.json's "client" list doesn't
    need touching; it currently does nothing.
*/
@Mixin(value = GeoModel.class, remap = false)
public abstract class GeoModelMixin {
}
