package net.jrdemiurge.skyarena.block;

import net.jrdemiurge.skyarena.SkyArena;
import net.jrdemiurge.skyarena.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SkyArena.MOD_ID);

    public static final DeferredBlock<Block> ALTAR_BATTLE = registerBlock("altar_battle",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .noOcclusion()
                    .lightLevel((state) -> 5)
                    .strength(-1.0F, 3600000.0F)
            ));

    public static final DeferredBlock<Block> ALTAR_BATTLE_TOP = registerBlock("altar_battle_top",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .noOcclusion()
                    .lightLevel((state) -> 5)
                    .strength(-1.0F, 3600000.0F)
            ));

    public static final DeferredBlock<Block> OAK_TROPHY = registerBlock("oak_trophy",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
                    .noOcclusion().
                    lightLevel((state) -> 5)
            ));

    public static final DeferredBlock<Block> STONE_TROPHY = registerBlock("stone_trophy",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                    .noOcclusion().
                    lightLevel((state) -> 5)
            ));

    public static final DeferredBlock<Block> IRON_TROPHY = registerBlock("iron_trophy",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .noOcclusion().
                    lightLevel((state) -> 5)
            ));

    public static final DeferredBlock<Block> GOLD_TROPHY = registerBlock("gold_trophy",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_BLOCK)
                    .noOcclusion().
                    lightLevel((state) -> 5)
            ));


    public static final DeferredBlock<Block> DIAMOND_TROPHY = registerBlock("diamond_trophy",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_BLOCK)
                    .noOcclusion().
                    lightLevel((state) -> 5)
            ));

    public static final DeferredBlock<Block> NETHERITE_TROPHY = registerBlock("netherite_trophy",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK)
                    .noOcclusion().
                    lightLevel((state) -> 5)
            ));


    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
