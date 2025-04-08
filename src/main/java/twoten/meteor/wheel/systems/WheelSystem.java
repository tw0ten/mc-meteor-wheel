package twoten.meteor.wheel.systems;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static net.minecraft.util.math.MathHelper.square;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.ModuleListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Module;
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

// TODO: marco wheel
public class WheelSystem extends System<WheelSystem> {
    public static class Wheel {
        public final Settings settings = new Settings();

        private final SettingGroup sgGeneral = settings.getDefaultGroup();

        public final Setting<List<Module>> modules = sgGeneral.add(new ModuleListSetting.Builder()
                .name("modules")
                .description("Select modules to put in quick access.")
                .build());

        public final Setting<Keybind> keybind = sgGeneral.add(new KeybindSetting.Builder()
                .name("bind")
                .description("Key to hold.")
                .build());

        public Wheel() {
        }

        public Wheel(final List<Module> modules) {
            this.modules.set(modules);
        }

        public Wheel(final NbtCompound settings) {
            this.settings.fromTag(settings);
        }

        public Wheel bind(final Keybind i) {
            this.keybind.set(i);
            return this;
        }
    }

    private class WheelScreen extends Screen {
        private static double s() {
            return mc.getWindow().getScaleFactor();
        }

        private final Keybind keybind;

        private final List<Module> modules;
        private Module selected;
        private final int l;

        private final double d;
        private double mx, my;
        private final boolean shadow = textShadow.get();

        private final double closeR = centerSize.get();
        private final double wheelR = wheelSize.get();

        public WheelScreen(final Wheel w) {
            super(Text.of(getName()));

            this.keybind = w.keybind.get();
            this.modules = w.modules.get();

            this.l = modules.size();
            this.d = 2.0 * Math.PI / l;

            MeteorClient.EVENT_BUS.subscribe(this);
        }

        @Override
        public void close() {
            super.close();
            act();
        }

        @Override
        public boolean keyReleased(final int keyCode, final int scanCode, final int modifiers) {
            if (keyCode == keybind.getValue()) {
                close();
                return true;
            }

            return super.keyReleased(keyCode, scanCode, modifiers);
        }

        @Override
        public void render(final DrawContext context, final int mouseX, final int mouseY, final float delta) {
            return;
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
        public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
            if (click.get()) {
                switch (button) {
                    case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> {
                        if (selected == null || !configure.get())
                            return false;
                        super.close();
                        mc.setScreen(GuiThemes.get().moduleScreen(selected));
                    }
                    case GLFW.GLFW_MOUSE_BUTTON_LEFT -> act();
                    default -> {
                        return false;
                    }
                }
                return selected != null;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean shouldPause() {
            return super.shouldPause() && false;
        }

        @Override
        protected void init() {
            super.init();
            mouseMoved(width / 2.0, height / 2.0);
        }

        @EventHandler
        private void onOpenScreen(final OpenScreenEvent event) {
            if (event.screen == this)
                return;
            MeteorClient.EVENT_BUS.unsubscribe(this);
        }

        private void act() {
            if (selected == null)
                return;

            selected.toggle();

            if (chatFeedback.get())
                selected.sendToggledMsg();
        }

        private Module selected() {
            final var s = s();
            final var x = mx - width * s / 2.0;
            final var y = my - height * s / 2.0;
            {
                final var l = square(x) + square(y);
                if (l <= square(closeR) || l > square(wheelR))
                    return null;
            }
            return modules.get((int) ((Math.PI * 2.0 + d / 2.0 + Math.atan2(x, y)) / d) % modules.size());
        }

        @EventHandler(priority = EventPriority.LOWEST)
        private void onRender(final Render2DEvent event) {
            final var r = HudRenderer.INSTANCE;
            final var s = s();
            final var cx = width / 2.0 * s;
            final var cy = height / 2.0 * s;
            r.begin(event.drawContext);
            for (var i = 0; i < l; i++) {
                final var module = modules.get(i);
                {
                    final var sin = Math.sin(d * i - d / 2.0);
                    final var cos = Math.cos(d * i - d / 2.0);
                    r.line(cx + closeR * sin, cy + closeR * cos, cx + wheelR * sin, cy + wheelR * cos, lineColor.get());
                }
                final var width = r.textWidth(module.title, shadow);
                final var x = cx + (closeR + wheelR / 2.0) * Math.sin(d * i) - width / 2.0;
                final var y = cy + (closeR + wheelR / 2.0) * Math.cos(d * i);
                r.text(module.title, x, y,
                        module == selected ? hoverColor.get()
                                : module.isActive()
                                        ? textColor.get()
                                        : disabledColor.get(),
                        shadow);
            }
            r.end();
        }
    }

    public static WheelSystem get() {
        return Systems.get(WheelSystem.class);
    }

    public final List<Wheel> wheels = new ArrayList<>();

    public final Settings settings = new Settings();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Keybind> favorites = sgGeneral.add(new KeybindSetting.Builder()
            .name("favorites")
            .description("A wheel for modules you have marked as favorite.")
            .defaultValue(Keybind.fromKey(GLFW.GLFW_KEY_R))
            .build());

    private final SettingGroup sgControl = settings.createGroup("Control");

    private final Setting<Boolean> click = sgControl.add(new BoolSetting.Builder()
            .name("click")
            .description("Enable clicking in the screen.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> configure = sgControl.add(new BoolSetting.Builder()
            .name("configure")
            .description("Right click to open module settings.")
            .defaultValue(true)
            .visible(click::get)
            .build());

    private final SettingGroup sgVisual = settings.createGroup("Appearance");

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

    private final Setting<SettingColor> lineColor = sgVisual.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The color of separator lines.")
            .defaultValue(Color.WHITE.a(100))
            .build());

    private final Setting<SettingColor> textColor = sgVisual.add(new ColorSetting.Builder()
            .name("text-color")
            .description("The color of module titles.")
            .defaultValue(Color.WHITE.a(200))
            .build());

    private final Setting<SettingColor> disabledColor = sgVisual.add(new ColorSetting.Builder()
            .name("disabled-color")
            .description("The color of disabled modules.")
            .defaultValue(Color.WHITE.a(100))
            .build());

    private final Setting<SettingColor> hoverColor = sgVisual.add(new ColorSetting.Builder()
            .name("hover-color")
            .description("The color used to highlight the selected module.")
            .defaultValue(new Color(255, 25, 25))
            .build());

    private final Setting<Boolean> textShadow = sgVisual.add(new BoolSetting.Builder()
            .name("text-shadow")
            .description("Whether to use shading on text.")
            .defaultValue(true)
            .build());

    private final SettingGroup sgOther = settings.createGroup("Other");

    private final Setting<Boolean> chatFeedback = sgOther.add(new BoolSetting.Builder()
            .name("chat-feedback")
            .description("Will send \"Toggled on/off\" messages.")
            .visible(Config.get().chatFeedback::get)
            .defaultValue(true)
            .build());

    public WheelSystem() {
        super("wheel");

        RainbowColors.register(() -> {
            lineColor.get().update();
            textColor.get().update();
            hoverColor.get().update();
            disabledColor.get().update();
        });

        if (isFirstInit)
            defaultWheels();
    }

    public Wheel favorites() {
        return new Wheel(Modules.get()
                .getAll().stream()
                .filter(i -> i.favorite)
                .toList());
    }

    @Override
    public NbtCompound toTag() {
        final var tag = new NbtCompound();

        tag.putInt("__version__", 1);

        tag.put("settings", settings.toTag());

        final var size = wheels.size();
        tag.putInt("size", size);
        for (var i = 0; i < size; i++)
            tag.put("w-" + i, wheels.get(i).settings.toTag());

        return tag;
    }

    @Override
    public WheelSystem fromTag(final NbtCompound tag) {
        if (!tag.contains("__version__"))
            return this;

        settings.fromTag(tag.getCompound("settings"));

        final var size = tag.getInt("size");
        for (var i = 0; i < size; i++)
            wheels.add(new Wheel(tag.getCompound("w-" + i)));

        return this;
    }

    public void defaultWheels() {
        wheels.clear();
        wheels.add(new Wheel(new ModuleListSetting.Builder()
                .defaultValue(ElytraFly.class, AutoWalk.class, KillAura.class)
                .build().get())
                .bind(Keybind.fromKey(GLFW.GLFW_KEY_C)));
        wheels.add(new Wheel(new ModuleListSetting.Builder()
                .defaultValue(Xray.class, BlockESP.class, StorageESP.class, Tracers.class, Fullbright.class)
                .build().get())
                .bind(Keybind.fromKey(GLFW.GLFW_KEY_G)));
    }

    public void open(final Wheel w) {
        if (mc.player == null)
            return;
        if (!Utils.canOpenGui())
            return;
        if (w.modules.get().isEmpty())
            return;

        mc.setScreen(new WheelScreen(w));
    }

    @EventHandler
    private void onKey(final KeyEvent event) {
        if (event.action != KeyAction.Press)
            return;

        if (event.key == favorites.get().getValue()) {
            open(favorites().bind(favorites.get()));
            return;
        }

        for (final var w : wheels)
            if (event.key == w.keybind.get().getValue()) {
                open(w);
                break;
            }
    }
}
