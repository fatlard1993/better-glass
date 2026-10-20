#!/usr/bin/env python3
"""Writes every texture, model, blockstate, recipe, advancement and loot table under src/main/resources.

Rerun after changing anything here; the output is checked in, and nothing else writes it.

Connected glass is drawn by Pandorical's ConnectedFaces provider out of pieces named after the
block: a frameless core, and one frame overlay per edge of each face, drawn only where the glass
really ends there, and a faint one where it only meets more of itself at an odd corner. Each face
also comes bare, without its streaks, for the faces of a sheet that leave them off. The overlays repeat the core's faces exactly, vertex for vertex, so the depth
test lets the later one land on top instead of fighting it.
"""

import json
import shutil
from pathlib import Path

from PIL import Image

MOD_ID = "better-glass-justfatlard"
ROOT = Path(__file__).parent / "src/main/resources"
ASSETS = ROOT / "assets"
DATA = ROOT / "data"

DIRECTIONS = ["down", "up", "north", "south", "west", "east"]

# Vanilla's own dye colours, as its stained glass is painted with them.
STAINED = {
	"white": (255, 255, 255), "orange": (216, 127, 51), "magenta": (178, 76, 216),
	"light_blue": (102, 153, 216), "yellow": (229, 229, 51), "lime": (127, 204, 25),
	"pink": (242, 127, 165), "gray": (76, 76, 76), "light_gray": (153, 153, 153),
	"cyan": (76, 127, 153), "purple": (127, 63, 178), "blue": (51, 76, 178),
	"brown": (102, 76, 51), "green": (102, 127, 51), "red": (153, 51, 51),
	"black": (25, 25, 25),
}

# Which edge of a face's texture meets each neighbouring side, under the game's default UVs.
FACE_EDGE = {
	"up": {"north": "top", "south": "bottom", "west": "left", "east": "right"},
	"down": {"north": "bottom", "south": "top", "west": "left", "east": "right"},
	"north": {"up": "top", "down": "bottom", "east": "left", "west": "right"},
	"south": {"up": "top", "down": "bottom", "west": "left", "east": "right"},
	"west": {"up": "top", "down": "bottom", "north": "left", "south": "right"},
	"east": {"up": "top", "down": "bottom", "south": "left", "north": "right"},
}


def write_json(path, data):
	path.parent.mkdir(parents=True, exist_ok=True)
	path.write_text(json.dumps(data, indent=2) + "\n")


def tex(name):
	return f"{MOD_ID}:block/{name}"


# ---- textures ---------------------------------------------------------------------------------

def blank():
	return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def frame(colour, alpha):
	"""The four one-pixel edges as separate sheets, each running corner to corner."""
	edges = {name: blank() for name in ("top", "bottom", "left", "right")}
	for i in range(16):
		edges["top"].putpixel((i, 0), colour + (alpha,))
		edges["bottom"].putpixel((i, 15), colour + (alpha,))
		edges["left"].putpixel((0, i), colour + (alpha,))
		edges["right"].putpixel((15, i), colour + (alpha,))
	return edges


def glint(image, colour, alpha):
	"""Two short streaks the size of vanilla's, so a wall of it stays see-through."""
	for x, y in ((4, 2), (3, 3), (2, 4), (13, 12), (12, 13)):
		image.putpixel((x, y), colour + (alpha,))


def streaked(bare, colour, alpha):
	inner = bare.copy()
	glint(inner, colour, alpha)
	return inner


def clear_glass():
	bare = blank()
	return streaked(bare, (220, 238, 238), 255), frame((206, 232, 232), 255), bare


def tinted_glass():
	bare = Image.new("RGBA", (16, 16), (42, 40, 43, 110))
	# Laid over a sheet already 110 deep, so this much more lands it at vanilla's 200.
	return streaked(bare, (74, 62, 82), 150), frame((48, 35, 53), 158), bare


def stained_glass(colour):
	bare = Image.new("RGBA", (16, 16), colour + (102,))
	# Over the 102 sheet beneath, this lands the frame at vanilla's 163.
	return streaked(bare, tuple(min(255, c + 40) for c in colour), 155), frame(colour, 102), bare


# Drawn in the manner of vanilla's breaking overlay: one-pixel jagged lines running out of a
# junction and fading at their tips, not a symmetric star. A full crack for some faces, and a
# shorter one, like the overlay's first stages, for the faces between, so a wall of it does not
# repeat one picture. "#" is the line, "+" where it thins out.
CRACK = """
................
.........+......
.........#......
..........#.....
..+.......#.....
...#.....#......
....##..#.......
......##.###....
........#...#+..
........#.......
.......#........
......#.##......
.....#....#.....
....+......+....
................
................
"""

HAIRLINE = """
................
................
................
................
................
..........+.....
.........#......
.........#......
........#.##....
.......#....+...
.......#........
......+.........
................
................
................
................
"""


def cracks(drawing, alpha):
	inner = blank()
	for y, row in enumerate(drawing.strip().splitlines()):
		for x, cell in enumerate(row):
			if cell != ".":
				inner.putpixel((x, y), (236, 246, 246, alpha if cell == "#" else alpha * 3 // 5))
	return inner


def cracked_glass():
	# A crack on every face, but on most faces of a joined sheet only a hairline.
	return cracks(CRACK, 225), frame((206, 232, 232), 255), cracks(HAIRLINE, 200)


def save(image, path):
	path.parent.mkdir(parents=True, exist_ok=True)
	image.save(path)


# How much of the frame an odd corner keeps.
FAINT = 0.35


def faint(image):
	alpha = image.getchannel("A").point(lambda a: round(a * FAINT))
	faded = image.copy()
	faded.putalpha(alpha)
	return faded


# Looks whose bare face has nothing on it, so a bare face of theirs is left out altogether.
EMPTY = set()


def glass_textures(name, inner, edges, bare):
	base = ASSETS / MOD_ID / "textures/block/connected"
	save(inner, base / f"{name}_inner.png")
	save(bare, base / f"{name}_bare.png")
	if bare.getbbox() is None:
		EMPTY.add(name)
	for edge, image in edges.items():
		save(image, base / f"{name}_edge_{edge}.png")
		save(faint(image), base / f"{name}_edge_{edge}_faint.png")


def whole(inner, edges):
	"""The block as it looks alone: sheet and all four edges."""
	image = inner.copy()
	for edge in edges.values():
		image.alpha_composite(edge)
	return image


SHARDS = """
................
....o...........
...oso..........
...oxxo.........
..oxsxo....o....
..oxxxo...oso...
..oxxo....osxo..
.oxxxo...oxxxo..
.oxxo....oxxo...
.oxo.....oxo....
.oo.....oxo.....
.o......oo......
......oo........
.....osxo.......
.....oxxxoo.....
......ooo.......
"""


def shard_item():
	"""Three slivers of glass, outlined so they read on a dark slot."""
	image = blank()
	colours = {"o": (96, 128, 136, 255), "x": (196, 228, 232, 210), "s": (250, 255, 255, 255)}
	for y, row in enumerate(SHARDS.strip("\n").splitlines()):
		for x, cell in enumerate(row):
			if cell in colours:
				image.putpixel((x, y), colours[cell])
	return image


def icon(looks):
	"""Four blocks of glass joined into one window, one of them cracked, with its shards beside it."""
	sheet = blank().resize((32, 32))
	inner, edges, _ = looks["glass"]
	for x, y in ((0, 0), (16, 0), (0, 16)):
		sheet.alpha_composite(inner, (x, y))
	sheet.alpha_composite(looks["cracked_glass"][0], (16, 16))
	outline = {"top": [(0, 0), (16, 0)], "left": [(0, 0), (0, 16)], "right": [(16, 0), (16, 16)], "bottom": [(0, 16), (16, 16)]}
	for edge, spots in outline.items():
		for spot in spots:
			sheet.alpha_composite(edges[edge], spot)
	# The cracked block is a block of its own, so the window keeps a frame round it.
	sheet.alpha_composite(edges["left"], (16, 16))
	sheet.alpha_composite(edges["top"], (16, 16))
	backed = Image.new("RGBA", (32, 32), (92, 146, 196, 255))
	backed.alpha_composite(sheet)
	backed.alpha_composite(shard_item(), (18, 17))
	return backed.resize((128, 128), Image.NEAREST)


# ---- connected models -------------------------------------------------------------------------

CUBE = {"from": [0, 0, 0], "to": [16, 16, 16]}


def connected_templates():
	base = ASSETS / MOD_ID / "models/block/connected"
	write_json(base / "none.json", {"textures": {"particle": "#inner"}, "elements": []})
	for face in DIRECTIONS:
		for suffix, key in (("", "#inner"), ("_bare", "#bare")):
			write_json(base / f"cube_{face}{suffix}.json", {
				"textures": {"particle": "#inner"},
				"elements": [dict(CUBE, faces={face: {"texture": key, "cullface": face}})],
			})
	for face, edges in FACE_EDGE.items():
		for toward, edge in edges.items():
			for suffix, key in (("", "#edge_"), ("_faint", "#faint_")):
				write_json(base / f"cube_{face}_{toward}{suffix}.json", {
					"textures": {"particle": "#inner"},
					"elements": [dict(CUBE, faces={face: {"texture": key + edge, "cullface": face}})],
				})

	# Vanilla's pane parts, rebuilt without their caps so stacked panes can leave them off.
	arm = {
		"side": {"from": [7, 0, 0], "to": [9, 16, 7], "end": "north",
			"faces": {"west": [16, 0, 9, 16], "east": [9, 0, 16, 16]}, "cap": [7, 0, 9, 7], "end_edge": "right"},
		"side_alt": {"from": [7, 0, 9], "to": [9, 16, 16], "end": "south",
			"faces": {"west": [7, 0, 0, 16], "east": [0, 0, 7, 16]}, "cap": [7, 0, 9, 7], "end_edge": "left"},
	}
	for name, part in arm.items():
		box = {"from": part["from"], "to": part["to"]}
		for suffix, key in (("", "#inner"), ("_bare", "#bare")):
			write_json(base / f"pane_{name}{suffix}.json", {
				"ambientocclusion": False,
				"textures": {"particle": "#inner"},
				"elements": [dict(box, faces={
					**{face: {"uv": uv, "texture": key} for face, uv in part["faces"].items()},
					part["end"]: {"uv": [7, 0, 9, 16], "texture": "#edge", "cullface": part["end"]},
				})],
			})
		for strip, edge in (("top", "top"), ("bottom", "bottom"), ("end", part["end_edge"])):
			write_json(base / f"pane_{name}_{strip}.json", {
				"ambientocclusion": False,
				"textures": {"particle": "#inner"},
				"elements": [dict(box, faces={face: {"uv": uv, "texture": "#edge_" + edge} for face, uv in part["faces"].items()})],
			})
		for cap in ("up", "down"):
			write_json(base / f"pane_{name}_cap_{cap}.json", {
				"ambientocclusion": False,
				"textures": {"particle": "#inner"},
				"elements": [dict(box, faces={cap: {"uv": part["cap"], "texture": "#edge"}})],
			})

	post = {"from": [7, 0, 7], "to": [9, 16, 9]}
	for name, (face, uv) in {"noside": ("north", [9, 0, 7, 16]), "noside_alt": ("east", [7, 0, 9, 16])}.items():
		for suffix, key in (("", "#inner"), ("_bare", "#bare")):
			write_json(base / f"pane_{name}{suffix}.json", {
				"ambientocclusion": False,
				"textures": {"particle": "#inner"},
				"elements": [dict(post, faces={face: {"uv": uv, "texture": key}})],
			})
		for strip in ("top", "bottom"):
			write_json(base / f"pane_{name}_{strip}.json", {
				"ambientocclusion": False,
				"textures": {"particle": "#inner"},
				"elements": [dict(post, faces={face: {"uv": uv, "texture": "#edge_" + strip}})],
			})
	for cap in ("up", "down"):
		write_json(base / f"pane_cap_{cap}.json", {
			"ambientocclusion": False,
			"textures": {"particle": "#inner"},
			"elements": [dict(post, faces={cap: {"uv": [7, 7, 9, 9], "texture": "#edge"}})],
		})


PANE_PARTS = ["cap_up", "cap_down"] + [
	f"{arm}{suffix}" for arm in ("side", "side_alt") for suffix in ("", "_bare", "_top", "_bottom", "_end", "_cap_up", "_cap_down")
] + [f"{post}{suffix}" for post in ("noside", "noside_alt") for suffix in ("", "_bare", "_top", "_bottom")]


def glass_textures_ref(name):
	ref = {"particle": tex(f"connected/{name}_inner"), "inner": tex(f"connected/{name}_inner"), "bare": tex(f"connected/{name}_bare")}
	for edge in ("top", "bottom", "left", "right"):
		ref["edge_" + edge] = tex(f"connected/{name}_edge_{edge}")
		ref["faint_" + edge] = tex(f"connected/{name}_edge_{edge}_faint")
	return ref


def connected_block(namespace, block, look):
	models = ASSETS / namespace / "models/block"
	textures = glass_textures_ref(look)
	for face in DIRECTIONS:
		write_json(models / f"{block}_connected_{face}.json", {"parent": f"{MOD_ID}:block/connected/cube_{face}", "textures": textures})
		bare = "none" if look in EMPTY else f"cube_{face}_bare"
		write_json(models / f"{block}_connected_{face}_bare.json", {"parent": f"{MOD_ID}:block/connected/{bare}", "textures": textures})
	for face, edges in FACE_EDGE.items():
		for toward in edges:
			for suffix in ("", "_faint"):
				write_json(models / f"{block}_connected_{face}_{toward}{suffix}.json",
					{"parent": f"{MOD_ID}:block/connected/cube_{face}_{toward}{suffix}", "textures": textures})


def connected_pane(namespace, pane, look, edge):
	models = ASSETS / namespace / "models/block"
	textures = dict(glass_textures_ref(look), edge=edge)
	for part in PANE_PARTS:
		write_json(models / f"{pane}_connected_{part}.json",
			{"parent": f"{MOD_ID}:block/connected/pane_{part}", "textures": textures})


# ---- our own blocks ---------------------------------------------------------------------------

def cracked_blocks():
	ns = ASSETS / MOD_ID
	write_json(ns / "models/block/cracked_glass.json",
		{"parent": "minecraft:block/cube_all", "textures": {"all": tex("cracked_glass")}})
	write_json(ns / "blockstates/cracked_glass.json", {"variants": {"": {"model": f"{MOD_ID}:block/cracked_glass"}}})

	pane_textures = {"pane": tex("cracked_glass"), "edge": "minecraft:block/glass_pane_top"}
	for part in ("post", "side", "side_alt", "noside", "noside_alt"):
		write_json(ns / f"models/block/cracked_glass_pane_{part}.json",
			{"parent": f"minecraft:block/template_glass_pane_{part}", "textures": pane_textures})

	def pane(part, y=0, when=None):
		entry = {"apply": {"model": f"{MOD_ID}:block/cracked_glass_pane_{part}"}}
		if y:
			entry["apply"]["y"] = y
		if when:
			entry["when"] = when
		return entry

	write_json(ns / "blockstates/cracked_glass_pane.json", {"multipart": [
		pane("post"),
		pane("side", when={"north": "true"}),
		pane("side", 90, {"east": "true"}),
		pane("side_alt", when={"south": "true"}),
		pane("side_alt", 90, {"west": "true"}),
		pane("noside", when={"north": "false"}),
		pane("noside_alt", when={"east": "false"}),
		pane("noside_alt", 90, {"south": "false"}),
		pane("noside", 270, {"west": "false"}),
	]})

	write_json(ns / "items/cracked_glass.json", {"model": {"type": "minecraft:model", "model": f"{MOD_ID}:block/cracked_glass"}})
	write_json(ns / "models/item/cracked_glass_pane.json",
		{"parent": "minecraft:item/generated", "textures": {"layer0": tex("cracked_glass")}})
	write_json(ns / "items/cracked_glass_pane.json", {"model": {"type": "minecraft:model", "model": f"{MOD_ID}:item/cracked_glass_pane"}})
	write_json(ns / "models/item/glass_shard.json",
		{"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD_ID}:item/glass_shard"}})
	write_json(ns / "items/glass_shard.json", {"model": {"type": "minecraft:model", "model": f"{MOD_ID}:item/glass_shard"}})


# ---- data -------------------------------------------------------------------------------------

def shards(count):
	"""Every shard back, but now and then one is lost: the same shape GlassShardLoot gives vanilla's glass."""
	return {
		"type": "minecraft:item",
		"name": f"{MOD_ID}:glass_shard",
		"modifier": [
			{"type": "minecraft:set_count", "count": count},
			{"type": "minecraft:set_count", "count": -1, "add": True,
				"condition": {"type": "minecraft:random_chance", "chance": 0.1}},
		],
	}


def loot(block, count):
	write_json(DATA / MOD_ID / f"loot_table/blocks/{block}.json", {
		"type": "minecraft:block",
		"pools": [{
			"rolls": 1,
			"condition": {"type": "minecraft:survives_explosion"},
			"entries": [{"type": "minecraft:alternatives", "children": [
				{"type": "minecraft:item", "condition": "minecraft:tool/can_silk_touch", "name": f"{MOD_ID}:{block}"},
				shards(count),
			]}],
		}],
		"random_sequence": f"{MOD_ID}:blocks/{block}",
	})


def recipe(name, body, unlocked_by):
	write_json(DATA / MOD_ID / f"recipe/{name}.json", body)
	write_json(DATA / MOD_ID / f"advancement/recipes/building_blocks/{name}.json", {
		"parent": "minecraft:recipes/root",
		"criteria": {
			"has_the_recipe": {"trigger": "minecraft:recipe_unlocked", "conditions": {"recipes": f"{MOD_ID}:{name}"}},
			"has_item": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": unlocked_by}]}},
		},
		"requirements": [["has_the_recipe", "has_item"]],
		"rewards": {"recipes": [f"{MOD_ID}:{name}"]},
	})


def recipes():
	shard, block, pane = f"{MOD_ID}:glass_shard", f"{MOD_ID}:cracked_glass", f"{MOD_ID}:cracked_glass_pane"
	recipe("cracked_glass", {
		"type": "minecraft:crafting_shaped", "category": "building",
		"key": {"#": shard}, "pattern": ["###", "###", "###"],
		"result": {"id": block, "count": 1},
	}, shard)
	recipe("cracked_glass_pane", {
		"type": "minecraft:crafting_shaped", "category": "building",
		"key": {"#": shard}, "pattern": ["##", "##"],
		"result": {"id": pane, "count": 1},
	}, shard)
	recipe("cracked_glass_pane_from_cracked_glass", {
		"type": "minecraft:crafting_shaped", "category": "building",
		"key": {"#": block}, "pattern": ["###", "###"],
		"result": {"id": pane, "count": 16},
	}, block)
	recipe("glass_from_cracked_glass", {
		"type": "minecraft:smelting", "category": "blocks", "cookingtime": 200, "experience": 0.1,
		"ingredient": block, "result": {"id": "minecraft:glass"},
	}, block)
	recipe("glass_pane_from_cracked_glass_pane", {
		"type": "minecraft:smelting", "category": "blocks", "cookingtime": 100, "experience": 0.05,
		"ingredient": pane, "result": {"id": "minecraft:glass_pane"},
	}, pane)


# ---- all of it --------------------------------------------------------------------------------

def main():
	for generated in (ASSETS, DATA):
		if generated.exists():
			shutil.rmtree(generated)

	looks = {"glass": clear_glass(), "tinted_glass": tinted_glass(), "cracked_glass": cracked_glass()}
	for colour, rgb in STAINED.items():
		looks[f"{colour}_stained_glass"] = stained_glass(rgb)
	for name, (inner, edges, bare) in looks.items():
		glass_textures(name, inner, edges, bare)
	save(whole(*looks["cracked_glass"][:2]), ASSETS / MOD_ID / "textures/block/cracked_glass.png")
	save(shard_item(), ASSETS / MOD_ID / "textures/item/glass_shard.png")
	save(icon(looks), ASSETS / MOD_ID / "icon.png")

	connected_templates()
	connected_block("minecraft", "glass", "glass")
	connected_block("minecraft", "tinted_glass", "tinted_glass")
	connected_pane("minecraft", "glass_pane", "glass", "minecraft:block/glass_pane_top")
	for colour in STAINED:
		connected_block("minecraft", f"{colour}_stained_glass", f"{colour}_stained_glass")
		connected_pane("minecraft", f"{colour}_stained_glass_pane", f"{colour}_stained_glass",
			f"minecraft:block/{colour}_stained_glass_pane_top")
	connected_block(MOD_ID, "cracked_glass", "cracked_glass")
	connected_pane(MOD_ID, "cracked_glass_pane", "cracked_glass", "minecraft:block/glass_pane_top")

	cracked_blocks()
	write_json(ASSETS / MOD_ID / "lang/en_us.json", {
		f"block.{MOD_ID}.cracked_glass": "Cracked Glass",
		f"block.{MOD_ID}.cracked_glass_pane": "Cracked Glass Pane",
		f"item.{MOD_ID}.glass_shard": "Glass Shard",
	})
	loot("cracked_glass", 9)
	loot("cracked_glass_pane", 4)
	recipes()


if __name__ == "__main__":
	main()
