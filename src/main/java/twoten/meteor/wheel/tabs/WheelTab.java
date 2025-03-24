package twoten.meteor.wheel.tabs;

import static meteordevelopment.meteorclient.MeteorClient.mc;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import meteordevelopment.meteorclient.MeteorClient;
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
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.movement.Flight;
import meteordevelopment.meteorclient.systems.modules.movement.Jesus;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
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

public class WheelTab extends Tab {
    public static class TabScreen extends WindowTabScreen {
        private final Wheel sys = Wheel.get();

        public TabScreen(final GuiTheme theme, final Tab tab) {
            super(theme, tab);
            sys.settings.onActivated();
        }

        @Override
        public void initWidgets() {
            add(theme.settings(sys.settings)).expandX();
        }

        @Override
        public boolean toClipboard() {
            return NbtUtils.toClipboard("wheel-settings", sys.settings.toTag());
        }

        @Override
        public boolean fromClipboard() {
            final NbtCompound clipboard = NbtUtils.fromClipboard(sys.settings.toTag());

            if (clipboard != null) {
                sys.settings.fromTag(clipboard);
                return true;
            }

            return false;
        }
    }

    private static class Wheel extends System<Wheel> {
        private class MapScreen extends Screen {
            private static double s() {
                return mc.getWindow().getScaleFactor();
            }

            private final List<Module> modules = Wheel.this.modules.get();
            private final int l = modules.size();
            private final double d = 1d / l * 2 * Math.PI;
            private final boolean shadow = textShadow.get();
            private final double closeR = centerSize.get();
            private final double wheelR = wheelSize.get();
            private Module selected;

            private double mx, my;

            public MapScreen() {
                super(Text.of(getName()));
                MeteorClient.EVENT_BUS.subscribe(this);
            }

            @Override
            public void close() {
                MeteorClient.EVENT_BUS.unsubscribe(this);
                super.close();
                if (selected == null)
                    return;
                selected.toggle();
            }

            @Override
            public boolean keyReleased(final int keyCode, final int scanCode, final int modifiers) {
                if (keyCode == keybind.get().getValue()) {
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
            protected void init() {
                super.init();
                mouseMoved(width / 2d, height / 2d);
            }

            private Module selected() {
                final var s = s();
                final var v = new Vec2f((float) (mx - width * s / 2f), (float) (my - height * s / 2f));
                {
                    final var d = v.length();
                    if (d <= closeR || d > wheelR)
                        return null;
                }
                return modules.get((int) ((Math.PI * 2 + d / 2 + Math.atan2(v.x, v.y)) / d) % modules.size());
            }

            @EventHandler
            private void onRender(final Render2DEvent event) {
                final var r = HudRenderer.INSTANCE;
                final var s = s();
                final var cx = width / 2 * s;
                final var cy = height / 2 * s;
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
                            cx + (closeR + wheelR / 2) * Math.sin(d * i) - r.textWidth(m.title, shadow) / 2, // TODO: proper text centering
                            cy + (closeR + wheelR / 2) * Math.cos(d * i),
                            m == selected ? hoverColor.get() : textColor.get(),
                            shadow);
                }
                r.end();
            }
        }

        public static Wheel get() {
            return Systems.get(Wheel.class);
        }

        private final Settings settings = new Settings();

        // TODO: should be able to make multiple wheels
        private final SettingGroup sgGeneral = settings.getDefaultGroup();

        private final Setting<List<Module>> modules = sgGeneral.add(new ModuleListSetting.Builder()
                .name("modules")
                .description("Select modules to put in quick access.")
                .defaultValue(Flight.class, Jesus.class, Xray.class)
                .build());

        private final Setting<Keybind> keybind = sgGeneral.add(new KeybindSetting.Builder()
                .name("bind")
                .description("Key to hold.")
                .defaultValue(Keybind.fromKey(GLFW.GLFW_KEY_R))
                .build());

        private final SettingGroup sgVisual = settings.createGroup("Appearance");

        private final Setting<Double> centerSize = sgVisual.add(new DoubleSetting.Builder()
                .name("center-size")
                .description("Radius of the area in the center of the screen is.")
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
                .description("The color for separator lines.")
                .defaultValue(Color.WHITE.a(100))
                .build());

        private final Setting<SettingColor> textColor = sgVisual.add(new ColorSetting.Builder()
                .name("text-color")
                .description("The color for the module title.")
                .defaultValue(Color.WHITE.a(200))
                .build());

        private final Setting<SettingColor> hoverColor = sgVisual.add(new ColorSetting.Builder()
                .name("hover-color")
                .description("The color to use to highlight the selected module.")
                .defaultValue(new Color(255, 25, 25))
                .build());

        private final Setting<Boolean> textShadow = sgVisual.add(new BoolSetting.Builder()
                .name("text-shadow")
                .description("Whether to use shading on text.")
                .defaultValue(true)
                .build());

        public Wheel() {
            super("wheel");

            RainbowColors.register(() -> {
                lineColor.get().update();
                textColor.get().update();
                hoverColor.get().update();
            });
        }

        @Override
        public NbtCompound toTag() {
            final NbtCompound tag = new NbtCompound();

            tag.putInt("__version__", 1);

            tag.put("settings", settings.toTag());

            return tag;
        }

        @Override
        public Wheel fromTag(final NbtCompound tag) {
            if (!tag.contains("__version__"))
                return this;

            settings.fromTag(tag.getCompound("settings"));

            return this;
        }

        @EventHandler
        private void onKey(final KeyEvent event) {
            if (event.action != KeyAction.Press)
                return;
            if (event.key == keybind.get().getValue())
                open();
        }

        private void open() {
            if (mc.player == null)
                return;
            if (!Utils.canOpenGui())
                return;
            if (modules.get().isEmpty())
                return;
            mc.setScreen(new MapScreen());
        }
    }

    public WheelTab() {
        super("Wheel");
        Systems.add(new Wheel());
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
