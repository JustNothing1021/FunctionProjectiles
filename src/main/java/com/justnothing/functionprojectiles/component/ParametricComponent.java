package com.justnothing.functionprojectiles.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ParametricComponent(String expressionX, String expressionY, String expressionZ) {
    public static final Codec<ParametricComponent> CODEC = RecordCodecBuilder.create(inst ->
        inst.group(
            Codec.STRING.fieldOf("expression_x").forGetter(ParametricComponent::expressionX),
            Codec.STRING.fieldOf("expression_y").forGetter(ParametricComponent::expressionY),
            Codec.STRING.fieldOf("expression_z").forGetter(ParametricComponent::expressionZ)
        ).apply(inst, ParametricComponent::new)
    );
}
