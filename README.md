# Better Glass

A Fabric mod that joins glass into one clear sheet, and stops broken glass from vanishing: it comes apart into shards that go back together as cracked glass.

## Glass That Joins Up

Vanilla draws a frame around every block of glass, so a window four blocks wide is a grid of sixteen little frames, and looking out through it means looking past all of them.

Here the frame only goes round the outside. Where glass meets the same glass, the edge between them is left off, so a wall of it reads as one pane, and what is behind it is what you see. The same goes for glass panes: a run of them is one window, side to side and floor to ceiling, with the frame at its ends and along its top and bottom. Panes stacked on each other also lose the caps between them, which left a line across every vanilla window two panes tall.

Curves keep it too. Where the surface of a sphere, a dome or a stair of glass folds over into more of itself, there is no line at all. The odd corners left, where a curve steps and the glass beyond only touches at an edge or a corner, keep a faint line, enough for the shape to read without getting in the way: a dome reads as one shell rather than a pile of framed steps. A box of glass still keeps its outline at full strength, because there the glass does end.

Every kind is drawn this way: clear, tinted, all sixteen colours of stained glass and their panes, and the cracked glass below. Only the same block joins, so a red window set into a clear wall keeps its own frame, and so does a cracked block in a clear wall.

The glass keeps vanilla's own small streaks, but not on every block: a block on its own has them, and a joined sheet keeps them on one face in five, spread evenly, enough to tell there is glass there without a pattern repeated across the whole view. Cracked glass works the same way with its cracks: a thin crack on one face in five, and only a hairline on the rest.

## Shards

Glass broken without silk touch drops its shards instead of nothing: nine from a block and four from a pane, which is exactly what it takes to make one again, though now and then one is lost. Silk touch still takes the block whole. Tinted glass is left as vanilla has it, because it already comes back whole however it is broken.

Shards go back together, but they remember being broken:

| Shards | Make |
|---|---|
| 9, filling the grid | 1 Cracked Glass |
| 4, in a square | 1 Cracked Glass Pane |

Cracked glass is real glass in every way that counts: it lets light through, it joins up with more of itself, it builds windows. Six blocks of it make sixteen cracked panes, the way glass does. Break it and it is shards again.

It is not to be trusted, though. It comes apart at a touch, three times quicker than glass. About three times in ten, cracked glass set in place gives way a moment later and falls back into shards, so building with it takes a few tries. And it does not hold weight for long: stand on it and it cracks further under you, with the sound of it, until after about three seconds it goes and you fall through. Step off in time and it settles again. Nothing much is lost either way: the shards come back as if you had broken it by hand.

To have clear glass back, smelt it: cracked glass comes out of a furnace as glass, and a cracked pane as a glass pane.

Every colour of glass breaks into the same clear shards; the colour does not survive.

## Pandorical

Better Glass runs server-side, and Pandorical is required there: the server will not load this mod without it. The shards and their loot are the server's alone, so everyone gets them. Pandorical sends the joined glass models and the cracked glass blocks to clients.

**The Pandorical mod must be installed client-side** to see either. Without it a client sees vanilla's framed glass, and still gets shards when glass breaks. Cracked glass is a real registered block, so a client without Pandorical cannot render it and cannot receive chunks containing one.

## Development

The map of the source and the art pipeline are in [DEVELOPMENT.md](DEVELOPMENT.md).

## License

MIT, see [LICENSE](LICENSE).
