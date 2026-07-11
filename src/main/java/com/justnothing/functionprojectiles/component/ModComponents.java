package com.justnothing.functionprojectiles.component;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public class ModComponents {

    private static final String FP_KEY = "fp_data";

    public static void register() {}

    public static FunctionComponent getFunction(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(FP_KEY)) return null;
        String[] parts = nbt.getString(FP_KEY).split("\\|");
        if (parts.length != 2 || !"f".equals(parts[0])) return null;
        return new FunctionComponent(parts[1]);
    }

    public static void setFunction(ItemStack stack, String expression) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putString(FP_KEY, "f|" + expression);
    }

    public static ParametricComponent getParametric(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(FP_KEY)) return null;
        String[] parts = nbt.getString(FP_KEY).split("\\|", 4);
        if (parts.length != 4 || !"p".equals(parts[0])) return null;
        return new ParametricComponent(parts[1], parts[2], parts[3]);
    }

    public static void setParametric(ItemStack stack, String x, String y, String z) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putString(FP_KEY, "p|" + x + "|" + y + "|" + z);
    }

    public static boolean hasComponent(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains(FP_KEY);
    }
}
