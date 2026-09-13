package nevercry.larp.mixin;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

@Mixin(ChatScreen.class)
public class ChatTabCompleteMixin {

    private static final String SUGGESTION = "!price";

    @Shadow @Final protected TextFieldWidget chatField;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        if (input.key() != GLFW.GLFW_KEY_TAB) return;

        String text = this.chatField.getText();
        if (isCompletablePrefix(text)) {
            this.chatField.setText(SUGGESTION + " ");
            this.chatField.setCursorToEnd(false);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void renderPriceHint(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        String text = this.chatField.getText();
        if (!isCompletablePrefix(text) || text.equals(SUGGESTION)) return;

        TextRenderer textRenderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;

        String remainder = SUGGESTION.substring(text.length());
        int x = this.chatField.getX() + 4 + textRenderer.getWidth(text);
        int y = this.chatField.getY() + (this.chatField.getHeight() - 8) / 2;

        context.drawText(textRenderer, Text.literal(remainder), x, y, 0xFF808080, false);
    }

    private static boolean isCompletablePrefix(String text) {
        return !text.isEmpty() && text.startsWith("!") && SUGGESTION.startsWith(text);
    }
}
