package nevercry.larp.mixin;

import java.util.Locale;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import nevercry.larp.PriceStore;


@Mixin(ClientPlayNetworkHandler.class)
public class ChatCommandMixin {

    private static final String COMMAND = "!price";

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String content, CallbackInfo ci) {
        if (content == null) return;

        String trimmed = content.trim();
        if (!trimmed.toLowerCase(Locale.ROOT).startsWith(COMMAND)) {
            return;
        }

        ci.cancel();

        String rest = trimmed.substring(COMMAND.length()).trim();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;



        PriceStore.setPrice(rest);
        client.player.sendMessage(
                Text.literal("Price set to:")
                        .formatted(Formatting.GRAY)
                        .append(Text.literal(rest).formatted(Formatting.GREEN)),
                false
        );
    }
}
