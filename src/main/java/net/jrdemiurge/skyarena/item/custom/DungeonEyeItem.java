package net.jrdemiurge.skyarena.item.custom;

import net.jrdemiurge.skyarena.mixin.EyeOfEnderAccessor;
import net.jrdemiurge.skyarena.util.ModTags;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.structure.Structure;

public class DungeonEyeItem extends Item {
    private final TagKey<Structure> Structure;

    public DungeonEyeItem(Properties properties, TagKey<Structure> structure) {
        super(properties);
        Structure = structure;
    }

    // можно переделать как в катаклизме чтобы всегда выживал но не тратился и с кд в 60 тиков
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverlevel) {
            BlockPos blockpos = serverlevel.findNearestMapStructure(Structure, player.blockPosition(), 100, false);
            if (blockpos != null) {
                EyeOfEnder eyeofender = new EyeOfEnder(level, player.getX(), player.getY(0.5), player.getZ());
                eyeofender.setItem(itemstack);
                eyeofender.signalTo(blockpos);
                ((EyeOfEnderAccessor) eyeofender).setSurviveAfterDeath(true);
                level.gameEvent(GameEvent.PROJECTILE_SHOOT, eyeofender.position(), GameEvent.Context.of(player));
                level.addFreshEntity(eyeofender);
                if (player instanceof ServerPlayer) {
                    CriteriaTriggers.USED_ENDER_EYE.trigger((ServerPlayer)player, blockpos);
                }

                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
                level.levelEvent(null, 1003, player.blockPosition(), 0);
                itemstack.consume(1, player);
                player.awardStat(Stats.ITEM_USED.get(this));
                player.swing(hand, true);
                return InteractionResultHolder.success(itemstack);
            }
        }

        return InteractionResultHolder.consume(itemstack);
    }
}
