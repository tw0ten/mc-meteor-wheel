package twoten.meteor.wheel.tabs;

import static meteordevelopment.meteorclient.MeteorClient.mc;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
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
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec2f;
import twoten.meteor.wheel.Wheel;

public class WheelTab extends Tab {
    public static class TabScreen extends WindowTabScreen {
        private final WheelSys sys = WheelSys.get();

        public TabScreen(final GuiTheme theme, final Tab tab) {
            super(theme, tab);
            sys.settings.onActivated();
        }

        @Override
        public void initWidgets() {
            add(theme.settings(sys.settings)).expandX();

            add(theme.horizontalSeparator()).expandX();

            {
                final var table = add(theme.table()).expandX().widget();

                table.add(theme.plus()).widget().action = () -> {
                    sys.wheels.addFirst(new Wheel(List.of()));
                    save();
                };
                table.add(theme.button("Reset")).widget().action = () -> {
                    sys.defaultWheels();
                    save();
                };
            }

            add(theme.horizontalSeparator()).expandX();

            final var table = add(theme.table()).expandX().widget();
            for (var i = 0; i < sys.wheels.size(); i++) {
                final var w = sys.wheels.get(i);

                // TODO: a sep screen like with profiles

                table.add(theme.label("Wheel #" + (i + 1))).expandCellX();
                table.add(theme.minus()).widget().action = () -> {
                    sys.wheels.remove(w);
                    save();
                };
                table.row();

                table.add(theme.settings(w.settings)).expandX();
                table.row();

                table.add(theme.horizontalSeparator()).expandX();
                table.row();
            }
        }

        @Override
        public boolean toClipboard() {
            return NbtUtils.toClipboard("wheel", sys.toTag());
        }

        @Override
        public boolean fromClipboard() {
            final NbtCompound clipboard = NbtUtils.fromClipboard(sys.toTag());

            if (clipboard != null) {
                sys.fromTag(clipboard);
                return true;
            }

            return false;
        }

        private void save() {
            sys.save();
            reload();
        }
    }

    private static class WheelSys extends System<WheelSys> {
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
                this.d = 1d / l * 2 * Math.PI;
            }

            @Override
            public void close() {
                super.close();
                if (selected == null)
                    return;

                selected.toggle();

                if (chatFeedback.get())
                    selected.sendToggledMsg();
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
            public boolean shouldPause() {
                return super.shouldPause() && false;
            }

            @Override
            protected void init() {
                super.init();
                mouseMoved(width / 2d, height / 2d);
            }

            private Module selected() {
                final var s = s();
                final var v = new Vec2f((float) (mx - width * s / 2d), (float) (my - height * s / 2d));
                {
                    final var d = v.length();
                    if (d <= closeR || d > wheelR)
                        return null;
                }
                return modules.get((int) ((Math.PI * 2 + d / 2d + Math.atan2(v.x, v.y)) / d) % modules.size());
            }

            @EventHandler
            private void onRender(final Render2DEvent event) {
                final var r = HudRenderer.INSTANCE;
                final var s = s();
                final var cx = width / 2d * s;
                final var cy = height / 2d * s;
                r.begin(event.drawContext);
                for (var i = 0; i < l; i++) {
                    {
                        final var sin = Math.sin(d * i - d / 2);
                        final var cos = Math.cos(d * i - d / 2);
                        r.line(cx + closeR * sin,
                                cy + closeR * cos,
                                cx + wheelR * sin,
                                cy + wheelR * cos,
                                lineColor.get());
                    }
                    final var m = modules.get(i);
                    r.text(m.title,
                            cx + (closeR + wheelR / 2) * Math.sin(d * i) - r.textWidth(m.title, shadow) / 2d,
                            cy + (closeR + wheelR / 2) * Math.cos(d * i),
                            m == selected ? hoverColor.get() : textColor.get(),
                            shadow);
                }
                r.end();
            }
        }

        public static WheelSys get() {
            return Systems.get(WheelSys.class);
        }

        private final List<Wheel> wheels = new ArrayList<>();

        private final Settings settings = new Settings();

        private final SettingGroup sgGeneral = settings.getDefaultGroup();

        private final Setting<Keybind> favorites = sgGeneral.add(new KeybindSetting.Builder()
                .name("favorites")
                .description("A wheel for modules you have marked as favorite.")
                .defaultValue(Keybind.fromKey(GLFW.GLFW_KEY_R))
                .build());

        private final Setting<Boolean> chatFeedback = sgGeneral.add(new BoolSetting.Builder()
                .name("chat-feedback")
                .description("Will send \"Toggled on/off\" messages.")
                .visible(Config.get().chatFeedback::get)
                .defaultValue(true)
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
                .defaultValue(200)
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

        public WheelSys() {
            super("wheel");

            RainbowColors.register(() -> {
                lineColor.get().update();
                textColor.get().update();
                hoverColor.get().update();
            });

            if (isFirstInit)
                defaultWheels();
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
        public WheelSys fromTag(final NbtCompound tag) {
            if (!tag.contains("__version__"))
                return this;

            settings.fromTag(tag.getCompound("settings"));

            final var size = tag.getInt("size");
            for (var i = 0; i < size; i++)
                wheels.add(new Wheel(tag.getCompound("w-" + i)));

            return this;
        }

        private void defaultWheels() {
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

        @EventHandler
        private void onKey(final KeyEvent event) {
            if (event.action != KeyAction.Press)
                return;

            if (event.key == favorites.get().getValue()) {
                open(Wheel.favorites(favorites.get()));
                return;
            }

            for (final var w : wheels)
                if (event.key == w.keybind.get().getValue()) {
                    open(w);
                    break;
                }
        }

        @EventHandler
        private void onOpenScreen(final OpenScreenEvent event) {
            if (mc.currentScreen instanceof final WheelScreen s)
                MeteorClient.EVENT_BUS.unsubscribe(s);
        }

        private void open(final Wheel w) {
            if (mc.player == null)
                return;
            if (!Utils.canOpenGui())
                return;
            if (w.modules.get().isEmpty())
                return;

            final var s = new WheelScreen(w);
            MeteorClient.EVENT_BUS.subscribe(s);
            mc.setScreen(s);
        }
    }

    public WheelTab() {
        super("Wheel");
        Systems.add(new WheelSys());
    }

    @Override
    public TabScreen createScreen(final GuiTheme theme) {
        return new TabScreen(theme, this);
    }

    @Override
    public boolean isScreen(final Screen screen) {
        return screen instanceof TabScreen;
    }
}
