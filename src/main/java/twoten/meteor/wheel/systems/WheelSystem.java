package twoten.meteor.wheel.systems;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static net.minecraft.util.math.MathHelper.square;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.macros.Macros;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.systems.modules.movement.AutoWalk;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFly;
import meteordevelopment.meteorclient.systems.modules.render.Fullbright;
import meteordevelopment.meteorclient.systems.modules.render.StorageESP;
import meteordevelopment.meteorclient.systems.modules.render.Tracers;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import meteordevelopment.meteorclient.systems.modules.render.blockesp.BlockESP;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.RainbowColors;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import twoten.meteor.wheel.etc.MacroWheel;
import twoten.meteor.wheel.etc.ModuleWheel;
import twoten.meteor.wheel.etc.Wheel;

public class WheelSystem extends System<WheelSystem> {
    private enum WheelName {
        Above,
        Below,
        Hide
    }

    private class WheelScreen<T> extends Screen {
        private static double s() {
            return mc.getWindow().getScaleFactor();
        }

        private final Wheel<T> wheel;
        private final Keybind keybind;
        private final T[] items;
        private T selected;

        private double mx, my;

        private final double d;
        private final double centerSize = WheelSystem.this.centerSize.get();
        private final double wheelSize = WheelSystem.this.wheelSize.get();

        public WheelScreen(final Wheel<T> wheel) {
            super(Text.of(wheel.name.get()));
            this.wheel = wheel;

            this.keybind = wheel.keybind.get();
            this.items = wheel.items();

            this.d = 2.0 * Math.PI / items.length;

            MeteorClient.EVENT_BUS.subscribe(this);
        }

        @Override
        public void close() {
            super.close();
            act();
        }

        @Override
        public boolean keyReleased(final KeyInput input) {
            if (input.getKeycode() == keybind.getValue()) {
                close();
                return true;
            }
            return super.keyReleased(input);
        }

        @Override
        public void mouseMoved(final double mouseX, final double mouseY) {
            super.mouseMoved(mouseX, mouseY);
            final var s = s();
            mx = mouseX * s;
            my = mouseY * s;
            selected = selected();
        }

        @Override
        public boolean mouseClicked(final Click click, final boolean doubled) {
            if (WheelSystem.this.click.get()) {
                if (selected == null)
                    return false;
                switch (click.button()) {
                    case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> {
                        if (!configure.get())
                            return false;
                        super.close();
                        wheel.configure(selected);
                    }
                    case GLFW.GLFW_MOUSE_BUTTON_LEFT -> act();
                    default -> {
                        return false;
                    }
                }
                return true;
            }
            return super.mouseClicked(click, doubled);
        }

        @Override
        public boolean shouldPause() {
            return false;
        }

        @Override
        protected void init() {
            super.init();
            mouseMoved(width / 2.0, height / 2.0);
        }

        private int blurOptionValue = 0;

        @EventHandler
        private void onOpenScreen(final OpenScreenEvent event) {
            final var blurOption = mc.options.getMenuBackgroundBlurriness();
            if (event.screen == this) {
                this.blurOptionValue = blurOption.getValue();
                blurOption.setValue(0);
                return;
            }
            blurOption.setValue(this.blurOptionValue);
            MeteorClient.EVENT_BUS.unsubscribe(this);
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
            return;
        }

        @Override
        protected void renderDarkening(DrawContext context, int x, int y, int width, int height) {
            return;
        }

        private void act() {
            if (selected == null)
                return;
            wheel.act(selected);
        }

        private T selected() {
            final var s = s();
            final var x = mx - width * s / 2.0;
            final var y = my - height * s / 2.0;
            {
                final var l = square(x) + square(y);
                if (l <= square(centerSize) || l > square(wheelSize))
                    return null;
            }
            return items[(int) ((Math.PI * 2.0 + d / 2.0 + Math.atan2(x, y)) / d) % items.length];
        }

        @EventHandler(priority = EventPriority.LOWEST)
        private void onRender(final Render2DEvent event) {
            final var renderer = HudRenderer.INSTANCE;
            final var shadow = textShadow.get();
            final var s = s();
            final var cx = width / 2.0 * s;
            final var cy = height / 2.0 * s;
            renderer.begin(event.drawContext);
            for (var i = 0; i < items.length; i++) {
                final var sin = Math.sin(d * i - d / 2.0);
                final var cos = Math.cos(d * i - d / 2.0);
                final var color = lineColor.get();
                renderer.line(
                        cx + centerSize * sin, cy + centerSize * cos,
                        cx + wheelSize * sin, cy + wheelSize * cos,
                        color);
            }
            for (var i = 0; i < items.length; i++) {
                final var r = wheelSize / 2.0 + centerSize;
                final var x = cx + Math.sin(d * i) * r;
                final var y = cy + Math.cos(d * i) * r;
                final var item = items[i];
                wheel.render(item, item == selected, renderer, x, y);
            }
            if (wheelName.get() != WheelName.Hide)
                renderer.text(getTitle().getString(),
                        width / 2.0 * s - renderer.textWidth(getTitle().getString(), shadow) / 2.0,
                        cy + (wheelName.get() == WheelName.Above ? -1 : +1) * (centerSize + wheelSize + 5 * s)
                                - renderer.textHeight(shadow) / 2.0 * s,
                        nameColor.get(), shadow);
            renderer.end();
        }
    }

    public static List<Wheel<?>> defaultWheels() {
        return List.of(
                new ModuleWheel(Modules.get().getAll().stream().filter(i -> i.favorite).toList())
                        .name("Favorites")
                        .bind(Keybind.fromKey(GLFW.GLFW_KEY_R)),
                new MacroWheel(Macros.get().getAll())
                        .name("All Macros")
                        .bind(Keybind.fromKey(GLFW.GLFW_KEY_H)),
                new ModuleWheel(ElytraFly.class, AutoWalk.class, KillAura.class)
                        .name("Generic")
                        .bind(Keybind.fromKey(GLFW.GLFW_KEY_C)),
                new ModuleWheel(Xray.class, BlockESP.class, StorageESP.class, Tracers.class, Fullbright.class)
                        .name("Render")
                        .bind(Keybind.fromKey(GLFW.GLFW_KEY_G)));
    }

    public final List<Wheel<?>> wheels = new ArrayList<>();

    public final Settings settings = new Settings();

    private final SettingGroup sgControl = settings.createGroup("Control");

    private final Setting<Boolean> click = sgControl.add(new BoolSetting.Builder()
            .name("click")
            .description("Enable clicking in the screen.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> configure = sgControl.add(new BoolSetting.Builder()
            .name("configure")
            .description("Right click to configure item.")
            .defaultValue(true)
            .visible(click::get)
            .build());

    private final SettingGroup sgVisual = settings.createGroup("Appearance");

    private final Setting<WheelName> wheelName = sgVisual.add(new EnumSetting.Builder<WheelName>()
            .name("wheel-name")
            .description("How to display the wheel name.")
            .defaultValue(WheelName.Below)
            .build());

    private final Setting<Double> centerSize = sgVisual.add(new DoubleSetting.Builder()
            .name("center-size")
            .description("Radius of the area in the center of the screen.")
            .min(0)
            .sliderRange(0, 50)
            .defaultValue(15)
            .build());

    private final Setting<Double> wheelSize = sgVisual.add(new DoubleSetting.Builder()
            .name("wheel-size")
            .description("Radius of the wheel itself.")
            .min(0)
            .sliderRange(100, 400)
            .defaultValue(300)
            .build());

    public final Setting<SettingColor> nameColor = sgVisual.add(new ColorSetting.Builder()
            .name("name-color")
            .description("Color of the wheel name.")
            .defaultValue(new Color(200, 200, 200, 200))
            .visible(() -> wheelName.get() != WheelName.Hide)
            .build());

    private final Setting<SettingColor> lineColor = sgVisual.add(new ColorSetting.Builder()
            .name("line-color")
            .description("Color of separator lines.")
            .defaultValue(Color.WHITE.a(100))
            .build());

    public final Setting<SettingColor> textColor = sgVisual.add(new ColorSetting.Builder()
            .name("text-color")
            .description("Default text color.")
            .defaultValue(Color.WHITE.a(200))
            .build());

    public final Setting<SettingColor> accentColor = sgVisual.add(new ColorSetting.Builder()
            .name("accent-color")
            .description("The color used to highlight the selected item.")
            .defaultValue(new Color(255, 25, 25))
            .build());

    public final Setting<SettingColor> disabledColor = sgVisual.add(new ColorSetting.Builder()
            .name("disabled-color")
            .description("Color of disabled modules.")
            .defaultValue(Color.WHITE.a(100))
            .build());

    public final Setting<Boolean> textShadow = sgVisual.add(new BoolSetting.Builder()
            .name("text-shadow")
            .defaultValue(true)
            .build());

    private final SettingGroup sgOther = settings.createGroup("Other");

    public final Setting<Boolean> chatFeedback = sgOther.add(new BoolSetting.Builder()
            .name("chat-feedback")
            .defaultValue(true)
            .build());

    public WheelSystem() {
        super("wheel");

        RainbowColors.register(() -> {
            lineColor.get().update();
            textColor.get().update();
            accentColor.get().update();
            disabledColor.get().update();
        });

        if (isFirstInit)
            wheels.addAll(defaultWheels());
    }

    @Override
    public NbtCompound toTag() {
        final var tag = new NbtCompound();
        tag.put("settings", settings.toTag());
        tag.put("wheels", NbtUtils.listToTag(wheels));
        return tag;
    }

    @Override
    public WheelSystem fromTag(final NbtCompound tag) {
        settings.fromTag(tag.getCompoundOrEmpty("settings"));
        wheels.clear();
        wheels.addAll(NbtUtils.listFromTag(tag.getListOrEmpty("wheels"), Wheel::load));
        return this;
    }

    public void open(final Wheel<?> wheel) {
        if (mc.player == null
                || !Utils.canOpenGui()
                || wheel.items().length == 0)
            return;
        mc.setScreen(new WheelScreen<>(wheel));
    }

    @EventHandler
    private void onKey(final KeyEvent event) {
        if (event.action != KeyAction.Press)
            return;

        for (final var w : wheels)
            if (event.key() == w.keybind.get().getValue()) {
                open(w);
                break;
            }
    }
}
