package shortestpath;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.*;

public class PathTileOverlay extends Overlay {
    private final Client client;
    private final ShortestPathPlugin plugin;
    private final ShortestPathConfig config;

    @Inject
    public PathTileOverlay(Client client, ShortestPathPlugin plugin, ShortestPathConfig config) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.LOW);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (config.drawDebugInfo()) {
            Tile[][] tiles = client.getScene().getTiles()[client.getPlane()];

            for (Tile[] row : tiles) {
                for (Tile tile : row) {
                    if (tile == null) {
                        continue;
                    }

                    Polygon tilePolygon = Perspective.getCanvasTilePoly(client, tile.getLocalLocation());

                    if (tilePolygon == null) {
                        continue;
                    }

                    int x = tile.getWorldLocation().getX();
                    int y = tile.getWorldLocation().getY();
                    int z = tile.getWorldLocation().getPlane();

                    String s = (!plugin.map.n(x, y, z) ? "n" : "") +
                            (!plugin.map.s(x, y, z) ? "s" : "") +
                            (!plugin.map.e(x, y, z) ? "e" : "") +
                            (!plugin.map.w(x, y, z) ? "w" : "");

                    if (!s.isEmpty() && !s.equals("nsew")) {
                        graphics.setColor(Color.WHITE);
                        int stringX = (int) (tilePolygon.getBounds().getCenterX() - graphics.getFontMetrics().getStringBounds(s, graphics).getWidth() / 2);
                        int stringY = (int) tilePolygon.getBounds().getCenterY();
                        graphics.drawString(s, stringX, stringY);
                    } else if (!s.isEmpty()) {
                        graphics.setColor(new Color(0, 128, 255, 128));
                        graphics.fill(tilePolygon);
                    }
                }
            }
        }

        if (config.drawTiles() && plugin.path != null) {
            for (WorldPoint point : plugin.path) {
                drawTile(graphics, point, new Color(255, 0, 0, 128));
            }
        }


        return null;
    }

    private void drawTile(Graphics2D graphics, WorldPoint point, Color color) {
        if (point.getPlane() != client.getPlane()) {
            return;
        }

        LocalPoint lp = LocalPoint.fromWorld(client, point);
        if (lp == null) {
            return;
        }

        Polygon poly = Perspective.getCanvasTilePoly(client, lp);
        if (poly == null) {
            return;
        }

        graphics.setColor(color);
        graphics.fill(poly);
    }
}
