package justfatlard.better_glass;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class CrackedGlassPaneBlock extends IronBarsBlock {
	public CrackedGlassPaneBlock(Properties properties) {
		super(properties);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
		Fragile.placed(level, pos, this);
	}

	@Override
	public void stepOn(Level level, BlockPos pos, BlockState onState, Entity entity) {
		Fragile.steppedOn(level, pos, this);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		Fragile.tick(state, level, pos);
	}
}
