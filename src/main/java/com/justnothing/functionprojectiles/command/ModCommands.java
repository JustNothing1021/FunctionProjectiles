package com.justnothing.functionprojectiles.command;

import com.justnothing.functionprojectiles.component.FunctionComponent;
import com.justnothing.functionprojectiles.component.ModComponents;
import com.justnothing.functionprojectiles.component.ParametricComponent;
import com.justnothing.functionprojectiles.expression.ExprParseException;
import com.justnothing.functionprojectiles.expression.ExprParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

public class ModCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("functionprojectiles")
                .requires(source -> source.hasPermissionLevel(0))
                .then(CommandManager.literal("apply")
                    .then(CommandManager.argument("expression", StringArgumentType.greedyString())
                        .executes(ctx -> applyExpression(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "expression"),
                            "function"
                        ))
                        .then(CommandManager.literal("function")
                            .executes(ctx -> applyExpression(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "expression"),
                                "function"
                            ))
                        )
                        .then(CommandManager.literal("parametric")
                            .executes(ctx -> applyExpression(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "expression"),
                                "parametric"
                            ))
                        )
                    )
                )
                .then(CommandManager.literal("clear")
                    .executes(ctx -> clearExpression(ctx.getSource()))
                )
            );
        });
    }

    private static int applyExpression(net.minecraft.server.command.ServerCommandSource source,
                                        String expression, String mode) {
        ItemStack stack = source.getPlayer().getMainHandStack();
        if (stack.isEmpty()) {
            source.sendError(Text.literal("You must hold an item!"));
            return 0;
        }

        if ("parametric".equals(mode)) {
            String[] parts = expression.split("\\|", 3);
            if (parts.length != 3) {
                source.sendError(Text.literal("Parametric mode requires 3 expressions separated by | (e.g., cos(t)|sin(t)|t)"));
                return 0;
            }
            try {
                ExprParser.parseForT(parts[0].trim());
                ExprParser.parseForT(parts[1].trim());
                ExprParser.parseForT(parts[2].trim());
            } catch (ExprParseException e) {
                source.sendError(Text.literal("Invalid expression: " + e.getMessage()));
                return 0;
            }
            stack.remove(ModComponents.FUNCTION);
            stack.set(ModComponents.PARAMETRIC,
                new ParametricComponent(parts[0].trim(), parts[1].trim(), parts[2].trim()));
            source.sendFeedback(() ->
                Text.literal("Applied parametric trajectory: " + parts[0].trim() + "|" + parts[1].trim() + "|" + parts[2].trim()),
                false
            );
        } else {
            try {
                ExprParser.parse(expression.trim());
            } catch (ExprParseException e) {
                source.sendError(Text.literal("Invalid expression: " + e.getMessage()));
                return 0;
            }
            stack.remove(ModComponents.PARAMETRIC);
            stack.set(ModComponents.FUNCTION, new FunctionComponent(expression.trim()));
            source.sendFeedback(() ->
                Text.literal("Applied function trajectory: f(x) = " + expression.trim()),
                false
            );
        }
        return 1;
    }

    private static int clearExpression(net.minecraft.server.command.ServerCommandSource source) {
        ItemStack stack = source.getPlayer().getMainHandStack();
        if (stack.isEmpty()) {
            source.sendError(Text.literal("You must hold an item!"));
            return 0;
        }

        boolean hadFunction = stack.contains(ModComponents.FUNCTION);
        boolean hadParametric = stack.contains(ModComponents.PARAMETRIC);

        stack.remove(ModComponents.FUNCTION);
        stack.remove(ModComponents.PARAMETRIC);

        if (hadFunction || hadParametric) {
            source.sendFeedback(() -> Text.literal("Cleared trajectory from held item"), false);
            return 1;
        } else {
            source.sendError(Text.literal("Held item has no trajectory"));
            return 0;
        }
    }
}
