package nevercry.larp.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;

import nevercry.larp.flight.ElytraCam;

/**
 * Le HUD (barre de vie, hotbar...) affiche le joueur de la camera. Comme la camera est le fantome,
 * on force l'affichage de ton vrai joueur.
 */
@Mixin(InGameHud.class)
public class FreecamHudMixin {

    @Inject(method = "getCameraPlayer", at = @At("HEAD"), cancellable = true)
    private void fakeElytra$realPlayerHud(CallbackInfoReturnable<PlayerEntity> cir) {
        if (ElytraCam.isActive()) {
            cir.setReturnValue(MinecraftClient.getInstance().player);
        }
    }
}
