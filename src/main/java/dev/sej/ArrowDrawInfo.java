package dev.sej;

public final class ArrowDrawInfo {
    private ArrowDrawInfo() {}

    public static final int STAGE_1_TICKS = 20;
    public static final int STAGE_DELAY_TICKS = 0;
    public static final int STAGE_2_TICKS = 20;

    public static final float STAGE_1_VELOCITY = 2.0f;
    public static final float STAGE_2_VELOCITY = 4.0f;

    public static final float STAGE_1_DAMAGE = 5.0f;
    public static final float STAGE_2_DAMAGE = 10.0f;

    public static final float MAX_ZOOM = 0.25f;
    public static final float ZOOM_EXPONENT = 1.5f;

    public static float lastPotentialDamage = 5.0f;
    public static float lastIntendedVelocity = 2.0f;
}