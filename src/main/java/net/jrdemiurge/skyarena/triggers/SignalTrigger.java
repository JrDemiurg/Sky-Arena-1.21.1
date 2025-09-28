package net.jrdemiurge.skyarena.triggers;

import com.mojang.serialization.Codec;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class SignalTrigger extends SimpleCriterionTrigger<SignalTrigger.Instance> {

    public void trigger(ServerPlayer player) {
        this.trigger(player, inst -> true);
    }

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public static final class Instance implements SimpleInstance {
        static final Instance INSTANCE = new Instance();
        static final Codec<Instance> CODEC = Codec.unit(INSTANCE);

        @Override
        public Optional<ContextAwarePredicate> player() {
            return Optional.empty();
        }
    }
}


