package justfatlard.better_glass.gametest;

import justfatlard.pandorical.api.PandoricalApi;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The pictures for the readme and the mod page: walls of glass and panes joined into sheets, with
 * something behind them to see through to.
 *
 * <p>Run it under xvfb-run; the frames land in build/run/clientGameTest/screenshots.
 */
public final class Showcase implements FabricClientGameTest {
	private static final int WIDTH = 1920;
	private static final int HEIGHT = 1080;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			TestServerConnection connection = world.getConnection();
			connection.waitForChunksRender();
			server.waitFor(s -> PandoricalApi.isAvailable(connection.getServerPlayer()));

			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule advance_weather false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("time set noon");
			server.runCommand("gamemode spectator @a");

			BlockPos origin = server.computeOnServer(s -> connection.getServerPlayer().blockPosition());
			int x = origin.getX();
			int y = origin.getY() + 20;
			int z = origin.getZ();

			// A floor and a backdrop in the air, clear of whatever terrain the seed put here.
			fill(server, x - 12, y - 1, z - 14, x + 12, y - 1, z + 2, "minecraft:smooth_stone");
			fill(server, x - 12, y, z - 14, x + 12, y + 6, z - 14, "minecraft:bricks");
			fill(server, x - 8, y, z - 12, x - 6, y + 2, z - 12, "minecraft:oak_log");

			// Left to right: clear glass, a stained wall, a window of panes, and the cracked kinds.
			fill(server, x - 10, y, z - 8, x - 7, y + 3, z - 8, "minecraft:glass");
			fill(server, x - 5, y, z - 8, x - 3, y + 2, z - 8, "minecraft:light_blue_stained_glass");
			fill(server, x - 5, y + 3, z - 8, x - 3, y + 3, z - 8, "minecraft:tinted_glass");
			fill(server, x - 1, y, z - 8, x + 3, y + 3, z - 8, "minecraft:glass_pane[east=true,west=true]");
			server.runCommand("setblock %d %d %d minecraft:glass_pane[east=true]".formatted(x - 1, y + 4, z - 8));
			fill(server, x + 5, y, z - 8, x + 6, y + 1, z - 8, "better-glass-justfatlard:cracked_glass");
			fill(server, x + 8, y, z - 8, x + 10, y + 2, z - 8, "better-glass-justfatlard:cracked_glass_pane[east=true,west=true]");
			fill(server, x + 5, y + 2, z - 8, x + 6, y + 3, z - 8, "minecraft:red_stained_glass_pane[east=true,west=true]");
			// A pane run that turns a corner and ends against a wall.
			fill(server, x - 10, y, z - 5, x - 10, y + 1, z - 3, "minecraft:glass_pane[north=true,south=true]");
			fill(server, x - 11, y, z - 6, x - 11, y + 1, z - 6, "minecraft:stone_bricks");

			look(server, x + 0.5, y + 1.5, z + 1.5, x + 0.5, y + 1.5, z - 8.0);
			context.waitTicks(100);
			connection.waitForChunksRender();
			shoot(context, "glass-front");

			look(server, x - 14.5, y + 4.0, z - 1.5, x - 6.0, y + 1.0, z - 8.0);
			context.waitTicks(40);
			connection.waitForChunksRender();
			shoot(context, "glass-angle");

			// Curves: a hollow sphere of glass, and a round tower of panes, each pane joined to
			// the ones beside it.
			int cz = z - 26;
			server.runOnServer(s -> {
				ServerLevel level = s.overworld();
				sphere(level, new BlockPos(x - 7, y + 5, cz), 5);
				ring(level, x + 7, y, cz, 5, 4);
			});
			look(server, x + 0.5, y + 8.0, z - 12.5, x + 0.5, y + 3.5, cz);
			context.waitTicks(40);
			connection.waitForChunksRender();
			shoot(context, "glass-curves");
		}
	}

	private static void sphere(ServerLevel level, BlockPos centre, int radius) {
		for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-radius, -radius, -radius), centre.offset(radius, radius, radius))) {
			double d = Math.sqrt(pos.distSqr(centre));
			if (d <= radius && d > radius - 1.2) level.setBlock(pos, Blocks.GLASS.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/** A circle walked so every step is a side step, since panes do not join corner to corner. */
	private static void ring(ServerLevel level, int cx, int y, int cz, int radius, int height) {
		Set<BlockPos> cells = new LinkedHashSet<>();
		BlockPos last = null;
		for (int i = 0; i <= 360; i++) {
			double a = Math.toRadians(i);
			BlockPos cell = new BlockPos(cx + (int) Math.round(radius * Math.cos(a)), 0, cz + (int) Math.round(radius * Math.sin(a)));
			if (last != null && last.getX() != cell.getX() && last.getZ() != cell.getZ()) {
				cells.add(new BlockPos(cell.getX(), 0, last.getZ()));
			}
			cells.add(cell);
			last = cell;
		}
		for (BlockPos cell : cells) {
			BlockState pane = Blocks.GLASS_PANE.defaultBlockState();
			for (Direction side : Direction.Plane.HORIZONTAL) {
				pane = pane.setValue(CrossCollisionBlock.PROPERTY_BY_DIRECTION.get(side), cells.contains(cell.relative(side)));
			}
			for (int dy = 0; dy < height; dy++) level.setBlock(cell.atY(y + dy), pane, Block.UPDATE_CLIENTS);
		}
	}

	private static void fill(TestServerContext server, int x1, int y1, int z1, int x2, int y2, int z2, String block) {
		server.runCommand("fill %d %d %d %d %d %d %s".formatted(x1, y1, z1, x2, y2, z2, block));
	}

	/**
	 * Stand the camera at one place and point it at another. The camera's y is the feet, so it
	 * looks from 1.62 above where it stands.
	 */
	private static void look(TestServerContext server, double x, double y, double z,
			double atX, double atY, double atZ) {
		double dx = atX - x;
		double dy = atY - (y + 1.62);
		double dz = atZ - z;
		double yaw = -Math.toDegrees(Math.atan2(dx, dz));
		double pitch = -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		server.runCommand("tp @a %.2f %.2f %.2f %.1f %.1f".formatted(x, y, z, yaw, pitch));
	}

	private static void shoot(ClientGameTestContext context, String name) {
		context.takeScreenshot(TestScreenshotOptions.of(name)
			.withSize(WIDTH, HEIGHT)
			.disableCounterPrefix());
	}
}
