package net.jrdemiurge.skyarena;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> WIKI_LINK = BUILDER
            .comment("Description for customizing the sky_arena.json config can be found at the following link")
            .define("wikiLink", "https://github.com/JrDemiurg/DemisSkyArenaMod/wiki");

    public static final ModConfigSpec.BooleanValue REQUIRE_EMPTY_CHEST = BUILDER
            .comment("""
                    If true, the reward key can only be used on empty chests.
                    If false, the chest's contents will be cleared before being filled with loot.""")
            .define("requireEmptyChest", false);

    public static final ModConfigSpec.BooleanValue ENABLE_LOSS_MESSAGE_LEAVE = BUILDER
            .comment("If true, a defeat message will be shown when the player leaves the arena.")
            .define("enableLossMessageLeave", true);

    public static final ModConfigSpec.BooleanValue ENABLE_LOSS_MESSAGE_DEATH = BUILDER
            .comment("If true, a defeat message will be shown when the player dies in battle.")
            .define("enableLossMessageDeath", true);

    public static final ModConfigSpec.BooleanValue ENABLE_UNCLAIMED_REWARD_MESSAGE = BUILDER
            .comment("""
                    If true, when mobs receive the Glowing effect, a message will appear recommending leaving the arena to restart the battle.
                    The message appears only once per game session.""")
            .define("enableUnclaimedRewardMessage", true);

    static final ModConfigSpec SPEC = BUILDER.build();
}
