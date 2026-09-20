package nevercry.larp.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;

import nevercry.larp.flight.ElytraCam;

/**
 * Pendant le freecam d'elytre, ton vrai joueur ne recoit aucune touche de deplacement :
 * il reste immobile et le serveur ne voit aucun mouvement.
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {

    @Inject(method = "tick", at = @At("TAIL"))
    private void fakeElytra$freezeBody(CallbackInfo ci) {
        if (ElytraCam.isActive()) {
            this.playerInput = PlayerInput.DEFAULT;
            this.movementVector = Vec2f.ZERO;
        }
    }
}
