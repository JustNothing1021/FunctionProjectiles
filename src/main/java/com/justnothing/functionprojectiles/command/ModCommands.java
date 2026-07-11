package com.justnothing.functionprojectiles.command;

import com.justnothing.functionprojectiles.component.FunctionComponent;
import com.justnothing.functionprojectiles.component.ModComponents;
import com.justnothing.functionprojectiles.component.ParametricComponent;
import com.justnothing.functionprojectiles.expression.ExprParseException;
import com.justnothing.functionprojectiles.expression.ExprParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ModCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("functionprojectiles")
                .requires(source -> true)
                .then(Commands.literal("apply")
                    .then(Commands.argument("expression", StringArgumentType.greedyString())
                        .executes(ctx -> applyExpression(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "expression"),
                            "function"
                        ))
                        .then(Commands.literal("function")
                            .executes(ctx -> applyExpression(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "expression"),
                                "function"
                            ))
                        )
                        .then(Commands.literal("parametric")
                            .executes(ctx -> applyExpression(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "expression"),
                                "parametric"
                            ))
                        )
                    )
                )
                .then(Commands.literal("clear")
                    .executes(ctx -> clearExpression(ctx.getSource()))
                )
            );
        });
    }

    private static int applyExpression(CommandSourceStack source,
                                        String expression, String mode)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ItemStack stack = source.getPlayerOrException().getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.literal("You must hold an item!"));
            return 0;
        }

        if ("parametric".equals(mode)) {
            String[] parts = expression.split("\\|", 3);
            if (parts.length != 3) {
                source.sendFailure(Component.literal("Parametric mode requires 3 expressions separated by | (e.g., cos(t)|sin(t)|t)"));
                return 0;
            }
            try {
                ExprParser.parseForT(parts[0].trim());
                ExprParser.parseForT(parts[1].trim());
                ExprParser.parseForT(parts[2].trim());
            } catch (ExprParseException e) {
                source.sendFailure(Component.literal("Invalid expression: " + e.getMessage()));
                return 0;
            }
            stack.remove(ModComponents.FUNCTION);
            stack.set(ModComponents.PARAMETRIC,
                new ParametricComponent(parts[0].trim(), parts[1].trim(), parts[2].trim()));
            source.sendSuccess(() ->
                Component.literal("Applied parametric trajectory: " + parts[0].trim() + "|" + parts[1].trim() + "|" + parts[2].trim()),
                false
            );
        } else {
            try {
                ExprParser.parse(expression.trim());
            } catch (ExprParseException e) {
                source.sendFailure(Component.literal("Invalid expression: " + e.getMessage()));
                return 0;
            }
            stack.remove(ModComponents.PARAMETRIC);
            stack.set(ModComponents.FUNCTION, new FunctionComponent(expression.trim()));
            source.sendSuccess(() ->
                Component.literal("Applied function trajectory: f(x) = " + expression.trim()),
                false
            );
        }
        return 1;
    }

    private static int clearExpression(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ItemStack stack = source.getPlayerOrException().getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.literal("You must hold an item!"));
            return 0;
        }

        boolean hadFunction = stack.has(ModComponents.FUNCTION);
        boolean hadParametric = stack.has(ModComponents.PARAMETRIC);

        stack.remove(ModComponents.FUNCTION);
        stack.remove(ModComponents.PARAMETRIC);

        if (hadFunction || hadParametric) {
            source.sendSuccess(() -> Component.literal("Cleared trajectory from held item"), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("Held item has no trajectory"));
            return 0;
        }
    }
}
