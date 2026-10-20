package justfatlard.better_glass;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;

/**
 * Cracked glass does not hold up: now and then it gives way a moment after being set in place, and
 * anything standing on it for too long goes through it.
 */
public final class Fragile {
	public static final float SHATTER_CHANCE = 0.3F;

	/** How long it bears weight before it gives way. */
	public static final int BEARS_TICKS = 60;

	/** Long enough to see it go in before it goes. */
	private static final int PLACED_DELAY_TICKS = 6;

	/** How often it looks to see whether it is still being stood on. */
	private static final int CHECK_TICKS = 5;

	/** Ticks of weight borne so far, by block; BEARS_TICKS or more and it goes on its next tick. */
	private static final Map<GlobalPos, Integer> STRAIN = new HashMap<>();

	private Fragile() {}

	static void placed(Level level, BlockPos pos, Block block) {
		if (level instanceof ServerLevel server && server.getRandom().nextFloat() < SHATTER_CHANCE) {
			STRAIN.put(GlobalPos.of(server.dimension(), pos), BEARS_TICKS);
			server.scheduleTick(pos, block, PLACED_DELAY_TICKS);
		}
	}

	static void steppedOn(Level level, BlockPos pos, Block block) {
		if (level instanceof ServerLevel server && !server.getBlockTicks().hasScheduledTick(pos, block)) {
			STRAIN.remove(GlobalPos.of(server.dimension(), pos));
			server.scheduleTick(pos, block, CHECK_TICKS);
		}
	}

	static void tick(BlockState state, ServerLevel level, BlockPos pos) {
		GlobalPos key = GlobalPos.of(level.dimension(), pos);
		int strain = STRAIN.getOrDefault(key, 0);
		if (strain < BEARS_TICKS) {
			if (!borne(level, pos)) {
				STRAIN.remove(key);
				level.destroyBlockProgress(crackId(pos), pos, -1);
				return;
			}
			strain += CHECK_TICKS;
			STRAIN.put(key, strain);
			level.destroyBlockProgress(crackId(pos), pos, Math.min(9, strain * 10 / BEARS_TICKS));
			level.playSound(null, pos, state.getSoundType().getHitSound(), SoundSource.BLOCKS, 0.5F, 0.6F + 0.4F * strain / BEARS_TICKS);
			if (strain < BEARS_TICKS) {
				level.scheduleTick(pos, state.getBlock(), CHECK_TICKS);
				return;
			}
		}
		STRAIN.remove(key);
		level.destroyBlockProgress(crackId(pos), pos, -1);
		// Broken as if by hand, so it leaves its shards.
		level.destroyBlock(pos, true);
	}

	/** Anything alive standing on top of it. */
	private static boolean borne(ServerLevel level, BlockPos pos) {
		AABB top = new AABB(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 1.1, pos.getZ() + 1);
		return !level.getEntitiesOfClass(LivingEntity.class, top, EntitySelector.NO_SPECTATORS.and(entity -> entity.onGround())).isEmpty();
	}

	/** The breaking overlay is keyed by who is breaking; entity ids are never negative, so these never clash with one. */
	private static int crackId(BlockPos pos) {
		return -1 - (pos.hashCode() & Integer.MAX_VALUE);
	}
}
