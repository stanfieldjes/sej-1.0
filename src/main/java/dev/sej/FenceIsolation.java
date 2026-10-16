package dev.sej;

import net.minecraft.state.property.BooleanProperty;

public final class FenceIsolation {
    public static final BooleanProperty ISOLATED = BooleanProperty.of("isolated");

    private FenceIsolation() {}
}