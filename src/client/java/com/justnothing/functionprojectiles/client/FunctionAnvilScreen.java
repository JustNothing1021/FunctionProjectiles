package com.justnothing.functionprojectiles.client;

import com.justnothing.functionprojectiles.block.FunctionAnvilScreenHandler;
import com.justnothing.functionprojectiles.network.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class FunctionAnvilScreen extends HandledScreen<FunctionAnvilScreenHandler> {

    private static final Identifier TEXTURE = new Identifier("minecraft", "textures/gui/container/dispenser.png");
    private TextFieldWidget expressionField;
    private ButtonWidget modeButton;
    private ButtonWidget craftButton;

    public FunctionAnvilScreen(FunctionAnvilScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        this.expressionField = new TextFieldWidget(this.textRenderer, x + 62, y + 18, 106, 16,
            Text.translatable("function-projectiles.expression"));
        this.expressionField.setMaxLength(128);
        this.expressionField.setChangedListener(this::onExpressionChanged);
        this.addSelectableChild(this.expressionField);

        this.modeButton = ButtonWidget.builder(getModeButtonText(), button -> toggleMode())
            .dimensions(x + 62, y + 52, 50, 16).build();
        this.addDrawableChild(this.modeButton);

        this.craftButton = ButtonWidget.builder(Text.translatable("function-projectiles.craft"), button -> craft())
            .dimensions(x + 116, y + 52, 52, 16).build();
        this.addDrawableChild(this.craftButton);
    }

    private void craft() {
        if (!this.handler.isValid()) return;
        var buf = PacketByteBufs.create();
        buf.writeString(this.handler.getNewItemName());
        buf.writeString(this.handler.getMode());
        ClientPlayNetworking.send(ModNetworking.FUNCTION_ANVIL_CRAFT, buf);
    }

    private void onExpressionChanged(String text) {
        this.handler.setNewItemName(text);
        var buf = PacketByteBufs.create();
        buf.writeString(text);
        buf.writeString(this.handler.getMode());
        ClientPlayNetworking.send(ModNetworking.FUNCTION_ANVIL_UPDATE, buf);
    }

    private void toggleMode() {
        String newMode = "function".equals(this.handler.getMode()) ? "parametric" : "function";
        this.handler.setMode(newMode);
        this.modeButton.setMessage(getModeButtonText());
        this.expressionField.setText("");
        var buf = PacketByteBufs.create();
        buf.writeString("");
        buf.writeString(newMode);
        ClientPlayNetworking.send(ModNetworking.FUNCTION_ANVIL_UPDATE, buf);
    }

    private Text getModeButtonText() {
        return "parametric".equals(this.handler.getMode())
            ? Text.translatable("function-projectiles.mode.parametric")
            : Text.translatable("function-projectiles.mode.function");
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        String label = "parametric".equals(this.handler.getMode()) ? "x(t)|y(t)|z(t):" : "f(x) =";
        context.drawText(this.textRenderer, label, 62, 12, 0x808080, false);

        if (!this.handler.getNewItemName().isBlank()) {
            int color = this.handler.isValid() ? 0x00FF00 : 0xFF0000;
            context.drawText(this.textRenderer, this.handler.isValid() ? "OK" : "ERR", 140, 12, color, false);
        }
        context.drawText(this.textRenderer, Text.translatable("container.inventory"),
            this.playerInventoryTitleX, this.playerInventoryTitleY, 4210752, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (this.expressionField != null) this.expressionField.render(context, mouseX, mouseY, delta);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        context.drawTexture(TEXTURE, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

    @Override
    public void resize(net.minecraft.client.MinecraftClient client, int width, int height) {
        String text = this.expressionField != null ? this.expressionField.getText() : "";
        super.resize(client, width, height);
        if (this.expressionField != null) this.expressionField.setText(text);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.expressionField != null && this.expressionField.isActive())
            if (this.expressionField.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.expressionField != null && this.expressionField.isActive())
            if (this.expressionField.charTyped(chr, modifiers)) return true;
        return super.charTyped(chr, modifiers);
    }
}
