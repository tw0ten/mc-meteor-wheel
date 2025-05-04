package twoten.meteor.wheel.etc;

import java.util.function.Supplier;

import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import twoten.meteor.wheel.systems.WheelSystem;

public abstract class Wheel<T> implements ISerializable<Wheel<T>> {
    public enum Type {
        Module(ModuleWheel::new),
        Macro(MacroWheel::new);

        public static Type get(final String name) {
            for (final var v : values())
                if (v.name().equals(name))
                    return v;
            return Module;
        }

        private final Supplier<Wheel<?>> supplier;

        Type(final Supplier<Wheel<?>> supplier) {
            this.supplier = supplier;
        }

        public Wheel<?> create() {
            return supplier.get();
        }
    }

    public static Wheel<?> load(final NbtElement tag) {
        final var compound = (NbtCompound) tag;
        return Type.get(compound.getString("type").orElse(null)).create().fromTag(compound);
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

    public abstract void render(T item, boolean selected, HudRenderer renderer, double x, double y);

    @Override
    public NbtCompound toTag() {
        final var tag = new NbtCompound();
        tag.putString("type", type().name());
        tag.put("settings", settings.toTag());
        return tag;
    }

    @Override
    public Wheel<T> fromTag(final NbtCompound tag) {
        settings.fromTag(tag.getCompoundOrEmpty("settings"));
        return this;
    }

    public String name() {
        return type() + " " + "[" + keybind + "]" + " " + items().length;
    }

    protected abstract Type type();
}
