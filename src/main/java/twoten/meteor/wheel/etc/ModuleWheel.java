package twoten.meteor.wheel.etc;

import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.settings.ModuleListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Module;

public class ModuleWheel extends Wheel<Module> {
    public final Setting<List<Module>> modules = sgItems.add(new ModuleListSetting.Builder()
            .name("modules")
            .description("Select modules to put in quick access.")
            .build());

    public ModuleWheel(final List<Module> modules) {
        this.modules.set(modules);
    }

    @SafeVarargs
    public ModuleWheel(final Class<? extends Module>... modules) {
        this(new ModuleListSetting.Builder().defaultValue(modules).build().get());
    }

    @Override
    public Module[] items() {
        return modules.get().toArray(Module[]::new);
    }

    @Override
    public void act(final Module item) {
        item.toggle();

        if (system().chatFeedback.get())
            item.sendToggledMsg();
    }

    @Override
    public void configure(final Module item) {
        MeteorClient.mc.setScreen(GuiThemes.get().moduleScreen(item));
    }

    @Override
    public void render(final Module item, final boolean selected,
            final HudRenderer renderer,
            final double x, final double y) {
        final var system = system();
        final var shadow = system.textShadow.get();
        final var width = renderer.textWidth(item.title, shadow);
        renderer.text(item.title, x - width / 2.0, y,
                selected ? system.accentColor.get()
                        : item.isActive() ? system.textColor.get()
                                : system.disabledColor.get(),
                shadow);
    }

    @Override
    public Wheel.Type type() {
        return Wheel.Type.Module;
    }
}
