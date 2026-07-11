package com.justnothing.functionprojectiles.client;

import com.justnothing.functionprojectiles.block.FunctionAnvilScreenHandler;
import com.justnothing.functionprojectiles.component.FunctionComponent;
import com.justnothing.functionprojectiles.component.ModComponents;
import com.justnothing.functionprojectiles.component.ParametricComponent;
import com.justnothing.functionprojectiles.network.ModNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class FunctionAnvilScreen extends AbstractContainerScreen<FunctionAnvilScreenHandler> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/gui/container/anvil.png");
    private EditBox expressionField;
    private Button modeButton;
    private boolean autoFilled = false;
    private ItemStack lastInputStack = ItemStack.EMPTY;

    public FunctionAnvilScreen(FunctionAnvilScreenHandler menu,
                                Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Expression input field - positioned in the rename area of the anvil
        this.expressionField = new EditBox(this.font,
            x + 59, y + 20, 112, 16, Component.literal("Expression"));
        this.expressionField.setBordered(true);
        this.expressionField.setMaxLength(1024);
        this.expressionField.setResponder(this::onExpressionChanged);
        updatePlaceholder();
        this.addRenderableWidget(this.expressionField);
        this.setInitialFocus(this.expressionField);

        // Mode toggle button - below the anvil area, above player inventory
        this.modeButton = Button.builder(
            getModeButtonText(),
            button -> toggleMode()
        ).bounds(x + 44, y + 68, 88, 16).build();
        this.addRenderableWidget(this.modeButton);
    }

    private void onExpressionChanged(String text) {
        this.menu.setNewItemName(text);
        ClientPlayNetworking.send(new ModNetworking.FunctionAnvilUpdatePayload(
            text, this.menu.getMode()
        ));
    }

    private void toggleMode() {
        String currentMode = this.menu.getMode();
        String newMode = "function".equals(currentMode) ? "parametric" : "function";
        this.menu.setMode(newMode);
        this.modeButton.setMessage(getModeButtonText());
        updatePlaceholder();
        this.expressionField.setValue("");
        ClientPlayNetworking.send(new ModNetworking.FunctionAnvilUpdatePayload(
            "", newMode
        ));
    }

    private Component getModeButtonText() {
        return "parametric".equals(this.menu.getMode())
            ? Component.translatable("function-projectiles.mode.parametric")
            : Component.translatable("function-projectiles.mode.function");
    }

    private void updatePlaceholder() {
        if (this.expressionField == null) return;
        if ("parametric".equals(this.menu.getMode())) {
            this.expressionField.setHint(Component.literal("x(t)|y(t)|z(t)"));
        } else {
            this.expressionField.setHint(Component.literal("f(x)"));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // Auto-fill expression from existing component only once when item is first placed
        ItemStack currentInput = this.menu.slots.get(0).getItem();
        boolean inputChanged = !ItemStack.isSameItemSameComponents(lastInputStack, currentInput);
        if (inputChanged) {
            lastInputStack = currentInput.copy();
            autoFilled = false;
        }
        if (!autoFilled && expressionField != null && expressionField.getValue().isEmpty() && !currentInput.isEmpty()) {
            FunctionComponent fc = currentInput.get(ModComponents.FUNCTION);
            if (fc != null) {
                expressionField.setValue(fc.expression());
                onExpressionChanged(fc.expression());
                if (!"function".equals(this.menu.getMode())) {
                    this.menu.setMode("function");
                    this.modeButton.setMessage(getModeButtonText());
                    updatePlaceholder();
                }
            } else {
                ParametricComponent pc = currentInput.get(ModComponents.PARAMETRIC);
                if (pc != null) {
                    String expr = pc.expressionX() + "|" + pc.expressionY() + "|" + pc.expressionZ();
                    expressionField.setValue(expr);
                    onExpressionChanged(expr);
                    if (!"parametric".equals(this.menu.getMode())) {
                        this.menu.setMode("parametric");
                        this.modeButton.setMessage(getModeButtonText());
                        updatePlaceholder();
                    }
                }
            }
            autoFilled = true;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        // extractLabels is called inside a translated context (leftPos, topPos),
        // so use relative coordinates here
        String label = "parametric".equals(this.menu.getMode())
            ? "x(t)|y(t)|z(t):" : "f(x) =";
        graphics.text(this.font, label, 59, 11, 0x808080, false);
    }

    @Override
    public void resize(int width, int height) {
        String text = this.expressionField != null ? this.expressionField.getValue() : "";
        super.resize(width, height);
        if (this.expressionField != null) {
            this.expressionField.setValue(text);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (this.expressionField != null && this.expressionField.isFocused()) {
            if (this.expressionField.keyPressed(keyEvent)) {
                return true;
            }
            // Consume key events when the text field is focused to prevent
            // game key bindings (like 'E' for inventory) from firing,
            // but allow Escape to close the screen
            if (keyEvent.key() != 256) { // GLFW_KEY_ESCAPE
                return true;
            }
        }
        return super.keyPressed(keyEvent);
    }

    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        if (this.expressionField != null && this.expressionField.isFocused()) {
            if (this.expressionField.charTyped(characterEvent)) {
                return true;
            }
        }
        return super.charTyped(characterEvent);
    }
}
