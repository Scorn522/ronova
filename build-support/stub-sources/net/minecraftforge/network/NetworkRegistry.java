package net.minecraftforge.network;

import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.simple.SimpleChannel;

public final class NetworkRegistry {
    private NetworkRegistry() {}

    public static final class ChannelBuilder {
        public static ChannelBuilder named(ResourceLocation name) {
            return new ChannelBuilder();
        }

        public ChannelBuilder networkProtocolVersion(Supplier<String> version) {
            return this;
        }

        public ChannelBuilder clientAcceptedVersions(Predicate<String> accepted) {
            return this;
        }

        public ChannelBuilder serverAcceptedVersions(Predicate<String> accepted) {
            return this;
        }

        public SimpleChannel simpleChannel() {
            return new SimpleChannel();
        }
    }
}
