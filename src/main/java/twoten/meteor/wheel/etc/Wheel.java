package twoten.meteor.wheel.etc;

import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import twoten.meteor.wheel.systems.WheelSystem;

// TODO: starscript wheel
public abstract class Wheel<T> implements ISerializable<Wheel<T>> {
    public static Wheel<?> load(final NbtElement tag) {
        return new ModuleWheel().fromTag((NbtCompound) tag);
    }

    protected static WheelSystem system() {
        return Systems.get(WheelSystem.class);
    }

    public final Settings settings = new Settings();

    protected final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Keybind> keybind = sgGeneral.add(new KeybindSetting.Builder()
            .name("bind")
            .description("Key to hold.")
            .build());

    public Wheel<T> bind(final Keybind i) {
        this.keybind.set(i);
        return this;
    }

    public abstract T[] items();

    public abstract void act(T item);

    public abstract void configure(T item);

    @Override
    public NbtCompound toTag() {
        final var tag = new NbtCompound();
        tag.put("settings", settings.toTag());
        return tag;
    }

    @Override
    public Wheel<T> fromTag(final NbtCompound tag) {
        settings.fromTag(tag.getCompoundOrEmpty("settings"));
        return this;
    }

    public String name() {
        return "[" + keybind + "]" + " " + items().length;
    }
}
