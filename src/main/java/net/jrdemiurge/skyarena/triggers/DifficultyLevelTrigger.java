package net.jrdemiurge.skyarena.triggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class DifficultyLevelTrigger extends SimpleCriterionTrigger<DifficultyLevelTrigger.Instance> {
    public void trigger(ServerPlayer player, int actualLevel) {
        this.trigger(player, inst -> actualLevel >= inst.minLevel());
    }

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public record Instance(Optional<ContextAwarePredicate> player, int minLevel)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                // стандартный опциональный фильтр по игроку (можно не указывать в JSON)
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                // порог из JSON; по умолчанию 0 (тогда триггер проходит всегда)
                Codec.INT.fieldOf("level").orElse(0).forGetter(Instance::minLevel)
        ).apply(inst, Instance::new));
    }
}