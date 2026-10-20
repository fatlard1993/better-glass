package justfatlard.better_glass;

import justfatlard.pandorical.api.BlockRegistration;
import justfatlard.pandorical.api.ItemRegistration;
import justfatlard.pandorical.api.PandoricalApi;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class Main implements ModInitializer {
	public static final String MOD_ID = "better-glass-justfatlard";

	/** A third of glass's, so it comes apart almost at a touch. */
	private static final float CRACKED_DESTROY_TIME = 0.1F;

	public static final CrackedGlassBlock CRACKED_GLASS = new CrackedGlassBlock(
		BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).destroyTime(CRACKED_DESTROY_TIME).setId(blockKey("cracked_glass")));

	public static final CrackedGlassPaneBlock CRACKED_GLASS_PANE = new CrackedGlassPaneBlock(
		BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS_PANE).destroyTime(CRACKED_DESTROY_TIME).setId(blockKey("cracked_glass_pane")));

	public static final Item GLASS_SHARD = new Item(new Item.Properties().setId(itemKey("glass_shard")));

	@Override
	public void onInitialize() {
		register("cracked_glass", CRACKED_GLASS);
		register("cracked_glass_pane", CRACKED_GLASS_PANE);
		Registry.register(BuiltInRegistries.ITEM, itemKey("glass_shard"), GLASS_SHARD);

		if (PandoricalApi.isAvailable()) {
			PandoricalApi.content().registerBlock(MOD_ID + ":cracked_glass", new BlockRegistration()
				.baseBlock("minecraft:glass")
				.model(MOD_ID + ":block/cracked_glass"));
			PandoricalApi.content().registerBlock(MOD_ID + ":cracked_glass_pane", new BlockRegistration()
				.baseBlock("minecraft:glass_pane")
				.model(MOD_ID + ":block/cracked_glass_pane_post"));
			PandoricalApi.content().registerItem(MOD_ID + ":cracked_glass", new ItemRegistration()
				.model(MOD_ID + ":block/cracked_glass"));
			PandoricalApi.content().registerItem(MOD_ID + ":cracked_glass_pane", new ItemRegistration()
				.model(MOD_ID + ":item/cracked_glass_pane"));
			PandoricalApi.content().registerItem(MOD_ID + ":glass_shard", new ItemRegistration()
				.model(MOD_ID + ":item/glass_shard"));
			// Also carries the connected models for vanilla's glass, which live in the minecraft
			// namespace beside the blocks they belong to.
			PandoricalApi.content().registerModAssets(MOD_ID);
		}

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COLORED_BLOCKS).register(content -> {
			content.accept(CRACKED_GLASS);
			content.accept(CRACKED_GLASS_PANE);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(content ->
			content.accept(GLASS_SHARD));

		GlassShardLoot.register();

		System.out.println("[" + MOD_ID + "] Loaded");
	}

	private static void register(String name, Block block) {
		Registry.register(BuiltInRegistries.BLOCK, blockKey(name), block);
		Registry.register(BuiltInRegistries.ITEM, itemKey(name),
			new BlockItem(block, new Item.Properties().setId(itemKey(name)).useBlockDescriptionPrefix()));
	}

	private static ResourceKey<Block> blockKey(String name) {
		return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, name));
	}

	private static ResourceKey<Item> itemKey(String name) {
		return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, name));
	}
}
