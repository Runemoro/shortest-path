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
        if (config.drawTransports()) {
            for (WorldPoint a : plugin.transports.keySet()) {
                drawTile(graphics, a, new Color(0, 255, 0, 128));

                Point ca = tileCenter(a);

                if (ca == null) {
                    continue;
                }

                for (WorldPoint b : plugin.transports.get(a)) {
                    Point cb = tileCenter(b);

                    if (cb != null) {
                        graphics.drawLine(ca.x, ca.y, cb.x, cb.y);
                    }
                }

                StringBuilder s = new StringBuilder();
                for (WorldPoint b : plugin.transports.get(a)) {
                    if (b.getPlane() > a.getPlane()) {
                        s.append("+");
                    } else if (b.getPlane() < a.getPlane()) {
                        s.append("-");
                    } else {
                        s.append("=");
                    }
                }
                graphics.setColor(Color.WHITE);
                graphics.drawString(s.toString(), ca.x, ca.y);
            }
        }

        if (config.drawCollisionMap()) {
            for (Tile[] row : client.getScene().getTiles()[client.getPlane()]) {
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
            int i = 0;
            for (WorldPoint point : plugin.path) {
                drawTile(graphics, point, ++i, new Color(255, 0, 0, 128));
            }
        }


        return null;
    }

    private Point tileCenter(WorldPoint b) {
        if (b.getPlane() != client.getPlane()) {
            return null;
        }

        LocalPoint lp = LocalPoint.fromWorld(client, b);
        if (lp == null) {
            return null;
        }

        Polygon poly = Perspective.getCanvasTilePoly(client, lp);
        if (poly == null) {
            return null;
        }

        int cx = poly.getBounds().x + poly.getBounds().width / 2;
        int cy = poly.getBounds().y + poly.getBounds().height / 2;
        return new Point(cx, cy);
    }

    private void drawTile(Graphics2D graphics, WorldPoint point, int i, Color color) {
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

        if (config.drawTileNumbers()) {
            String s = "" + i;
            graphics.setColor(Color.WHITE);
            int stringX = (int) (poly.getBounds().getCenterX() - graphics.getFontMetrics().getStringBounds(s, graphics).getWidth() / 2);
            int stringY = (int) poly.getBounds().getCenterY();
            graphics.drawString(s, stringX, stringY);
        }
    }
}
