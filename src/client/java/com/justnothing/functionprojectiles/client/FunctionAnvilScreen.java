package com.justnothing.functionprojectiles.client;

import com.justnothing.functionprojectiles.block.FunctionAnvilScreenHandler;
import com.justnothing.functionprojectiles.component.FunctionComponent;
import com.justnothing.functionprojectiles.component.ModComponents;
import com.justnothing.functionprojectiles.component.ParametricComponent;
import com.justnothing.functionprojectiles.network.ModNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class FunctionAnvilScreen extends HandledScreen<FunctionAnvilScreenHandler> {

    private static final Identifier TEXTURE = Identifier.of("minecraft", "textures/gui/container/anvil.png");
    private TextFieldWidget expressionField;
    private ButtonWidget modeButton;

    public FunctionAnvilScreen(FunctionAnvilScreenHandler handler,
                                PlayerInventory inventory, Text title) {
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

        // Expression input field - positioned in the rename area of the anvil
        this.expressionField = new TextFieldWidget(this.textRenderer,
            x + 59, y + 24, 103, 12, Text.literal("Expression"));
        this.expressionField.setDrawsBackground(false);
        this.expressionField.setMaxLength(256);
        this.expressionField.setChangedListener(this::onExpressionChanged);
        updatePlaceholder();
        this.addSelectableChild(this.expressionField);
        this.setInitialFocus(this.expressionField);

        // Mode toggle button - below the anvil area, above player inventory
        this.modeButton = ButtonWidget.builder(
            getModeButtonText(),
            button -> toggleMode()
        ).dimensions(x + 44, y + 68, 88, 16).build();
        this.addDrawableChild(this.modeButton);
    }

    private void onExpressionChanged(String text) {
        this.handler.setNewItemName(text);
        ClientPlayNetworking.send(new ModNetworking.FunctionAnvilUpdatePayload(
            text, this.handler.getMode()
        ));
    }

    private void toggleMode() {
        String currentMode = this.handler.getMode();
        String newMode = "function".equals(currentMode) ? "parametric" : "function";
        this.handler.setMode(newMode);
        this.modeButton.setMessage(getModeButtonText());
        updatePlaceholder();
        this.expressionField.setText("");
        ClientPlayNetworking.send(new ModNetworking.FunctionAnvilUpdatePayload(
            "", newMode
        ));
    }

    private Text getModeButtonText() {
        return "parametric".equals(this.handler.getMode())
            ? Text.translatable("function-projectiles.mode.parametric")
            : Text.translatable("function-projectiles.mode.function");
    }

    private void updatePlaceholder() {
        if (this.expressionField == null) return;
        if ("parametric".equals(this.handler.getMode())) {
            this.expressionField.setPlaceholder(Text.literal("x(t)|y(t)|z(t)"));
        } else {
            this.expressionField.setPlaceholder(Text.literal("f(x)"));
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        super.drawForeground(context, mouseX, mouseY);
        // Draw expression label above the text field area
        String label = "parametric".equals(this.handler.getMode())
            ? "x(t)|y(t)|z(t):" : "f(x) =";
        context.drawText(this.textRenderer, label, 59, 14, 0x808080, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (this.expressionField != null) {
            this.expressionField.render(context, mouseX, mouseY, delta);
        }
        // Auto-fill expression from existing component when item is placed
        if (expressionField != null && expressionField.getText().isEmpty()) {
            net.minecraft.item.ItemStack stack = this.handler.slots.get(0).getStack();
            if (!stack.isEmpty()) {
                FunctionComponent fc = stack.get(ModComponents.FUNCTION);
                if (fc != null) {
                    expressionField.setText(fc.expression());
                    onExpressionChanged(fc.expression());
                } else {
                    ParametricComponent pc = stack.get(ModComponents.PARAMETRIC);
                    if (pc != null) {
                        String expr = pc.expressionX() + "|" + pc.expressionY() + "|" + pc.expressionZ();
                        expressionField.setText(expr);
                        onExpressionChanged(expr);
                    }
                }
            }
        }
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
        if (this.expressionField != null) {
            this.expressionField.setText(text);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.expressionField != null && this.expressionField.isActive()) {
            if (this.expressionField.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.expressionField != null && this.expressionField.isActive()) {
            if (this.expressionField.charTyped(chr, modifiers)) {
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }
}
