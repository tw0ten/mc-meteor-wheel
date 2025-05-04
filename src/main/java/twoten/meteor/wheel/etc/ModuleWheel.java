package twoten.meteor.wheel.etc;

import java.util.List;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.settings.ModuleListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Module;

public class ModuleWheel extends Wheel<Module> {
    public final Setting<List<Module>> modules = sgGeneral.add(new ModuleListSetting.Builder()
            .name("modules")
            .description("Select modules to put in quick access.")
            .build());

    public ModuleWheel(final List<Module> modules) {
        this.modules.set(modules);
    }

    public ModuleWheel() {
        this(List.of());
    }

    @Override
    public Module[] items() {
        return modules.get().toArray(Module[]::new);
    }

    @Override
    public void act(final Module item) {
        item.toggle();

        if (Config.get().chatFeedback.get() && system().chatFeedback.get())
            item.sendToggledMsg();
    }

    @Override
    public void configure(final Module item) {
        MeteorClient.mc.setScreen(GuiThemes.get().moduleScreen(item));
    }
}
