package com.justnothing.functionprojectiles.block;

import com.justnothing.functionprojectiles.component.FunctionComponent;
import com.justnothing.functionprojectiles.component.ModComponents;
import com.justnothing.functionprojectiles.component.ParametricComponent;
import com.justnothing.functionprojectiles.expression.ExprParseException;
import com.justnothing.functionprojectiles.expression.ExprParser;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class FunctionAnvilScreenHandler extends AbstractContainerMenu {

    private static final int EXP_COST = 3;
    private static final int INPUT_SLOT_INDEX = 0;
    private static final int OUTPUT_SLOT_INDEX = 1;
    private static final int PLAYER_INVENTORY_START = 2;
    private static final int PLAYER_INVENTORY_END = 38;

    private final SimpleContainer input = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            updateResult();
        }
    };
    private final SimpleContainer output = new SimpleContainer(1);
    private final ContainerLevelAccess context;
    private final DataSlot levelCost = DataSlot.standalone();
    private String newItemName = "";
    private String mode = "function";

    public FunctionAnvilScreenHandler(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public FunctionAnvilScreenHandler(int id, Inventory inventory, ContainerLevelAccess context) {
        super(ModScreenHandlers.FUNCTION_ANVIL, id);
        this.context = context;
        this.addDataSlot(levelCost);

        // Input slot - left slot position of the vanilla anvil texture
        this.addSlot(new Slot(input, INPUT_SLOT_INDEX, 27, 47));

        // Output slot - standard anvil output position
        // Note: inventory index is 0 (not OUTPUT_SLOT_INDEX) because SimpleContainer only has 1 slot
        this.addSlot(new Slot(output, 0, 134, 47) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player playerEntity) {
                return canTakeOutput(playerEntity);
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
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
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            if (slotIndex == INPUT_SLOT_INDEX) {
                if (!this.moveItemStackTo(slotStack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex == OUTPUT_SLOT_INDEX) {
            if (!this.moveItemStackTo(slotStack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, true)) {
                return ItemStack.EMPTY;
            }
            if (slotStack.isEmpty()) {
                onTakeOutput(player, result);
            }
        } else {
                if (!this.moveItemStackTo(slotStack, INPUT_SLOT_INDEX, OUTPUT_SLOT_INDEX, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        context.execute((level, pos) -> {
            for (int i = 0; i < input.getContainerSize(); ++i) {
                ItemStack stack = input.getItem(i);
                if (!stack.isEmpty()) {
                    player.getInventory().add(stack);
                }
            }
        });
        input.clearContent();
        output.clearContent();
    }

    public void updateResult() {
        ItemStack inputStack = input.getItem(0);
        if (inputStack.isEmpty()) {
            output.setItem(0, ItemStack.EMPTY);
            levelCost.set(0);
            return;
        }

        if (newItemName == null || newItemName.isBlank()) {
            output.setItem(0, ItemStack.EMPTY);
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
            output.setItem(0, ItemStack.EMPTY);
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

        output.setItem(0, resultStack);
        levelCost.set(EXP_COST);
    }

    private boolean canTakeOutput(Player player) {
        return player.getAbilities().instabuild
            || (player.experienceLevel >= EXP_COST && levelCost.get() > 0);
    }

    private void onTakeOutput(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            player.giveExperienceLevels(-EXP_COST);
        }
        input.getItem(0).shrink(stack.getCount());
        updateResult();
        this.broadcastChanges();
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
