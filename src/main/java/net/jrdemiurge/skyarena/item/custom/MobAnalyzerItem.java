package net.jrdemiurge.skyarena.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// TODO вынести сюда логику вывода статов алтаря
public class MobAnalyzerItem extends Item {
    public MobAnalyzerItem(Properties properties) {
        super(properties);
    }

    // TODO я мог бы на шифт выводить все имеющиеся у моба атрибуты, если это возможно
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand usedHand) {
        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            String entityId = interactionTarget.getEncodeId();
            serverPlayer.sendSystemMessage(Component.literal("§4Mob: §a" + entityId));

            Map<Holder<Attribute>, Double> attributes = new LinkedHashMap<>();

            addAttributeIfPresent(attributes, interactionTarget, Attributes.MAX_HEALTH);
            addAttributeIfPresent(attributes, interactionTarget, Attributes.ATTACK_DAMAGE);
            addAttributeIfPresent(attributes, interactionTarget, Attributes.ATTACK_KNOCKBACK);
            addAttributeIfPresent(attributes, interactionTarget, Attributes.ATTACK_SPEED);
            addAttributeIfPresent(attributes, interactionTarget, Attributes.ARMOR);
            addAttributeIfPresent(attributes, interactionTarget, Attributes.ARMOR_TOUGHNESS);
            addAttributeIfPresent(attributes, interactionTarget, Attributes.FOLLOW_RANGE);
            addAttributeIfPresent(attributes, interactionTarget, Attributes.KNOCKBACK_RESISTANCE);
            addAttributeIfPresent(attributes, interactionTarget, Attributes.MOVEMENT_SPEED);
            addAttributeIfPresent(attributes, interactionTarget, Attributes.FLYING_SPEED);

            attributes.forEach((attr, value) -> {
                String formattedValue = String.format("%.2f", value);
                serverPlayer.sendSystemMessage(
                        Component.translatable(attr.value().getDescriptionId())
                                .withStyle(ChatFormatting.GOLD)
                                .append(Component.literal(": §a" + formattedValue))
                );
            });

            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private void addAttributeIfPresent(Map<Holder<Attribute>, Double> attributes, LivingEntity entity, Holder<Attribute> attribute) {
        if (entity.getAttribute(attribute) != null) {
            attributes.put(attribute, entity.getAttributeValue(attribute));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.skyarena.mob_analyzer"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}