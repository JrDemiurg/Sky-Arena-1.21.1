package net.jrdemiurge.skyarena.item;

import net.jrdemiurge.skyarena.SkyArena;
import net.jrdemiurge.skyarena.item.custom.DungeonEyeItem;
import net.jrdemiurge.skyarena.item.custom.MobAnalyzerItem;
import net.jrdemiurge.skyarena.util.ModTags;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SkyArena.MOD_ID);

    public static final DeferredItem<Item> FOREST_KEY = ITEMS.register("forest_key",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> CRIMSON_KEY = ITEMS.register("crimson_key",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> DESERT_KEY = ITEMS.register("desert_key",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ICE_KEY = ITEMS.register("ice_key",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ENDER_KEY = ITEMS.register("ender_key",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> CRIMSON_EYE = ITEMS.register("crimson_eye",
            () -> new DungeonEyeItem(new Item.Properties(), ModTags.EYE_OF_SKY_LOCATED));

    public static final DeferredItem<Item> ICE_EYE = ITEMS.register("ice_eye",
            () -> new DungeonEyeItem(new Item.Properties(), ModTags.EYE_OF_ICE_LOCATED));

    public static final DeferredItem<Item> MOB_ANALYZER = ITEMS.register("mob_analyzer",
            () -> new MobAnalyzerItem(new Item.Properties().stacksTo(1)));

    public static void register (IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
