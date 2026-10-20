package justfatlard.better_glass.gametest;

import justfatlard.better_glass.Fragile;
import justfatlard.better_glass.GlassShardLoot;
import justfatlard.better_glass.Main;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Glass broken by hand gives back its shards, one short now and then, and silk touch still takes
 * the block whole. Tinted glass is untouched: it already comes back whole. Cracked glass set in
 * place gives way about as often as it should, and leaves its shards when it does; it holds weight
 * only for a while, and comes apart faster than glass.
 */
public final class GlassBreaksIntoShards implements FabricClientGameTest {
	private static final int ROLLS = 200;

	/** A grid of placements, spaced so no two touch. */
	private static final int GRID = 15;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getServer().runOnServer(server -> {
				ServerLevel level = server.overworld();
				ItemStack silk = new ItemStack(Items.DIAMOND_PICKAXE);
				silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);

				shards(level, Blocks.GLASS, GlassShardLoot.PER_BLOCK);
				shards(level, Blocks.STAINED_GLASS.red(), GlassShardLoot.PER_BLOCK);
				shards(level, Main.CRACKED_GLASS, GlassShardLoot.PER_BLOCK);
				shards(level, Blocks.GLASS_PANE, GlassShardLoot.PER_PANE);
				shards(level, Blocks.STAINED_GLASS_PANE.black(), GlassShardLoot.PER_PANE);
				shards(level, Main.CRACKED_GLASS_PANE, GlassShardLoot.PER_PANE);

				for (Block block : List.of(Blocks.GLASS, Blocks.GLASS_PANE, Main.CRACKED_GLASS, Main.CRACKED_GLASS_PANE)) {
					expect(drops(level, block, silk), block.asItem(), 1, block + " under silk touch");
				}
				expect(drops(level, Blocks.TINTED_GLASS, ItemStack.EMPTY), Items.TINTED_GLASS, 1, "tinted glass by hand");

				for (Block block : List.of(Main.CRACKED_GLASS, Main.CRACKED_GLASS_PANE)) {
					float cracked = block.defaultBlockState().getDestroySpeed(level, BlockPos.ZERO);
					float whole = Blocks.GLASS.defaultBlockState().getDestroySpeed(level, BlockPos.ZERO);
					if (cracked >= whole) throw new AssertionError(block + " takes " + cracked + " to break, no quicker than glass's " + whole);
				}
			});

			TestServerContext server = world.getServer();
			BlockPos origin = server.computeOnServer(s -> world.getConnection().getServerPlayer().blockPosition().above(30));
			bearsWeightForAWhile(context, server, origin);
			for (Block block : List.of(Main.CRACKED_GLASS, Main.CRACKED_GLASS_PANE)) {
				server.runOnServer(s -> {
					ServerLevel level = s.overworld();
					for (BlockPos pos : grid(origin)) {
						level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
						block.setPlacedBy(level, pos, block.defaultBlockState(), null, new ItemStack(block));
					}
				});
				context.waitTicks(20);
				server.runOnServer(s -> {
					ServerLevel level = s.overworld();
					int placed = GRID * GRID;
					int standing = 0;
					for (BlockPos pos : grid(origin)) {
						if (level.getBlockState(pos).is(block)) standing++;
						level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
					}
					int broke = placed - standing;
					double expected = placed * Fragile.SHATTER_CHANCE;
					double spread = Math.sqrt(expected * (1 - Fragile.SHATTER_CHANCE));
					if (Math.abs(broke - expected) > 4 * spread) {
						throw new AssertionError(broke + " of " + placed + " placed " + block + " broke, not about "
							+ Math.round(placed * Fragile.SHATTER_CHANCE));
					}
					AABB around = new AABB(origin).inflate(2 * GRID + 2, 32, 2 * GRID + 2);
					List<ItemEntity> shards = level.getEntitiesOfClass(ItemEntity.class, around, item -> item.getItem().is(Main.GLASS_SHARD));
					if (shards.isEmpty()) throw new AssertionError(block + " broke on placing and left no shards");
					shards.forEach(ItemEntity::discard);
				});
			}
		}
	}

	/** A stand left on one block goes through it in time; the empty one beside it stays. */
	private static void bearsWeightForAWhile(ClientGameTestContext context, TestServerContext server, BlockPos origin) {
		BlockPos loaded = origin.offset(-4, 0, 0);
		BlockPos empty = origin.offset(-6, 0, 0);
		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			for (BlockPos pos : List.of(loaded, empty)) {
				level.setBlock(pos, Main.CRACKED_GLASS.defaultBlockState(), Block.UPDATE_ALL);
				// Stepped on once, as anything landing on it does.
				Main.CRACKED_GLASS.stepOn(level, pos, Main.CRACKED_GLASS.defaultBlockState(), null);
			}
		});
		server.runCommand("summon minecraft:armor_stand %d %d %d".formatted(loaded.getX(), loaded.getY() + 1, loaded.getZ()));
		context.waitTicks(Fragile.BEARS_TICKS - 20);
		server.runOnServer(s -> {
			if (!s.overworld().getBlockState(loaded).is(Main.CRACKED_GLASS)) {
				throw new AssertionError("cracked glass gave way under a stand before " + Fragile.BEARS_TICKS + " ticks");
			}
		});
		context.waitTicks(40);
		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			if (level.getBlockState(loaded).is(Main.CRACKED_GLASS)) {
				throw new AssertionError("cracked glass still bears a stand after " + (Fragile.BEARS_TICKS + 20) + " ticks");
			}
			if (!level.getBlockState(empty).is(Main.CRACKED_GLASS)) {
				throw new AssertionError("cracked glass with nothing on it gave way");
			}
			level.setBlock(empty, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		});
		server.runCommand("kill @e[type=minecraft:armor_stand]");
	}

	private static List<BlockPos> grid(BlockPos origin) {
		List<BlockPos> cells = new ArrayList<>();
		for (int i = 0; i < GRID; i++) {
			for (int j = 0; j < GRID; j++) cells.add(origin.offset(2 * i, 0, 2 * j));
		}
		return cells;
	}

	private static void shards(ServerLevel level, Block block, int count) {
		Set<Integer> seen = new HashSet<>();
		for (int i = 0; i < ROLLS; i++) {
			List<ItemStack> drops = drops(level, block, ItemStack.EMPTY);
			if (drops.size() != 1 || !drops.getFirst().is(Main.GLASS_SHARD)) {
				throw new AssertionError(block + " by hand dropped " + drops + ", not shards");
			}
			seen.add(drops.getFirst().getCount());
		}
		if (!seen.equals(Set.of(count, count - 1))) {
			throw new AssertionError(block + " by hand dropped shard counts " + seen + " over " + ROLLS + " breaks, not "
				+ count + " and now and then " + (count - 1));
		}
	}

	private static List<ItemStack> drops(ServerLevel level, Block block, ItemStack tool) {
		return Block.getDrops(block.defaultBlockState(), level, BlockPos.ZERO, null, null, tool);
	}

	private static void expect(List<ItemStack> drops, net.minecraft.world.item.Item item, int count, String what) {
		if (drops.size() != 1 || !drops.getFirst().is(item) || drops.getFirst().getCount() != count) {
			throw new AssertionError(what + " dropped " + drops + ", not " + count + " " + item);
		}
	}
}
