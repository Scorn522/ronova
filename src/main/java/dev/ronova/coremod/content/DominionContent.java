package dev.ronova.coremod.content;

import dev.ronova.coremod.RonovaDominion;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class DominionContent {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, RonovaDominion.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RonovaDominion.MOD_ID);

    public static final RegistryObject<Item> RONOVA_BLADE =
        ITEMS.register("ronova_blade", RonovaBladeItem::new);

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = TABS.register("main", () ->
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.ronova"))
            .icon(() -> RONOVA_BLADE.get().getDefaultInstance())
            .displayItems((parameters, output) -> output.accept(RONOVA_BLADE.get()))
            .build());

    private DominionContent() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        TABS.register(bus);
    }
}
