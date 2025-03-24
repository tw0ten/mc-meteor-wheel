package twoten.meteor.wheel;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import net.fabricmc.loader.api.FabricLoader;
import twoten.meteor.wheel.tabs.WheelTab;

public class Addon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        LOG.info("Initializing {}", name);

        // Tabs
        Tabs.add(new WheelTab());
    }

    @Override
    public String getPackage() {
        return "twoten.meteor.wheel";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("tw0ten", "mc-meteor-wheel", "main");
    }

    @Override
    public String getCommit() {
        return FabricLoader.getInstance()
                .getModContainer("meteor-wheel").orElseThrow()
                .getMetadata()
                .getCustomValue(MeteorClient.MOD_ID + ":commit")
                .getAsString();
    }
}
