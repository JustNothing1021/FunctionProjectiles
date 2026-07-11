package com.justnothing.functionprojectiles.block;

import com.justnothing.functionprojectiles.component.FunctionComponent;
import com.justnothing.functionprojectiles.component.ModComponents;
import com.justnothing.functionprojectiles.component.ParametricComponent;
import com.justnothing.functionprojectiles.expression.ExprParseException;
import com.justnothing.functionprojectiles.expression.ExprParser;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.Property;
import net.minecraft.screen.slot.Slot;

public class FunctionAnvilScreenHandler extends ScreenHandler {

    private static final int EXP_COST = 3;
    private static final int INPUT_SLOT_INDEX = 0;
    private static final int OUTPUT_SLOT_INDEX = 1;
    private static final int PLAYER_INVENTORY_START = 2;
    private static final int PLAYER_INVENTORY_END = 38;

    private final SimpleInventory input = new SimpleInventory(1);
    private final SimpleInventory output = new SimpleInventory(1);
    private final ScreenHandlerContext context;
    private final Property levelCost = Property.create();
    private String newItemName = "";
    private String mode = "function";

    public FunctionAnvilScreenHandler(int syncId, PlayerInventory inventory) {
        this(syncId, inventory, ScreenHandlerContext.EMPTY);
    }

    public FunctionAnvilScreenHandler(int syncId, PlayerInventory inventory, ScreenHandlerContext context) {
        super(ModScreenHandlers.FUNCTION_ANVIL, syncId);
        this.context = context;
        this.addProperty(levelCost);

        // Input slot - left slot position of the vanilla anvil texture
        this.addSlot(new Slot(input, INPUT_SLOT_INDEX, 27, 47));

        // Output slot - standard anvil output position
        // Note: inventory index is 0 (not OUTPUT_SLOT_INDEX) because SimpleInventory only has 1 slot
        this.addSlot(new Slot(output, 0, 134, 47) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }

            @Override
            public boolean canTakeItems(PlayerEntity playerEntity) {
                return canTakeOutput(playerEntity);
            }

            @Override
            public void onTakeItem(PlayerEntity player, ItemStack stack) {
                onTakeOutput(player, stack);
            }
        });

        // Player inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }

        input.addListener(inv -> updateResult());
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
        updateResult();
    }

    public void setNewItemName(String name) {
        this.newItemName = name;
        updateResult();
    }

    public String getNewItemName() {
        return newItemName;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasStack()) {
            ItemStack slotStack = slot.getStack();
            result = slotStack.copy();

            if (slotIndex == INPUT_SLOT_INDEX) {
                if (!this.insertItem(slotStack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex == OUTPUT_SLOT_INDEX) {
            if (!this.insertItem(slotStack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, true)) {
                return ItemStack.EMPTY;
            }
            if (slotStack.isEmpty()) {
                onTakeOutput(player, result);
            }
        } else {
                if (!this.insertItem(slotStack, INPUT_SLOT_INDEX, OUTPUT_SLOT_INDEX, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }

        return result;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        context.run((world, pos) -> {
            for (int i = 0; i < input.size(); ++i) {
                ItemStack stack = input.getStack(i);
                if (!stack.isEmpty()) {
                    player.getInventory().offerOrDrop(stack);
                }
            }
        });
        input.clear();
        output.clear();
    }

    public void updateResult() {
        ItemStack inputStack = input.getStack(0);
        if (inputStack.isEmpty()) {
            output.setStack(0, ItemStack.EMPTY);
            levelCost.set(0);
            return;
        }

        if (newItemName == null || newItemName.isBlank()) {
            output.setStack(0, ItemStack.EMPTY);
            levelCost.set(0);
            return;
        }

        boolean valid;
        if ("parametric".equals(mode)) {
            valid = validateParametric(newItemName);
        } else {
            valid = validateFunction(newItemName);
        }

        if (!valid) {
            output.setStack(0, ItemStack.EMPTY);
            levelCost.set(0);
            return;
        }

        ItemStack resultStack = inputStack.copy();

        if ("parametric".equals(mode)) {
            resultStack.remove(ModComponents.FUNCTION);
            String[] parts = newItemName.split("\\|", 3);
            if (parts.length == 3) {
                resultStack.set(ModComponents.PARAMETRIC,
                    new ParametricComponent(parts[0].trim(), parts[1].trim(), parts[2].trim()));
            }
        } else {
            resultStack.remove(ModComponents.PARAMETRIC);
            resultStack.set(ModComponents.FUNCTION,
                new FunctionComponent(newItemName.trim()));
        }

        output.setStack(0, resultStack);
        levelCost.set(EXP_COST);
    }

    private boolean canTakeOutput(PlayerEntity player) {
        return player.getAbilities().creativeMode
            || (player.experienceLevel >= EXP_COST && levelCost.get() > 0);
    }

    private void onTakeOutput(PlayerEntity player, ItemStack stack) {
        if (!player.getAbilities().creativeMode) {
            player.addExperienceLevels(-EXP_COST);
        }
        input.getStack(0).decrement(stack.getCount());
        updateResult();
        this.sendContentUpdates();
    }

    private boolean validateFunction(String expression) {
        try {
            ExprParser.parse(expression.trim());
            return true;
        } catch (ExprParseException e) {
            return false;
        }
    }

    private boolean validateParametric(String expression) {
        String[] parts = expression.split("\\|", 3);
        if (parts.length != 3) {
            return false;
        }
        try {
            ExprParser.parseForT(parts[0].trim());
            ExprParser.parseForT(parts[1].trim());
            ExprParser.parseForT(parts[2].trim());
            return true;
        } catch (ExprParseException e) {
            return false;
        }
    }
}
