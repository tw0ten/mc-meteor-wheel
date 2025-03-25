package twoten.meteor.wheel;

import java.util.List;

import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.ModuleListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import net.minecraft.nbt.NbtCompound;

public class Wheel {
    public static Wheel favorites(final Keybind key) {
        return new Wheel(Modules.get()
                .getAll().stream()
                .filter(i -> i.favorite)
                .toList())
                .bind(key);
    }

    public final Settings settings = new Settings();

    private final SettingGroup sgGeneral = settings.createGroup("Settings");

    public final Setting<List<Module>> modules = sgGeneral.add(new ModuleListSetting.Builder()
            .name("modules")
            .description("Select modules to put in quick access.")
            .build());

    public final Setting<Keybind> keybind = sgGeneral.add(new KeybindSetting.Builder()
            .name("bind")
            .description("Key to hold.")
            .build());

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
