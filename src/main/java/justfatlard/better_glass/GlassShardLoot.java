package justfatlard.better_glass;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableSource;
import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.advancements.predicates.EnchantmentPredicate;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.predicates.DataComponentPredicates;
import net.minecraft.core.component.predicates.EnchantmentsPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/**
 * Glass broken without silk touch comes apart into shards rather than nothing.
 *
 * <p>Vanilla's glass tables hold one pool, the block itself under silk touch, so this appends the
 * other half: every shard it took to make, nine to a block and four to a pane, as the recipes
 * putting them back count them, and now and then one short. Tinted glass is left alone, since it
 * already comes back whole however it is broken.
 */
public final class GlassShardLoot {
	public static final int PER_BLOCK = 9, PER_PANE = 4;
	private static final float ONE_LOST = 0.1F;

	private static final Set<ResourceKey<LootTable>> BLOCKS =
		tables(Stream.concat(Stream.of(Blocks.GLASS), Blocks.STAINED_GLASS.asList().stream()));

	private static final Set<ResourceKey<LootTable>> PANES =
		tables(Stream.concat(Stream.of(Blocks.GLASS_PANE), Blocks.STAINED_GLASS_PANE.asList().stream()));

	private GlassShardLoot() {}

	/** Read off the blocks rather than written out, so a renamed vanilla table cannot silently miss. */
	private static Set<ResourceKey<LootTable>> tables(Stream<Block> blocks) {
		return blocks.map(Block::getLootTable).flatMap(Optional::stream).collect(Collectors.toUnmodifiableSet());
	}

	public static void register() {
		LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
			// Only vanilla's own copy: a datapack that rewrote a glass table said what it wanted.
			if (source != LootTableSource.VANILLA) return;
			if (BLOCKS.contains(key)) builder.pool(shards(PER_BLOCK, registries).build());
			else if (PANES.contains(key)) builder.pool(shards(PER_PANE, registries).build());
		});
	}

	private static LootPool.Builder shards(int count, HolderLookup.Provider registries) {
		return LootPool.lootPool()
			.setRolls(ContextIntProviders.exactly(1))
			.add(LootItem.lootTableItem(Main.GLASS_SHARD)
				.apply(SetItemCountFunction.setCount(ContextIntProviders.exactly(count)))
				.apply(SetItemCountFunction.setCount(ContextIntProviders.exactly(-1), true)
					.when(LootItemRandomChanceCondition.randomChance(ONE_LOST))))
			.when(hasSilkTouch(registries).invert())
			.when(ExplosionCondition.survivesExplosion());
	}

	private static LootItemCondition.Builder hasSilkTouch(HolderLookup.Provider registries) {
		return MatchTool.toolMatches(ItemPredicate.Builder.item()
			.withComponents(DataComponentMatchers.Builder.components()
				.partial(DataComponentPredicates.ENCHANTMENTS, EnchantmentsPredicate.enchantments(List.of(
					new EnchantmentPredicate(
						registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),
						MinMaxBounds.Ints.atLeast(1)))))
				.build()));
	}
}
