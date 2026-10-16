package dev.sej.mixin.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.WorldCreator;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.world.Difficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin extends Screen {

    @Shadow public abstract WorldCreator getWorldCreator();

    protected CreateWorldScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void sej$replaceUi(CallbackInfo ci) {
        this.clearChildren();
        this.setFocused(null);

        WorldCreator creator = this.getWorldCreator();

        boolean creative = creator.getGameMode() == WorldCreator.Mode.CREATIVE;
        creator.setGameMode(creative ? WorldCreator.Mode.CREATIVE : WorldCreator.Mode.SURVIVAL);
        creator.setCheatsEnabled(creative);

        int left = this.width / 2 - 100;

        TextFieldWidget sej$nameField = new TextFieldWidget(
                this.textRenderer, left, 60, 200, 20, Text.translatable("selectWorld.enterName"));
        sej$nameField.setMaxLength(64);
        sej$nameField.setText(creator.getWorldName());
        sej$nameField.setChangedListener(creator::setWorldName);
        this.addDrawableChild(sej$nameField);

        TextFieldWidget sej$seedField = new TextFieldWidget(
                this.textRenderer, left, 100, 200, 20, Text.translatable("selectWorld.enterSeed"));
        sej$seedField.setMaxLength(128);
        sej$seedField.setText(creator.getSeed());
        sej$seedField.setChangedListener(creator::setSeed);
        this.addDrawableChild(sej$seedField);

        int rowY = 140;
        int halfWidth = 98;

        this.addDrawableChild(CyclingButtonWidget
                .<WorldCreator.Mode>builder(mode -> Text.translatable(
                        mode == WorldCreator.Mode.CREATIVE
                                ? "sej.world.creative"
                                : "sej.world.survival"))
                .values(WorldCreator.Mode.SURVIVAL, WorldCreator.Mode.CREATIVE)
                .initially(creative ? WorldCreator.Mode.CREATIVE : WorldCreator.Mode.SURVIVAL)
                .omitKeyText()
                .build(left, rowY, halfWidth, 20, Text.translatable("selectWorld.gameMode"),
                        (button, mode) -> {
                            creator.setGameMode(mode);
                            creator.setCheatsEnabled(mode == WorldCreator.Mode.CREATIVE);
                        }));

        this.addDrawableChild(CyclingButtonWidget
                .builder(Difficulty::getTranslatableName)
                .values(Difficulty.PEACEFUL, Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD)
                .initially(creator.getDifficulty())
                .omitKeyText()
                .build(left + 102, rowY, halfWidth, 20, Text.translatable("options.difficulty"),
                        (button, difficulty) -> creator.setDifficulty(difficulty)));

        this.addDrawableChild(ButtonWidget
                .builder(Text.translatable("selectWorld.create"),
                        b -> ((CreateWorldScreenInvoker) this).sej$createLevel())
                .dimensions(this.width / 2 - 102, this.height - 40, 100, 20)
                .build());

        this.addDrawableChild(ButtonWidget
                .builder(Text.translatable("gui.cancel"), b -> this.close())
                .dimensions(this.width / 2 + 2, this.height - 40, 100, 20)
                .build());

        this.setInitialFocus(sej$nameField);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void sej$renderLabels(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        context.drawTextWithShadow(this.textRenderer, Text.translatable("sej.selectWorld.enterName"),
                this.width / 2 - 100, 48, 0xA0A0A0);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("sej.selectWorld.enterSeed"),
                this.width / 2 - 100, 88, 0xA0A0A0);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("sej.selectWorld.gameMode"),
                this.width / 2 - 100, 128, 0xA0A0A0);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("sej.options.difficulty"),
                this.width / 2 + 2, 128, 0xA0A0A0);
    }
}