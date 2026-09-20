package nevercry.larp.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;

import nevercry.larp.flight.ElytraCam;

/**
 * Pendant le freecam d'elytre, la souris oriente le fantome et non ton vrai joueur
 * (donc aucun paquet de rotation n'est envoye au serveur).
 */
@Mixin(Entity.class)
public class FreecamLookMixin {

    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void fakeElytra$redirectLook(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if (ElytraCam.isActive() && (Object) this instanceof ClientPlayerEntity) {
            ElytraCam.look(cursorDeltaX, cursorDeltaY);
            ci.cancel();
        }
    }
}
