package net.jrdemiurge.skyarena.item;

import net.jrdemiurge.skyarena.SkyArena;
import net.jrdemiurge.skyarena.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SkyArena.MOD_ID);

    public static final Supplier<CreativeModeTab> SKYARENA_TAB = CREATIVE_MODE_TAB.register("skyarena_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModBlocks.ALTAR_BATTLE.get()))
                    .title(Component.translatable("creativetab.skyarena_tab"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModBlocks.ALTAR_BATTLE.get());
                        output.accept(ModBlocks.OAK_TROPHY.get());
                        output.accept(ModBlocks.STONE_TROPHY.get());
                        output.accept(ModBlocks.IRON_TROPHY.get());
                        output.accept(ModBlocks.GOLD_TROPHY.get());
                        output.accept(ModBlocks.DIAMOND_TROPHY.get());
                        output.accept(ModBlocks.NETHERITE_TROPHY.get());
                        output.accept(ModItems.CRIMSON_KEY.get());
                        output.accept(ModItems.DESERT_KEY.get());
                        output.accept(ModItems.ENDER_KEY.get());
                        output.accept(ModItems.ICE_KEY.get());
                        output.accept(ModItems.FOREST_KEY.get());
                        output.accept(ModItems.CRIMSON_EYE.get());
                        output.accept(ModItems.ICE_EYE.get());
                        output.accept(ModItems.MOB_ANALYZER.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
