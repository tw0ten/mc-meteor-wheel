package twoten.meteor.wheel.etc;

import java.util.List;

import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.macros.Macro;
import meteordevelopment.meteorclient.systems.macros.Macros;
import meteordevelopment.meteorclient.utils.player.ChatUtils;

public class MacroWheel extends Wheel<Macro> {
    // TODO: actual select screen instead of typing by hand
    private final Setting<List<String>> macros = sgItems.add(new StringListSetting.Builder()
            .name("macros")
            .description("Select macros.")
            .build());

    public MacroWheel(final String... macros) {
        this.macros.set(List.of(macros));
    }

    public MacroWheel(final List<Macro> macros) {
        this(macros.stream().map(i -> i.name.get()).toArray(String[]::new));
    }

    @Override
    public Macro[] items() {
        final var system = Macros.get();
        return macros.get().stream().map(system::get).filter(i -> i != null).toArray(Macro[]::new);
    }

    @Override
    public void act(final Macro item) {
        if (system().chatFeedback.get())
            ChatUtils.info("Running macro (highlight)%s(default).", item.name.get());

        item.onAction();
    }

    @Override
    public void configure(final Macro item) {
        ChatUtils.info("TODO: EditMacroScreen is private");
    }

    @Override
    public void render(final Macro item, final boolean selected, final HudRenderer renderer, final double x,
            final double y) {
        final var system = system();
        final var shadow = system.textShadow.get();
        final var title = item.name.get();
        final var width = renderer.textWidth(title, shadow);
        renderer.text(title, x - width / 2.0, y,
                selected ? system.accentColor.get()
                        : system.textColor.get(),
                shadow);
    }

    @Override
    protected Wheel.Type type() {
        return Wheel.Type.Macro;
    }
}
