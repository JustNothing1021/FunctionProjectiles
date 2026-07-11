package com.justnothing.functionprojectiles.block;

import com.justnothing.functionprojectiles.expression.ExprParseException;
import com.justnothing.functionprojectiles.expression.ExprParser;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;

public class FunctionAnvilScreenHandler extends ScreenHandler {

    private static final int PLAYER_INVENTORY_START = 1;
    private static final int PLAYER_INVENTORY_END = 37;

    private final SimpleInventory inventory = new SimpleInventory(1);
    private final ScreenHandlerContext context;
    private String newItemName = "";
    private String mode = "function";

    public FunctionAnvilScreenHandler(int syncId, PlayerInventory inv) {
        this(syncId, inv, ScreenHandlerContext.EMPTY);
    }

    public FunctionAnvilScreenHandler(int syncId, PlayerInventory playerInv, ScreenHandlerContext context) {
        super(ModScreenHandlers.FUNCTION_ANVIL, syncId);
        this.context = context;

        this.addSlot(new Slot(inventory, 0, 27, 47)); // input — only one slot

        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; ++col)
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
    }

    public String getMode() { return mode; }
    public void setMode(String m) { this.mode = m; }
    public String getNewItemName() { return newItemName; }
    public void setNewItemName(String n) { this.newItemName = n; }

    public boolean isValid() {
        if (inventory.getStack(0).isEmpty() || newItemName == null || newItemName.isBlank()) return false;
        try {
            if ("parametric".equals(mode)) {
                String[] p = newItemName.split("\\|", 3);
                if (p.length != 3) return false;
                ExprParser.parseForT(p[0].trim());
                ExprParser.parseForT(p[1].trim());
                ExprParser.parseForT(p[2].trim());
            } else {
                ExprParser.parse(newItemName.trim());
            }
            return true;
        } catch (ExprParseException e) { return false; }
    }

    public ItemStack getInputStack() { return inventory.getStack(0); }

    @Override public boolean canUse(PlayerEntity player) { return true; }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasStack()) return ItemStack.EMPTY;
        ItemStack slotStack = slot.getStack();
        ItemStack result = slotStack.copy();
        if (slotIndex == 0) {
            if (!this.insertItem(slotStack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false))
                return ItemStack.EMPTY;
        } else {
            if (!this.insertItem(slotStack, 0, 0, false))
                return ItemStack.EMPTY;
        }
        if (slotStack.isEmpty()) slot.setStack(ItemStack.EMPTY);
        else slot.markDirty();
        return result;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        context.run((world, pos) -> player.getInventory().offerOrDrop(inventory.getStack(0)));
        inventory.clear();
    }
}
