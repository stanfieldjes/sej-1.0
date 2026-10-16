package dev.sej;

import de.maxhenkel.voicechat.VoicechatClient;
import de.maxhenkel.voicechat.natives.SpeexManager;
import de.maxhenkel.voicechat.voice.client.ClientManager;
import de.maxhenkel.voicechat.voice.client.ClientVoicechat;
import de.maxhenkel.voicechat.voice.client.microphone.MicrophoneManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Audio settings: game sound categories (some merged), plus Simple Voice Chat's
 * volume, mic level and devices. The Output device drives both the game and voice chat.
 */
public class AudioScreen extends Screen {

    private static final int FULL_WIDTH = 204;
    private static final int HALF_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SPACING = 24;
    private static final int SECTION_GAP = 8;

    /** Voice sliders: 100% sits in the middle, 200% at the right end. */
    private static final double VOICE_MAX_PERCENT = 200.0;
    /** Leftmost bit of the Voice Input slider that counts as "Auto" (automatic gain control). */
    private static final double AUTO_ZONE = 0.03;
    // Simple Voice Chat's mic gain limits, in dB.
    private static final double MIN_GAIN_DB = -40.0;
    private static final double MAX_GAIN_DB = 24.0;
    /** Room for device names inside a half-width button. */
    private static final int DEVICE_TEXT_WIDTH = HALF_WIDTH - 8;

    private final Screen parent;
    /** False until a click starts on this screen, so the release from the click that opened it is ignored. */
    private boolean receivedPress;

    public AudioScreen(Screen parent) {
        super(Text.translatable("sej.audio.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        GameOptions options = MinecraftClient.getInstance().options;

        int left = this.width / 2 - FULL_WIDTH / 2;
        int right = left + FULL_WIDTH - HALF_WIDTH;
        int y = this.height / 6 + 24;

        // Game sounds: music, environment, blocks on the left; players, hostiles, friendlies on the right.
        this.addDrawableChild(new CategorySlider(options, left, y, "sej.audio.music", SoundCategory.MUSIC));
        this.addDrawableChild(new CategorySlider(options, right, y, "sej.audio.players", SoundCategory.PLAYERS));
        y += SPACING;
        this.addDrawableChild(new CategorySlider(options, left, y, "sej.audio.environment", SoundCategory.WEATHER, SoundCategory.AMBIENT));
        this.addDrawableChild(new CategorySlider(options, right, y, "sej.audio.hostiles", SoundCategory.HOSTILE));
        y += SPACING;
        this.addDrawableChild(new CategorySlider(options, left, y, "sej.audio.blocks", SoundCategory.BLOCKS, SoundCategory.RECORDS));
        this.addDrawableChild(new CategorySlider(options, right, y, "sej.audio.friendlies", SoundCategory.NEUTRAL));
        y += SPACING + SECTION_GAP;

        // Voice: devices, with their levels directly underneath.
        this.addDrawableChild(this.outputDeviceButton(options, left, y));
        this.addDrawableChild(this.inputDeviceButton(right, y));
        y += SPACING;
        this.addDrawableChild(new VoiceOutputSlider(left, y));
        this.addDrawableChild(new VoiceInputSlider(right, y));
        y += SPACING + SECTION_GAP;

        this.addDrawableChild(ButtonWidget
                .builder(ScreenTexts.DONE, b -> this.close())
                .dimensions(left, y, FULL_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.receivedPress = true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!this.receivedPress) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void removed() {
        MinecraftClient.getInstance().options.write();
        // Saving any entry writes Simple Voice Chat's whole client config file.
        VoicechatClient.CLIENT_CONFIG.voiceChatVolume.save();
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(this.parent);
    }

    // ------------------------------------------------------------------ devices

    /** Game output and voice chat speaker, switched together. "" means the system default. */
    private CyclingButtonWidget<String> outputDeviceButton(GameOptions options, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        List<String> devices = new ArrayList<>();
        devices.add("");
        devices.addAll(client.getSoundManager().getSoundDevices());

        String current = options.getSoundDevice().getValue();
        if (!devices.contains(current)) current = "";

        return this.deviceButton(x, y, "sej.audio.output", devices, current, device -> {
            options.getSoundDevice().setValue(device); // vanilla reloads the game's sound system on change

            List<String> voiceSpeakers = de.maxhenkel.voicechat.voice.client.SoundManager.getAllSpeakers();
            String voiceDevice = voiceSpeakers.contains(device) ? device : "";
            VoicechatClient.CLIENT_CONFIG.speaker.set(voiceDevice).save();
            reloadVoiceAudio();
        });
    }

    private CyclingButtonWidget<String> inputDeviceButton(int x, int y) {
        List<String> devices = new ArrayList<>();
        devices.add("");
        devices.addAll(MicrophoneManager.deviceNames());

        String current = VoicechatClient.CLIENT_CONFIG.microphone.get();
        if (!devices.contains(current)) current = "";

        return this.deviceButton(x, y, "sej.audio.input", devices, current, device -> {
            VoicechatClient.CLIENT_CONFIG.microphone.set(device).save();
            reloadVoiceAudio();
        });
    }

    /** Cycles through devices on click (shift-click goes backwards). Long names are cut short; hover shows the full one. */
    private CyclingButtonWidget<String> deviceButton(int x, int y, String labelKey, List<String> devices, String current,
                                                     Consumer<String> onChange) {
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        Function<String, Text> fullName = device -> Text.translatable(labelKey, displayName(device));
        Function<String, Text> shortName = device -> fitToWidth(font, fullName.apply(device));

        return CyclingButtonWidget.builder(shortName)
                .values(devices)
                .initially(current)
                .omitKeyText()
                .tooltip(device -> Tooltip.of(fullName.apply(device)))
                .build(x, y, HALF_WIDTH, BUTTON_HEIGHT, Text.translatable(labelKey, ""), (button, device) -> onChange.accept(device));
    }

    private static Text displayName(String device) {
        if (device.isEmpty()) return Text.translatable("sej.audio.default_device");
        return Text.literal(de.maxhenkel.voicechat.voice.client.SoundManager.cleanDeviceName(device));
    }

    private static Text fitToWidth(TextRenderer font, Text text) {
        String string = text.getString();
        if (font.getWidth(string) <= DEVICE_TEXT_WIDTH) return text;
        String ellipsis = "…";
        return Text.literal(font.trimToWidth(string, DEVICE_TEXT_WIDTH - font.getWidth(ellipsis)) + ellipsis);
    }

    private static void reloadVoiceAudio() {
        ClientVoicechat voice = ClientManager.getClient();
        if (voice != null) voice.reloadAudio();
    }

    // ------------------------------------------------------------------ voice settings
    // Simple Voice Chat always has defaults for these; the fallbacks just make that explicit.

    private static double voiceVolume() {
        Double volume = VoicechatClient.CLIENT_CONFIG.voiceChatVolume.get();
        return volume != null ? volume : 1.0;
    }

    private static double micGainDb() {
        Double gain = VoicechatClient.CLIENT_CONFIG.microphoneGain.get();
        return gain != null ? gain : 0.0;
    }

    private static boolean autoGainOn() {
        return !Boolean.FALSE.equals(VoicechatClient.CLIENT_CONFIG.agc.get());
    }

    // ------------------------------------------------------------------ sliders

    /** One slider for one or more vanilla sound categories (e.g. Weather + Ambient). */
    private static class CategorySlider extends SliderWidget {
        private final GameOptions options;
        private final String labelKey;
        private final SoundCategory[] categories;

        CategorySlider(GameOptions options, int x, int y, String labelKey, SoundCategory... categories) {
            super(x, y, HALF_WIDTH, BUTTON_HEIGHT, Text.empty(), options.getSoundVolumeOption(categories[0]).getValue());
            this.options = options;
            this.labelKey = labelKey;
            this.categories = categories;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            Text amount = this.value <= 0.0
                    ? ScreenTexts.OFF
                    : Text.literal(Math.round(this.value * 100) + "%");
            this.setMessage(Text.translatable(this.labelKey, amount));
        }

        @Override
        protected void applyValue() {
            for (SoundCategory category : this.categories) {
                this.options.getSoundVolumeOption(category).setValue(this.value);
            }
        }
    }

    /** How loud other players are. 0-200%, 100% in the middle. */
    private static class VoiceOutputSlider extends SliderWidget {
        VoiceOutputSlider(int x, int y) {
            super(x, y, HALF_WIDTH, BUTTON_HEIGHT, Text.empty(),
                    MathHelper.clamp(voiceVolume() * 100.0 / VOICE_MAX_PERCENT, 0.0, 1.0));
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            long percent = Math.round(this.value * VOICE_MAX_PERCENT);
            Text amount = percent <= 0 ? ScreenTexts.OFF : Text.literal(percent + "%");
            this.setMessage(Text.translatable("sej.audio.voice_output", amount));
        }

        @Override
        protected void applyValue() {
            VoicechatClient.CLIENT_CONFIG.voiceChatVolume.set(this.value * VOICE_MAX_PERCENT / 100.0);
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            VoicechatClient.CLIENT_CONFIG.voiceChatVolume.save();
        }
    }

    /**
     * Mic level. All the way left = Auto (automatic gain control).
     * Otherwise 0-200%, 100% in the middle, stored as Simple Voice Chat's gain in dB.
     */
    private static class VoiceInputSlider extends SliderWidget {
        VoiceInputSlider(int x, int y) {
            super(x, y, HALF_WIDTH, BUTTON_HEIGHT, Text.empty(), initialValue());
            this.updateMessage();
        }

        private static boolean autoAvailable() {
            return SpeexManager.canUseAgc();
        }

        private static double initialValue() {
            if (autoAvailable() && autoGainOn()) return 0.0;
            double percent = 100.0 * Math.pow(10.0, micGainDb() / 20.0);
            double min = autoAvailable() ? AUTO_ZONE : 0.0;
            return MathHelper.clamp(percent / VOICE_MAX_PERCENT, min, 1.0);
        }

        private boolean isAuto() {
            return autoAvailable() && this.value < AUTO_ZONE;
        }

        @Override
        protected void updateMessage() {
            Text amount = this.isAuto()
                    ? Text.translatable("sej.audio.auto")
                    : Text.literal(Math.round(this.value * VOICE_MAX_PERCENT) + "%");
            this.setMessage(Text.translatable("sej.audio.voice_input", amount));
        }

        @Override
        protected void applyValue() {
            if (this.isAuto()) {
                VoicechatClient.CLIENT_CONFIG.agc.set(true);
                return;
            }
            double percent = Math.max(1.0, this.value * VOICE_MAX_PERCENT);
            double gainDb = MathHelper.clamp(20.0 * Math.log10(percent / 100.0), MIN_GAIN_DB, MAX_GAIN_DB);
            VoicechatClient.CLIENT_CONFIG.agc.set(false);
            VoicechatClient.CLIENT_CONFIG.microphoneGain.set(gainDb);
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            VoicechatClient.CLIENT_CONFIG.microphoneGain.save();
        }
    }
}