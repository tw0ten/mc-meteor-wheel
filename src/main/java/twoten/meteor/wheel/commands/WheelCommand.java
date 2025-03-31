package twoten.meteor.wheel.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.Systems;
import net.minecraft.command.CommandSource;
import twoten.meteor.wheel.systems.WheelSystem;

public class WheelCommand extends Command {
    public WheelCommand() {
        super("wheel", "Intended for use in combination with macros - create wheel menus programmatically.");
    }

    @Override
    public void build(final LiteralArgumentBuilder<CommandSource> builder) {
        final var sys = Systems.get(WheelSystem.class);
        // TODO: ???
        builder.then(literal("favorites").executes(context -> {
            sys.open(sys.favorites());
            return SINGLE_SUCCESS;
        }));
    }
}
