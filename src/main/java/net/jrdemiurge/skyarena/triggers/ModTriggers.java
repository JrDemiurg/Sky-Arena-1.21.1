package net.jrdemiurge.skyarena.triggers;

import net.jrdemiurge.skyarena.SkyArena;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModTriggers {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGER_TYPES =
            DeferredRegister.create(Registries.TRIGGER_TYPE, SkyArena.MOD_ID);

    public static final Supplier<SignalTrigger> USE_ALTAR_BATTLE =
            TRIGGER_TYPES.register("use_altar_battle", SignalTrigger::new);

    public static final Supplier<SignalTrigger> USE_MUSIC_DISK =
            TRIGGER_TYPES.register("use_music_disk", SignalTrigger::new);

    public static final Supplier<SignalTrigger> USE_NETHERITE_INGOT =
            TRIGGER_TYPES.register("use_netherite_ingot", SignalTrigger::new);

    public static final Supplier<SignalTrigger> USE_STICK =
            TRIGGER_TYPES.register("use_stick", SignalTrigger::new);

    public static final Supplier<DifficultyLevelTrigger> DIFFICULTY_LEVEL =
            TRIGGER_TYPES.register("difficulty_level", DifficultyLevelTrigger::new);

    public static void register(IEventBus eventBus) {
        TRIGGER_TYPES.register(eventBus);
    }
}
