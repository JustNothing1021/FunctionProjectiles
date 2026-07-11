package com.justnothing.functionprojectiles.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record FunctionComponent(String expression) {
    public static final Codec<FunctionComponent> CODEC = RecordCodecBuilder.create(inst ->
        inst.group(Codec.STRING.fieldOf("expression").forGetter(FunctionComponent::expression))
            .apply(inst, FunctionComponent::new)
    );
}
