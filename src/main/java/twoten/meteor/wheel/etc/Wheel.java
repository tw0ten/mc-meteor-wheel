package twoten.meteor.wheel.etc;

import java.util.List;

import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.ModuleListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

// TODO: starscript wheel
public class Wheel implements ISerializable<Wheel> {
    public final Settings settings = new Settings();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Keybind> keybind = sgGeneral.add(new KeybindSetting.Builder()
            .name("bind")
            .description("Key to hold.")
            .build());

    public final Setting<List<Module>> modules = sgGeneral.add(new ModuleListSetting.Builder()
            .name("modules")
            .description("Select modules to put in quick access.")
            .build());

    public Wheel(final List<Module> modules) {
        this.modules.set(modules);
    }

    public Wheel() {
        this(List.of());
    }

    public Wheel bind(final Keybind i) {
        this.keybind.set(i);
        return this;
    }

    @Override
    public NbtCompound toTag() {
        final var tag = new NbtCompound();
        tag.put("settings", settings.toTag());
        return tag;
    }

    @Override
    public Wheel fromTag(final NbtCompound tag) {
        settings.fromTag(tag.getCompoundOrEmpty("settings"));
        return this;
    }

    public static Wheel load(final NbtElement tag) {
        return new Wheel().fromTag((NbtCompound) tag);
    }
}
