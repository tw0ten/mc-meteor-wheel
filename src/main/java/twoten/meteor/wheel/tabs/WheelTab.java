package twoten.meteor.wheel.tabs;

import static meteordevelopment.meteorclient.MeteorClient.mc;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.render.prompts.YesNoPrompt;
import net.minecraft.client.gui.screen.Screen;
import twoten.meteor.wheel.etc.Wheel;
import twoten.meteor.wheel.systems.WheelSystem;

public class WheelTab extends Tab {
    public static class TabScreen extends WindowTabScreen {
        private class AddWheelScreen extends WindowScreen {
            public AddWheelScreen(final GuiTheme theme) {
                super(theme, "Select Wheel Type");
            }

            @Override
            public void initWidgets() {
                for (final var e : Wheel.Type.values())
                    add(theme.button(e.name())).expandX().widget().action = () -> {
                        close();
                        final var wheel = e.create();
                        Systems.get(WheelSystem.class).wheels.addFirst(wheel);
                        mc.setScreen(new EditWheelScreen(theme, wheel));
                    };
            }
        }

        private class EditWheelScreen extends WindowScreen {
            private WContainer settingsContainer;
            private final Wheel<?> wheel;

            public EditWheelScreen(final GuiTheme theme, final Wheel<?> wheel) {
                super(theme, "Edit Wheel (" + wheel.type() + ")");
                this.wheel = wheel;
            }

            @Override
            public void initWidgets() {
                settingsContainer = add(theme.verticalList()).expandX().widget();
                settingsContainer.add(theme.settings(wheel.settings)).expandX();

                add(theme.horizontalSeparator()).expandX();

                add(theme.button("Done")).expandX().widget().action = this::close;
            }

            @Override
            public void tick() {
                wheel.settings.tick(settingsContainer, theme);
            }

            @Override
            protected void onClosed() {
                super.onClosed();
                save();
            }
        }

        private final WheelSystem sys = Systems.get(WheelSystem.class);

        private WContainer settings;

        public TabScreen(final GuiTheme theme, final Tab tab) {
            super(theme, tab);
            sys.settings.onActivated();
        }

        @Override
        public void initWidgets() {
            final var section = add(theme.section("Wheels", true)).expandX().widget();

            final var table = section.add(theme.table()).expandX().widget();

            table.add(theme.label("Name")).expandCellX();
            table.add(theme.label("Items")).expandCellX();
            table.add(theme.label("Bind")).expandCellX();

            table.add(theme.button(GuiRenderer.RESET)).widget().action = YesNoPrompt.create(theme, this)
                    .title("Wheel Settings")
                    .message("Reset wheels list?")
                    .onYes(() -> {
                        sys.wheels.clear();
                        sys.wheels.addAll(WheelSystem.defaultWheels());
                        save();
                    })::show;

            table.add(theme.plus()).widget().action = () -> mc.setScreen(new AddWheelScreen(theme));

            table.row();
            table.add(theme.horizontalSeparator()).expandX();
            table.row();

            for (var i = 0; i < sys.wheels.size(); i++) {
                final var w = sys.wheels.get(i);

                table.add(theme.label(w.name.get())).expandCellX();
                table.add(theme.label(w.items().length + " " + w.type())).expandCellX();
                table.add(theme.label(w.keybind.get().toString())).expandCellX();

                table.add(theme.button(GuiRenderer.EDIT)).widget().action = () -> mc
                        .setScreen(new EditWheelScreen(theme, w));
                table.add(theme.minus()).widget().action = () -> {
                    sys.wheels.remove(w);
                    save();
                };

                table.row();
            }

            settings = (WContainer) add(theme.settings(sys.settings)).expandX().widget();
        }

        @Override
        public void tick() {
            super.tick();
            sys.settings.tick(settings, theme);
        }

        @Override
        public boolean toClipboard() {
            return NbtUtils.toClipboard(sys);
        }

        @Override
        public boolean fromClipboard() {
            return NbtUtils.fromClipboard(sys);
        }

        private void save() {
            sys.save();
            reload();
        }
    }

    public WheelTab() {
        super("Wheel");
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
