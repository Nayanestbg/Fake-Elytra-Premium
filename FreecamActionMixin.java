package nevercry.larp.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.MinecraftClient;

import nevercry.larp.flight.ElytraCam;

/**
 * Pendant le freecam d'elytre, les clics (attaque, utilisation, casse de bloc, pick block)
 * sont ignores : ton vrai joueur ne doit rien faire cote serveur.
 */
@Mixin(MinecraftClient.class)
public class FreecamActionMixin {

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void fakeElytra$noAttack(CallbackInfoReturnable<Boolean> cir) {
        if (ElytraCam.isActive()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void fakeElytra$noUse(CallbackInfo ci) {
        if (ElytraCam.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void fakeElytra$noBreak(boolean breaking, CallbackInfo ci) {
        if (ElytraCam.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "doItemPick", at = @At("HEAD"), cancellable = true)
    private void fakeElytra$noPick(CallbackInfo ci) {
        if (ElytraCam.isActive()) {
            ci.cancel();
        }
    }
}
