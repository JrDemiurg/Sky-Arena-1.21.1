package net.jrdemiurge.skyarena.block.entity;

import net.jrdemiurge.skyarena.Config;
import net.jrdemiurge.skyarena.block.custom.AltarBlock;
import net.jrdemiurge.skyarena.config.*;
import net.jrdemiurge.skyarena.scheduler.Scheduler;
import net.jrdemiurge.skyarena.util.BossBarHideZones;
import net.jrdemiurge.skyarena.util.FullProtectionZones;
import net.jrdemiurge.skyarena.util.MobGriefingProtectionZones;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class AltarBlockEntity extends BlockEntity {

    private static final Map<Player, BlockPos> activeAltarBlocks = new HashMap<>();
    private Player activatingPlayer;
    public final List<Entity> summonedMobs = new ArrayList<>();
    private boolean battlePhaseActive = false;
    private boolean playerDeath = false;
    private int deathDelay = 0;
    private long battleEndTime = 0;
    private int glowingCounter = 0;
    private boolean firstMessageSent = false;
    private static final Map<Player, Long> playerMessageTimestamps = new HashMap<>();
    // музыка
    private ItemStack recordItem = ItemStack.EMPTY;
    private boolean isPlayingMusic = false;
    private long musicEndTick = 0;
    private long musicTickCount = 0;
    private Holder<JukeboxSong> song;
    private long ticksSinceSongStarted;
    // сложность
    private int difficultyLevel = 1;
    private int battleDifficultyLevel = 1;
    // данные блока
    private String arenaType = "sky_arena";
    private int mobSpawnRadius;
    private int spawnDistanceFromPlayer;
    private int battleLossDistance;
    private int mobTeleportDistance;
    private int mobGriefingProtectionRadius = 0;
    private int fullProtectionRadius = 0;
    private int bossBarHideRadius = 0;
    private int mobCostRatio;

    private boolean allowDifficultyReset;
    private boolean allowWaterAndAirSpawn;
    private boolean individualPlayerStats;
    private boolean setNight;
    private boolean setRain;
    private boolean disableMobItemDrop;

    private int startingPoints;
    private double startingStatMultiplier;

    private boolean resetDifficultyOnDefeat;
    private boolean autoWaveRun;

    private double pointsCoefficientPerBlocks;
    private double statMultiplierCoefficientPerBlocks;
    private double lootTableCountCoefficientPerBlocks;

    private List<DifficultyLevelRange> difficultyLevelRanges = new ArrayList<>();
    private LinkedHashMap<String, MobGroup> mobGroups = new LinkedHashMap<>();
    private LinkedHashMap<Integer, PresetWave> presetWaves = new LinkedHashMap<>();

    private final Map<String, Integer> playerDifficulty = new HashMap<>();

    public AltarBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.ALTAR_BLOCK_ENTITY.get(), pPos, pBlockState);
    }

    public void loadArenaConfig(String arenaType) {
        if (level != null && level.isClientSide) {
            return;
        }

        ArenaConfig arenaConfig;

        if (SkyArenaConfig.configData == null) {
            arenaConfig = SkyArenaConfig.DEFAULT_ARENA;
        } else {
            arenaConfig = SkyArenaConfig.configData.arenas.getOrDefault(arenaType, SkyArenaConfig.DEFAULT_ARENA);
        }

        if (arenaConfig != null) {
            this.arenaType = arenaType;
            this.mobSpawnRadius = arenaConfig.mobSpawnRadius != 0 ? arenaConfig.mobSpawnRadius : 36;
            this.spawnDistanceFromPlayer = arenaConfig.spawnDistanceFromPlayer;
            this.battleLossDistance = arenaConfig.battleLossDistance != 0 ? arenaConfig.battleLossDistance : 60;
            this.mobTeleportDistance = arenaConfig.mobTeleportDistance != 0 ? arenaConfig.mobTeleportDistance : 50;
            this.mobGriefingProtectionRadius = arenaConfig.mobGriefingProtectionRadius;
            this.fullProtectionRadius = arenaConfig.fullProtectionRadius;
            this.bossBarHideRadius = arenaConfig.bossBarHideRadius;
            this.mobCostRatio = arenaConfig.mobCostRatio != 0 ? arenaConfig.mobCostRatio : 20;

            this.allowDifficultyReset = arenaConfig.allowDifficultyReset;
            this.allowWaterAndAirSpawn = arenaConfig.allowWaterAndAirSpawn;
            this.individualPlayerStats = arenaConfig.individualPlayerStats;
            this.setNight = arenaConfig.setNight;
            this.setRain = arenaConfig.setRain;
            this.disableMobItemDrop = arenaConfig.disableMobItemDrop;

            this.startingPoints = arenaConfig.startingPoints != 0 ? arenaConfig.startingPoints : 500;
            this.startingStatMultiplier = arenaConfig.startingStatMultiplier != 0.0 ? arenaConfig.startingStatMultiplier : 1.0;

            this.resetDifficultyOnDefeat = arenaConfig.resetDifficultyOnDefeat;
            this.autoWaveRun = arenaConfig.autoWaveRun;

            this.pointsCoefficientPerBlocks = arenaConfig.pointsCoefficientPer1000BlocksFromWorldCenter;
            this.statMultiplierCoefficientPerBlocks = arenaConfig.statMultiplierCoefficientPer1000BlocksFromWorldCenter;
            this.lootTableCountCoefficientPerBlocks = arenaConfig.lootTableCountCoefficientPer1000BlocksFromWorldCenter;

            if (fullProtectionRadius != 0 && fullProtectionRadius > mobGriefingProtectionRadius) {
                mobGriefingProtectionRadius = fullProtectionRadius;
            }

            if (arenaConfig.difficultyLevelRanges != null) {
                this.difficultyLevelRanges = new ArrayList<>(arenaConfig.difficultyLevelRanges);
            } else {
                this.difficultyLevelRanges = new ArrayList<>();
            }

            if (arenaConfig.mobGroups != null) {
                this.mobGroups = new LinkedHashMap<>(arenaConfig.mobGroups);
            } else {
                this.mobGroups = new LinkedHashMap<>();
            }

            if (arenaConfig.presetWaves != null) {
                this.presetWaves = new LinkedHashMap<>(arenaConfig.presetWaves);
            } else {
                this.presetWaves = new LinkedHashMap<>();
            }

            if(level != null) {
                MobGriefingProtectionZones.remove(level, this.getBlockPos());
                if (mobGriefingProtectionRadius != 0) {
                    MobGriefingProtectionZones.add(level, this.getBlockPos(), mobGriefingProtectionRadius);
                }

                FullProtectionZones.remove(level, this.getBlockPos());
                if (fullProtectionRadius != 0) {
                    FullProtectionZones.add(level, this.getBlockPos(), fullProtectionRadius);
                }

                BossBarHideZones.remove(level, this.getBlockPos());
                if (bossBarHideRadius != 0){
                    BossBarHideZones.add(level, this.getBlockPos(), bossBarHideRadius);
                }
            }

            if (this.level != null) {
                this.level.blockEntityChanged(this.getBlockPos());
            }
        }
    }

    public void switchToNextArena() {
        if (SkyArenaConfig.configData == null) {
            return;
        }

        List<String> arenaTypes = new ArrayList<>(SkyArenaConfig.configData.arenas.keySet());

        if (arenaTypes.isEmpty()) {
            return;
        }

        int currentIndex = arenaTypes.indexOf(this.arenaType);
        int nextIndex = (currentIndex + 1) % arenaTypes.size();

        this.arenaType = arenaTypes.get(nextIndex);
        loadArenaConfig(this.arenaType);

        if (this.level != null) {
            this.level.blockEntityChanged(this.getBlockPos());
        }
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (!this.level.isClientSide()) {
            if (mobGriefingProtectionRadius != 0) {
                MobGriefingProtectionZones.add(level, this.getBlockPos(), mobGriefingProtectionRadius);
            }

            if (fullProtectionRadius != 0) {
                FullProtectionZones.add(level, this.getBlockPos(), fullProtectionRadius);
            }

            if (bossBarHideRadius != 0) {
                BossBarHideZones.add(level, this.getBlockPos(), bossBarHideRadius);
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        removeSummonedMobs();
        if (!this.level.isClientSide()) {
            MobGriefingProtectionZones.remove(level, this.getBlockPos());

            FullProtectionZones.remove(level, this.getBlockPos());

            BossBarHideZones.remove(level, this.getBlockPos());
        }
        stopMusic();
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider registries) {
        super.saveAdditional(pTag, registries);

        pTag.putInt("DifficultyLevel", this.difficultyLevel);
        pTag.putString("ArenaType", this.arenaType);
        pTag.putInt("StartingPoints", this.startingPoints);
        pTag.putDouble("StartingStatMultiplier", this.startingStatMultiplier);
        pTag.putInt("MobSpawnRadius", this.mobSpawnRadius);
        pTag.putInt("MobCostRatio", this.mobCostRatio);
        pTag.putInt("SpawnDistanceFromPlayer", this.spawnDistanceFromPlayer);
        pTag.putInt("BattleLossDistance", this.battleLossDistance);
        pTag.putInt("MobTeleportDistance", this.mobTeleportDistance);
        pTag.putBoolean("AllowDifficultyReset", this.allowDifficultyReset);
        pTag.putBoolean("AllowWaterAndAirSpawn", this.allowWaterAndAirSpawn);
        pTag.putBoolean("IndividualPlayerStats", this.individualPlayerStats);
        pTag.putBoolean("SetNight", this.setNight);
        pTag.putBoolean("SetRain", this.setRain);
        pTag.putBoolean("EnableMobItemDrop", this.disableMobItemDrop);
        pTag.putInt("MobGriefingProtectionRadius", this.mobGriefingProtectionRadius);
        pTag.putInt("FullProtectionRadius", this.fullProtectionRadius);
        pTag.putInt("BossBarHideRadius", this.bossBarHideRadius);
        pTag.putBoolean("ResetDifficultyOnDefeat", this.resetDifficultyOnDefeat);
        pTag.putBoolean("AutoWaveRun", this.autoWaveRun);
        pTag.putDouble("PointsCoefficientPerBlocks", this.pointsCoefficientPerBlocks);
        pTag.putDouble("StatMultiplierCoefficientPerBlocks", this.statMultiplierCoefficientPerBlocks);
        pTag.putDouble("LootTableCountCoefficientPerBlocks", this.lootTableCountCoefficientPerBlocks);

        if (!this.recordItem.isEmpty()) {
            pTag.put("RecordItem", this.recordItem.save(registries));
        }

        if (!playerDifficulty.isEmpty()) {
            CompoundTag difficultyTag = new CompoundTag();
            for (Map.Entry<String, Integer> entry : playerDifficulty.entrySet()) {
                difficultyTag.putInt(entry.getKey(), entry.getValue());
            }
            pTag.put("PlayerDifficulty", difficultyTag);
        }

        if (!difficultyLevelRanges.isEmpty()) {
            ListTag rangesTag = new ListTag();
            for (DifficultyLevelRange range : difficultyLevelRanges) {
                CompoundTag rangeTag = new CompoundTag();

                ListTag intList = new ListTag();
                for (int val : range.range) {
                    intList.add(IntTag.valueOf(val));
                }
                rangeTag.put("Range", intList);
                rangeTag.putInt("PointsIncrease", range.pointsIncrease);
                rangeTag.putDouble("StatMultiplierIncrease", range.statMultiplierIncrease);
                rangeTag.putString("RewardLootTable", range.rewardLootTable);
                rangeTag.putInt("RewardCount", range.rewardCount);

                ListTag usedGroupsTag = new ListTag();
                for (String group : range.mobGroupsUsed) {
                    usedGroupsTag.add(StringTag.valueOf(group));
                }
                rangeTag.put("MobGroupsUsed", usedGroupsTag);

                rangesTag.add(rangeTag);
            }
            pTag.put("DifficultyLevelRanges", rangesTag);
        }

        if (!mobGroups.isEmpty()) {
            CompoundTag groupsTag = new CompoundTag();
            for (Map.Entry<String, MobGroup> entry : mobGroups.entrySet()) {
                CompoundTag groupTag = new CompoundTag();
                MobGroup group = entry.getValue();
                groupTag.putDouble("SquadSpawnChance", group.squadSpawnChance);
                groupTag.putInt("SquadSpawnSize", group.squadSpawnSize);
                groupTag.putDouble("StatMultiplierCoefficient", group.statMultiplierCoefficient);
                groupTag.putDouble("MobSpawnChance", group.mobSpawnChance);

                CompoundTag mobValuesTag = new CompoundTag();
                for (Map.Entry<String, Integer> mobEntry : group.mobValues.entrySet()) {
                    mobValuesTag.putInt(mobEntry.getKey(), mobEntry.getValue());
                }

                groupTag.put("MobValues", mobValuesTag);
                groupsTag.put(entry.getKey(), groupTag);
            }
            pTag.put("MobGroups", groupsTag);
        }

        if (!presetWaves.isEmpty()) {
            CompoundTag presetWavesTag = new CompoundTag();

            for (Map.Entry<Integer, PresetWave> entry : presetWaves.entrySet()) {
                CompoundTag waveTag = new CompoundTag();
                waveTag.putDouble("MobStatMultiplier", entry.getValue().mobStatMultiplier);
                waveTag.putString("RewardLootTable", entry.getValue().rewardLootTable);
                waveTag.putInt("RewardCount", entry.getValue().rewardCount);

                ListTag mobsTag = new ListTag();
                for (WaveMob mob : entry.getValue().mobs) {
                    CompoundTag mobTag = new CompoundTag();
                    mobTag.putString("Type", mob.type);
                    mobTag.putInt("Count", mob.count);
                    mobsTag.add(mobTag);
                }

                waveTag.put("Mobs", mobsTag);
                presetWavesTag.put(entry.getKey().toString(), waveTag);
            }

            pTag.put("PresetWaves", presetWavesTag);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider registries) {
        super.loadAdditional(pTag, registries);

        if (pTag.contains("DifficultyLevel")) this.difficultyLevel = pTag.getInt("DifficultyLevel");
        if (pTag.contains("ArenaType")) this.arenaType = pTag.getString("ArenaType");
        if (pTag.contains("StartingPoints")) this.startingPoints = pTag.getInt("StartingPoints");
        if (pTag.contains("StartingStatMultiplier")) this.startingStatMultiplier = pTag.getDouble("StartingStatMultiplier");
        if (pTag.contains("MobSpawnRadius")) this.mobSpawnRadius = pTag.getInt("MobSpawnRadius");
        if (pTag.contains("MobCostRatio")) this.mobCostRatio = pTag.getInt("MobCostRatio");
        if (pTag.contains("SpawnDistanceFromPlayer")) this.spawnDistanceFromPlayer = pTag.getInt("SpawnDistanceFromPlayer");
        if (pTag.contains("BattleLossDistance")) this.battleLossDistance = pTag.getInt("BattleLossDistance");
        if (pTag.contains("MobTeleportDistance")) this.mobTeleportDistance = pTag.getInt("MobTeleportDistance");
        if (pTag.contains("AllowDifficultyReset")) this.allowDifficultyReset = pTag.getBoolean("AllowDifficultyReset");
        if (pTag.contains("AllowWaterAndAirSpawn")) this.allowWaterAndAirSpawn = pTag.getBoolean("AllowWaterAndAirSpawn");
        if (pTag.contains("IndividualPlayerStats")) this.individualPlayerStats = pTag.getBoolean("IndividualPlayerStats");
        if (pTag.contains("SetNight")) this.setNight = pTag.getBoolean("SetNight");
        if (pTag.contains("SetRain")) this.setRain = pTag.getBoolean("SetRain");
        if (pTag.contains("EnableMobItemDrop")) this.disableMobItemDrop = pTag.getBoolean("EnableMobItemDrop");
        if (pTag.contains("MobGriefingProtectionRadius")) this.mobGriefingProtectionRadius = pTag.getInt("MobGriefingProtectionRadius");
        if (pTag.contains("FullProtectionRadius")) this.fullProtectionRadius = pTag.getInt("FullProtectionRadius");
        if (pTag.contains("BossBarHideRadius")) this.bossBarHideRadius = pTag.getInt("BossBarHideRadius");
        if (pTag.contains("ResetDifficultyOnDefeat")) this.resetDifficultyOnDefeat = pTag.getBoolean("ResetDifficultyOnDefeat");
        if (pTag.contains("AutoWaveRun")) this.autoWaveRun = pTag.getBoolean("AutoWaveRun");
        if (pTag.contains("PointsCoefficientPerBlocks")) this.pointsCoefficientPerBlocks = pTag.getDouble("PointsCoefficientPerBlocks");
        if (pTag.contains("StatMultiplierCoefficientPerBlocks")) this.statMultiplierCoefficientPerBlocks = pTag.getDouble("StatMultiplierCoefficientPerBlocks");
        if (pTag.contains("LootTableCountCoefficientPerBlocks")) this.lootTableCountCoefficientPerBlocks = pTag.getDouble("LootTableCountCoefficientPerBlocks");

        if (pTag.contains("RecordItem")) {
            this.recordItem = ItemStack.parse(registries, pTag.getCompound("RecordItem")).orElse(ItemStack.EMPTY);
        }

        if (pTag.contains("PlayerDifficulty")) {
            CompoundTag difficultyTag = pTag.getCompound("PlayerDifficulty");
            for (String key : difficultyTag.getAllKeys()) {
                playerDifficulty.put(key, difficultyTag.getInt(key));
            }
        }

        if (pTag.contains("DifficultyLevelRanges")) {
            ListTag rangesTag = pTag.getList("DifficultyLevelRanges", Tag.TAG_COMPOUND);
            difficultyLevelRanges.clear();

            for (Tag tag : rangesTag) {
                CompoundTag rangeTag = (CompoundTag) tag;
                DifficultyLevelRange range = new DifficultyLevelRange();

                ListTag intList = rangeTag.getList("Range", Tag.TAG_INT);
                range.range = new ArrayList<>();
                for (Tag intTag : intList) {
                    range.range.add(((IntTag) intTag).getAsInt());
                }

                range.pointsIncrease = rangeTag.getInt("PointsIncrease");
                range.statMultiplierIncrease = rangeTag.getDouble("StatMultiplierIncrease");
                range.rewardLootTable = rangeTag.getString("RewardLootTable");
                range.rewardCount = rangeTag.getInt("RewardCount");

                ListTag usedGroupsTag = rangeTag.getList("MobGroupsUsed", Tag.TAG_STRING);
                range.mobGroupsUsed = new ArrayList<>();
                for (Tag groupTag : usedGroupsTag) {
                    range.mobGroupsUsed.add(((StringTag) groupTag).getAsString());
                }

                difficultyLevelRanges.add(range);
            }
        }

        if (pTag.contains("MobGroups")) {
            CompoundTag groupsTag = pTag.getCompound("MobGroups");
            mobGroups.clear();

            for (String key : groupsTag.getAllKeys()) {
                CompoundTag groupTag = groupsTag.getCompound(key);
                MobGroup group = new MobGroup();

                group.squadSpawnChance = groupTag.getDouble("SquadSpawnChance");
                group.squadSpawnSize = groupTag.getInt("SquadSpawnSize");
                group.statMultiplierCoefficient = groupTag.getDouble("StatMultiplierCoefficient");
                group.mobSpawnChance = groupTag.getDouble("MobSpawnChance");

                CompoundTag mobValuesTag = groupTag.getCompound("MobValues");
                group.mobValues = new LinkedHashMap<>();
                for (String mobId : mobValuesTag.getAllKeys()) {
                    group.mobValues.put(mobId, mobValuesTag.getInt(mobId));
                }

                mobGroups.put(key, group);
            }
        }

        if (pTag.contains("PresetWaves")) {
            CompoundTag presetWavesTag = pTag.getCompound("PresetWaves");
            presetWaves.clear();

            for (String key : presetWavesTag.getAllKeys()) {
                int waveNumber = Integer.parseInt(key);
                CompoundTag waveTag = presetWavesTag.getCompound(key);

                PresetWave wave = new PresetWave();
                wave.mobStatMultiplier = waveTag.getDouble("MobStatMultiplier");
                wave.rewardLootTable = waveTag.getString("RewardLootTable");
                wave.rewardCount = waveTag.getInt("RewardCount");

                ListTag mobsTag = waveTag.getList("Mobs", Tag.TAG_COMPOUND);
                wave.mobs = new ArrayList<>();

                for (Tag tag : mobsTag) {
                    CompoundTag mobTag = (CompoundTag) tag;
                    WaveMob mob = new WaveMob();
                    mob.type = mobTag.getString("Type");
                    mob.count = mobTag.getInt("Count");
                    wave.mobs.add(mob);
                }

                presetWaves.put(waveNumber, wave);
            }
        }

        if (SkyArenaConfig.configData != null) {
            ArenaConfig arenaConfig = SkyArenaConfig.configData.arenas.getOrDefault(this.arenaType, null);
            if (arenaConfig != null) {
                loadArenaConfig(this.arenaType);
            }
        }
    }

    public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
        if (battlePhaseActive) {
            handleMusic();
            checkPlayerLeftArena();
            handlePlayerDeath();
            updateSummonedMobs();
            if (autoWaveRun) runAutoWave();
        } else {
            updateDifficultyMessages();
        }
    }

    private void updateDifficultyMessages() {
        long gameTime = level.getGameTime();
        if (gameTime % 10 != 0) return;

        AABB area = new AABB(worldPosition).inflate(2);
        List<Player> players = level.getEntitiesOfClass(Player.class, area);

        for (Player player : players) {
            if (!isBattleOngoing(getDifficultyLevel(player))) return;
            
            long lastShownTick = playerMessageTimestamps.getOrDefault(player, -40L);

            if (gameTime - lastShownTick >= 60) {
                int diff = getDifficultyLevel(player);
                player.displayClientMessage(
                        Component.translatable("message.skyarena.difficult_level")
                                .append(Component.literal(String.valueOf(diff))), true);
                playerMessageTimestamps.put(player, level.getGameTime() - 20);
            }
        }
    }

    // TODO проверить что случшится если установить модовую пластинку а потом удалить мод и включить алтарь
    private void handleMusic() {
        /*if (!isPlayingMusic || level == null || recordItem.isEmpty()) return;

        if (musicTickCount >= musicEndTick) {
            stopMusic();
            startMusic();
        }
        musicTickCount++;*/

        if (level != null && this.song != null) {
            if (this.song.value().hasFinished(this.ticksSinceSongStarted)) {
                stopMusic();
                startMusic();
            } else {
                this.ticksSinceSongStarted++;
            }
        }
    }

    public void startMusic() {
        if (this.level != null && !recordItem.isEmpty()) {
            Optional<Holder<JukeboxSong>> optional = JukeboxSong.fromStack(this.level.registryAccess(), recordItem);
            if (optional.isPresent()) {
                this.song = optional.get();
                this.ticksSinceSongStarted = 0L;
                int i = level.registryAccess().registryOrThrow(Registries.JUKEBOX_SONG).getId(this.song.value());
                level.levelEvent(null, 1010, this.getBlockPos(), i);
                this.setChanged(); // ???
            }
        }
        //
        /*if (this.level != null && !recordItem.isEmpty() && !isPlayingMusic) {
            this.musicTickCount = this.level.getGameTime();
            RecordItem record = (RecordItem) recordItem.getItem();
            this.musicEndTick = this.musicTickCount + record.getLengthInTicks() + 20L;
            this.isPlayingMusic = true;
            this.level.levelEvent(null, 1010, this.getBlockPos(), Item.getId(recordItem.getItem()));
            this.setChanged();
        }*/
    }

    public void stopMusic() {
        if (this.level != null && this.song != null) {
            this.song = null;
            this.ticksSinceSongStarted = 0L;
            level.gameEvent(GameEvent.JUKEBOX_STOP_PLAY, this.getBlockPos(), GameEvent.Context.of(this.getBlockState()));
            level.levelEvent(1011, this.getBlockPos(), 0);
            this.setChanged(); // ???
        }
        //
        /*if (this.level != null && isPlayingMusic) {
            this.isPlayingMusic = false;
            this.level.levelEvent(1011, this.getBlockPos(), 0);
            this.setChanged();
        }*/
    }

    private void checkPlayerLeftArena() {
        if (activatingPlayer == null) return;
        double distance = activatingPlayer.distanceToSqr(worldPosition.getCenter());

        if (distance > battleLossDistance * battleLossDistance) {
            onDefeat();

            if (activatingPlayer instanceof ServerPlayer serverPlayer && Config.ENABLE_LOSS_MESSAGE_LEAVE.isTrue()) {
                sendTitle(serverPlayer,
                        "message.skyarena.battle_failed", ChatFormatting.DARK_RED,
                        "message.skyarena.left_arena", ChatFormatting.WHITE);
            }
        }
    }

    private void handlePlayerDeath() {
        if (!playerDeath) return;
        deathDelay++;

        if (activatingPlayer != null && deathDelay > 10) {
            if (activatingPlayer.getHealth() > 0) {
                playerDeath = false;
                deathDelay = 0;
                return;
            }

            onDefeat();
            if (activatingPlayer instanceof ServerPlayer serverPlayer && Config.ENABLE_LOSS_MESSAGE_DEATH.isTrue()) {
                sendTitle(serverPlayer,
                        "message.skyarena.wasted", ChatFormatting.WHITE,
                        "message.skyarena.battle_failed", ChatFormatting.DARK_RED);
            }
        }
    }

    private void sendTitle(ServerPlayer player, String titleKey, ChatFormatting titleColor, String subtitleKey, ChatFormatting subtitleColor) {
        Component title = Component.translatable(titleKey).withStyle(titleColor);
        Component subtitle = Component.translatable(subtitleKey).withStyle(subtitleColor);
        player.connection.send(new ClientboundSetTitleTextPacket(title));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 80, 10));
    }

    public void onDefeat() {
        removeSummonedMobs();
        setBattlePhaseActive(false);
        this.stopMusic();
        if (resetDifficultyOnDefeat) setDifficultyLevel(activatingPlayer, 1);
        removeAltarActivationForPlayer(/*player*/);
        playerDeath = false;
        deathDelay = 0;
    }

    private void updateSummonedMobs() {
        if (activatingPlayer == null) return;
        summonedMobs.removeIf(Entity::isRemoved);

        for (Entity entity : summonedMobs) {
            if (entity instanceof Mob mob) {
                if (mob.getTarget() == null || !mob.getTarget().isAlive()) {
                    if (!(activatingPlayer.isCreative() || activatingPlayer.isSpectator())) {
                        double mobDistance = activatingPlayer.distanceToSqr(entity);
                        if (mobDistance <= 16 * 16) {
                            mob.setTarget(activatingPlayer);
                        }
                    }
                }
            }

            double mobDistanceToAltar = entity.distanceToSqr(worldPosition.getCenter());
            if (mobDistanceToAltar > mobTeleportDistance * mobTeleportDistance) {
                List<BlockPos> spawnPositions = findValidSpawnPositions(level, worldPosition, activatingPlayer);
                if (!spawnPositions.isEmpty()) {
                    BlockPos teleportPos = spawnPositions.get(level.random.nextInt(spawnPositions.size()));
                    entity.teleportTo(teleportPos.getX() + 0.5, teleportPos.getY(), teleportPos.getZ() + 0.5);
                }
            }
        }
    }

    private void runAutoWave() {
        if (!battlePhaseActive || !canSummonMobs()) return;
        long gameTime = level.getGameTime();
        if (gameTime % 5 != 0) return;
        BlockState state = level.getBlockState(worldPosition);

        if (!(state.getBlock() instanceof AltarBlock customBlock)) {
            return;
        }

        customBlock.use(
                state,
                level,
                worldPosition,
                activatingPlayer,
                InteractionHand.OFF_HAND,
                new BlockHitResult(Vec3.atCenterOf(worldPosition), Direction.UP, worldPosition, false)
        );

        if (!isBattleOngoing(getDifficultyLevel(activatingPlayer))) {
            removeAltarActivationForPlayer();
            return;
        }

        for (int i = 0; i < 5; i++) {
            int finalI = i;
            Scheduler.schedule(() -> {
                if (activatingPlayer instanceof ServerPlayer serverPlayer) {
                    Component title = Component.translatable(String.valueOf(5 - finalI));
                    serverPlayer.connection.send(new ClientboundSetTitleTextPacket(title));
                    serverPlayer.connection.send(new ClientboundSetTitlesAnimationPacket(5, 15, 0));
                }
            }, 20 * i);
        }

        Scheduler.schedule(() -> {
            customBlock.use(
                    state,
                    level,
                    worldPosition,
                    activatingPlayer,
                    InteractionHand.OFF_HAND,
                    new BlockHitResult(Vec3.atCenterOf(worldPosition), Direction.UP, worldPosition, false)
            );
        }, 100);
    }
    // TODO если каким то образом выпадет снег то мобы не смогу заспавниться, поэтому центральный блок надо возможно проверять на коллизию
    public List<BlockPos> findValidSpawnPositions(Level level, BlockPos center, Player player) {
        List<BlockPos> validPositions = new ArrayList<>();

        if (!allowWaterAndAirSpawn) {
            for (int x = -mobSpawnRadius; x <= mobSpawnRadius; x++) {
                for (int z = -mobSpawnRadius; z <= mobSpawnRadius; z++) {
                    BlockPos currentPos = center.offset(x, 0, z);

                    if (x * x + z * z <= mobSpawnRadius * mobSpawnRadius &&
                            level.isEmptyBlock(currentPos) &&
                            level.isEmptyBlock(currentPos.above()) &&
                            level.isEmptyBlock(currentPos.above(2)) &&
                            level.isEmptyBlock(currentPos.north()) && level.isEmptyBlock(currentPos.south()) &&
                            level.isEmptyBlock(currentPos.east()) && level.isEmptyBlock(currentPos.west()) &&
                            !level.isEmptyBlock(currentPos.below()) &&
                            player.blockPosition().distSqr(currentPos) > spawnDistanceFromPlayer * spawnDistanceFromPlayer) {
                        validPositions.add(currentPos);
                    }
                }
            }
            return validPositions;
        }else{
            for (int x = -mobSpawnRadius; x <= mobSpawnRadius; x++) {
                for (int z = -mobSpawnRadius; z <= mobSpawnRadius; z++) {
                    BlockPos currentPos = center.offset(x, 0, z);

                    if (x * x + z * z <= mobSpawnRadius * mobSpawnRadius &&
                            level.getBlockState(currentPos).getCollisionShape(level, currentPos).isEmpty() &&
                            level.getBlockState(currentPos.above()).getCollisionShape(level, currentPos.above()).isEmpty() &&
                            level.getBlockState(currentPos.above(2)).getCollisionShape(level, currentPos.above(2)).isEmpty() &&
                            level.getBlockState(currentPos.north()).getCollisionShape(level, currentPos.north()).isEmpty() &&
                            level.getBlockState(currentPos.south()).getCollisionShape(level, currentPos.south()).isEmpty() &&
                            level.getBlockState(currentPos.east()).getCollisionShape(level, currentPos.east()).isEmpty() &&
                            level.getBlockState(currentPos.west()).getCollisionShape(level, currentPos.west()).isEmpty() &&
                            player.blockPosition().distSqr(currentPos) > spawnDistanceFromPlayer * spawnDistanceFromPlayer) {
                        validPositions.add(currentPos);
                    }
                }
            }
            return validPositions;
        }
    }

    public void applyGlowEffectToSummonedMobs(Player pPlayer) {
        glowingCounter++;

        if (glowingCounter > 4) {
            int duration = 12000;
            int amplifier = 0;
            glowingCounter = 0;

            int affectedMobs = 0;

            for (Entity mob : summonedMobs) {
                if (mob instanceof LivingEntity livingMob) {
                    livingMob.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, amplifier, false, false));
                    affectedMobs++;
                }
            }
            if (Config.ENABLE_UNCLAIMED_REWARD_MESSAGE.isTrue()) {
                if (!firstMessageSent) {
                    pPlayer.displayClientMessage(Component.translatable("message.skyarena.unclaimed_reward"), false);
                    firstMessageSent = true;
                } else {
                    pPlayer.displayClientMessage(Component.translatable("message.skyarena.mobs_glowing", affectedMobs), true);
                }
                putPlayerMessageTimestamps(pPlayer);
            }
        }
    }

    public boolean isBattleOngoing(int difficultyLevel) {
        for (DifficultyLevelRange range : difficultyLevelRanges) {
            int start = range.range.get(0);
            int end = range.range.get(1);
            if (difficultyLevel >= start && difficultyLevel <= end) {
                return true;
            }
        }
        return presetWaves.containsKey(difficultyLevel);
    }

    public record LootReward(String rewardLootTable, int rewardCount) {}

    public LootReward getRewardFromDifficultyRanges(int difficultyLevel) {
        for (DifficultyLevelRange range : difficultyLevelRanges) {
            int start = range.range.get(0);
            int end = range.range.get(1);
            if (difficultyLevel >= start && difficultyLevel <= end) {
                String lootTable = range.rewardLootTable;
                int count = range.rewardCount;
                return new LootReward(lootTable, count);
            }
        }
        return null;
    }

    public List<ExpandedMobInfo> getAvailableMobs(int difficultyLevel) {
        List<ExpandedMobInfo> result = new ArrayList<>();

        for (DifficultyLevelRange range : difficultyLevelRanges) {
            int start = range.range.get(0);
            int end = range.range.get(1);
            if (difficultyLevel >= start && difficultyLevel <= end) {
                for (String groupId : range.mobGroupsUsed) {
                    MobGroup group = mobGroups.get(groupId);
                    if (group == null) continue;

                    for (Map.Entry<String, Integer> entry : group.mobValues.entrySet()) {
                        String mobId = entry.getKey();
                        int cost = entry.getValue();

                        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.parse(mobId))) continue;

                        ExpandedMobInfo info = new ExpandedMobInfo(
                                mobId,
                                cost,
                                group.squadSpawnChance,
                                group.squadSpawnSize,
                                group.statMultiplierCoefficient,
                                group.mobSpawnChance
                        );
                        result.add(info);
                    }
                }
                break;
            }
        }

        return result;
    }

    public void removeSummonedMobs() {
        for (Entity mob : summonedMobs) {
            if (mob != null && mob.isAlive()) {
                mob.remove(Entity.RemovalReason.DISCARDED);
            }
        }
        summonedMobs.clear();
    }

    public int getPoints(Player player) {
        int difficulty = getDifficultyLevel(player);
        int completedLevels = difficulty - 1;
        int totalPoints = startingPoints;

        for (DifficultyLevelRange range : difficultyLevelRanges) {
            int start = range.range.get(0);
            int end = range.range.get(1);

            if (completedLevels >= start) {
                int levelsPassedInRange = Math.min(completedLevels, end) - start + 1;
                totalPoints += levelsPassedInRange * range.pointsIncrease;
            }
        }

        return totalPoints;
    }

    public double getStatMultiplier(Player player) {
        int difficulty = getDifficultyLevel(player);
        int completedLevels = difficulty - 1;
        double totalStatMultiplier = startingStatMultiplier;

        for (DifficultyLevelRange range : difficultyLevelRanges) {
            int start = range.range.get(0);
            int end = range.range.get(1);

            if (completedLevels >= start) {
                int levelsPassedInRange = Math.min(completedLevels, end) - start + 1;
                totalStatMultiplier += levelsPassedInRange * range.statMultiplierIncrease;
            }
        }

        return totalStatMultiplier;
    }

    public int getDifficultyLevel(Player player) {
        if (individualPlayerStats) {
            String key = player.getGameProfile().getName() + "_" + this.arenaType;
            return playerDifficulty.getOrDefault(key, 1);
        }
        return difficultyLevel;
    }

    public void increaseDifficultyLevel(Player player) {
        if (individualPlayerStats) {
            String key = player.getGameProfile().getName() + "_" + this.arenaType;
            playerDifficulty.put(key, getDifficultyLevel(player) + 1);
        } else {
            this.difficultyLevel++;
        }

        if (this.level != null) {
            this.level.blockEntityChanged(this.getBlockPos());
        }
    }

    public void setDifficultyLevel(Player player, int level) {
        if (player == null) return;
        if (individualPlayerStats) {
            String key = player.getGameProfile().getName() + "_" + this.arenaType;
            playerDifficulty.put(key, level);
        } else {
            this.difficultyLevel = level;
        }

        if (this.level != null) {
            this.level.blockEntityChanged(this.getBlockPos());
        }
    }

    public void setRecordItem(ItemStack stack) {
        if (stack.get(DataComponents.JUKEBOX_PLAYABLE) != null) {
            this.recordItem = stack.copy();

            if (this.level != null) {
                this.level.blockEntityChanged(this.getBlockPos());
            }
        }
    }

    public void clearRecordItem() {
        this.recordItem = ItemStack.EMPTY;

        if (this.level != null) {
            this.level.blockEntityChanged(this.getBlockPos());
        }
    }

    public void addSummonedMob(Entity mob) {
        summonedMobs.add(mob);
    }

    public boolean canSummonMobs() {
        summonedMobs.removeIf(Entity::isRemoved);
        return summonedMobs.isEmpty();
    }

    public void recordAltarActivation(Player player, BlockPos pos) {
        activeAltarBlocks.put(player, pos);
        activatingPlayer = player;
    }

    public static BlockPos getAltarPosForPlayer(Player player) {
        return activeAltarBlocks.get(player);
    }

    public void removeAltarActivationForPlayer() {
        activeAltarBlocks.remove(activatingPlayer);
    }

    public void putPlayerMessageTimestamps(Player player){
        playerMessageTimestamps.put(player, level.getGameTime());
    }

    public boolean isResetDifficultyOnDefeat() {
        return resetDifficultyOnDefeat;
    }

    public boolean isAutoWaveRun() {
        return autoWaveRun;
    }

    public double getPointsCoefficientPerBlocks() {
        return pointsCoefficientPerBlocks;
    }

    public double getStatMultiplierCoefficientPerBlocks() {
        return statMultiplierCoefficientPerBlocks;
    }

    public double getLootTableCountCoefficientPerBlocks() {
        return lootTableCountCoefficientPerBlocks;
    }

    public int getFullProtectionRadius() {
        return fullProtectionRadius;
    }

    public int getStartingPoints() {
        return startingPoints;
    }

    public void setGlowingCounter(int glowingCounter) {
        this.glowingCounter = glowingCounter;
    }

    public void setPlayerDeath(boolean death) {
        playerDeath = death;
    }

    public int getBattleDifficultyLevel() {
        return battleDifficultyLevel;
    }

    public void setBattleDifficultyLevel(int battleDifficultyLevel) {
        this.battleDifficultyLevel = battleDifficultyLevel;
    }

    public String getArenaType() {
        return arenaType;
    }

    public boolean isBattlePhaseActive() {
        return battlePhaseActive;
    }

    public int getSpawnDistanceFromPlayer() {
        return spawnDistanceFromPlayer;
    }

    public int getBattleLossDistance() {
        return battleLossDistance;
    }

    public int getMobTeleportDistance() {
        return mobTeleportDistance;
    }

    public boolean isAllowWaterAndAirSpawn() {
        return allowWaterAndAirSpawn;
    }

    public boolean isIndividualPlayerStats() {
        return individualPlayerStats;
    }

    public int getMobSpawnRadius() {
        return mobSpawnRadius;
    }

    public int getMobCostRatio() {
        return mobCostRatio;
    }

    public boolean isSetNight() {
        return setNight;
    }

    public boolean isSetRain() {
        return setRain;
    }

    public boolean isDisableMobItemDrop() {
        return disableMobItemDrop;
    }

    public boolean isAllowDifficultyReset() {
        return allowDifficultyReset;
    }

    public int getMobGriefingProtectionRadius() {
        return mobGriefingProtectionRadius;
    }

    public int getBossBarHideRadius() {
        return bossBarHideRadius;
    }

    public Map<Integer, PresetWave> getPresetWaves() {
        return this.presetWaves;
    }

    public long getBattleEndTime() {
        return battleEndTime;
    }

    public void setBattleEndTime(long battleEndTime) {
        this.battleEndTime = battleEndTime;
    }

    public double getStartingStatMultiplier() {
        return startingStatMultiplier;
    }

    public List<DifficultyLevelRange> getDifficultyLevelRanges() {
        return difficultyLevelRanges;
    }

    public LinkedHashMap<String, MobGroup> getMobGroups() {
        return mobGroups;
    }

    public void setBattlePhaseActive(boolean battlePhaseActive) {
        this.battlePhaseActive = battlePhaseActive;
    }

    public Player getActivatingPlayer() {
        return activatingPlayer;
    }
}
