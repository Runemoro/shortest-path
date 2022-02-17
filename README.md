# Shortest Path

![icon](icon.png)

## Info
Draws the shortest path to a chosen destination on the map (right click a spot on the world map or shift right click a tile to use).

![illustration](https://user-images.githubusercontent.com/53493631/154380329-e1cacdce-a589-4ac3-b6d8-d0dc19f88b2a.png)

## Config options
- Avoid wilderness: ✅ `true`
  - Whether the wilderness should be avoided if possible
    (otherwise, will suggest using wilderness lever to travel from Edgeville to Ardougne)
- Draw path on world map: ✅ `true`
  - Whether the path should be drawn on the world map
- Draw path on minimap: ✅ `true`
  - Whether the path should be drawn on the minimap
- Draw path on tiles: ✅ `true`
  - Whether the path should be drawn on the game tiles
- Draw transports: ⬜️ `false`
  - Whether transports should be drawn
- Draw collision map: ⬜️ `false`
  - Whether the collision map should be drawn
- Cancel instead of recalculating: ⬜️ `false`
  - Whether the path should be cancelled rather than recalculated when the distance limit is exceeded
- Recalculate distance: `10`
  - Distance from the path the player should be for it to be recalculated (-1 for never)
- Finish distance: `5`
  - Distance from the target tile at which the path should be ended (-1 for never)
- Show tile counter: `Disabled`
  - Whether to display the number of tiles travelled, number of tiles remaining or disable counting
- Colours
  - Path: ![#FFFF0000](https://via.placeholder.com/15/FF0000/000000?text=+) `#FFFF0000`
    - Colour of the path tiles on the world map, minimap and in the game scene
  - Calculating: ![#FF0000FF](https://via.placeholder.com/15/0000FF/000000?text=+) `#FF0000FF`
    - Colour of the path tiles while the pathfinding calculation is in progress
  - Transports: ![#8000FF00](https://via.placeholder.com/15/80FF80/000000?text=+) `#8000FF00`
    - Colour of the transport tiles
  - Collision map: ![#800080FF](https://via.placeholder.com/15/80c0FF/000000?text=+) `#800080FF`
    - Colour of the collision map tiles
