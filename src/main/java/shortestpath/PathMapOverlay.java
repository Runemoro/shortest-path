package shortestpath;

import com.google.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.worldmap.WorldMapOverlay;
import shortestpath.pathfinder.CollisionMap;

public class PathMapOverlay extends Overlay {
    private final Client client;
    private final ShortestPathPlugin plugin;
    private final ShortestPathConfig config;

    @Inject
    private WorldMapOverlay worldMapOverlay;

    @Inject
    private PathMapOverlay(Client client, ShortestPathPlugin plugin, ShortestPathConfig config) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.LOW);
        setLayer(OverlayLayer.MANUAL);
        drawAfterLayer(ComponentID.WORLD_MAP_MAPVIEW);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.drawMap()) {
            return null;
        }

        if (client.getWidget(ComponentID.WORLD_MAP_MAPVIEW) == null) {
            return null;
        }

        Area worldMapClipArea = getWorldMapClipArea(client.getWidget(ComponentID.WORLD_MAP_MAPVIEW).getBounds());
        graphics.setClip(worldMapClipArea);

        if (config.drawCollisionMap()) {
            graphics.setColor(config.colourCollisionMap());
            Rectangle extent = getWorldMapExtent(client.getWidget(ComponentID.WORLD_MAP_MAPVIEW).getBounds());
            final CollisionMap map = plugin.getMap();
            final int z = client.getPlane();
            for (int x = extent.x; x < (extent.x + extent.width + 1); x++) {
                for (int y = extent.y - extent.height; y < (extent.y + 1); y++) {
                    if (map.isBlocked(x, y, z)) {
                        drawOnMap(graphics, new WorldPoint(x, y, z), false);
                    }
                }
            }
        }

        if (config.drawTransports()) {
            graphics.setColor(Color.WHITE);
            for (WorldPoint a : plugin.getTransports().keySet()) {
                Point mapA = worldMapOverlay.mapWorldPointToGraphicsPoint(a);
                if (mapA == null || !worldMapClipArea.contains(mapA.getX(), mapA.getY())) {
                    continue;
                }

                for (Transport b : plugin.getTransports().getOrDefault(a, new ArrayList<>())) {
                    Point mapB = worldMapOverlay.mapWorldPointToGraphicsPoint(b.getDestination());
                    if (mapB == null || !worldMapClipArea.contains(mapB.getX(), mapB.getY())) {
                        continue;
                    }

                    graphics.drawLine(mapA.getX(), mapA.getY(), mapB.getX(), mapB.getY());
                }
            }
        }

        if (plugin.getPathfinder() != null) {
            Color colour = plugin.getPathfinder().isDone() ? config.colourPath() : config.colourPathCalculating();
            List<WorldPoint> path = plugin.getPathfinder().getPath();
            for (int i = 0; i < path.size(); i++) {
                graphics.setColor(colour);
                WorldPoint point = path.get(i);
                WorldPoint last = (i > 0) ? path.get(i - 1) : point;
                if (point.distanceTo(last) > 1) {
                    drawOnMap(graphics, last, point, true);
                }
                drawOnMap(graphics, point, true);
            }
        }

        return null;
    }

    private void drawOnMap(Graphics2D graphics, WorldPoint point, boolean checkHover) {
        drawOnMap(graphics, point, point.dx(1).dy(-1), checkHover);
    }

    private void drawOnMap(Graphics2D graphics, WorldPoint point, WorldPoint offset, boolean checkHover) {
        Point start = plugin.mapWorldPointToGraphicsPoint(point);
        Point end = plugin.mapWorldPointToGraphicsPoint(offset);

        if (start == null || end == null) {
            return;
        }

        int x = start.getX();
        int y = start.getY();
        final int width = end.getX() - x;
        final int height = end.getY() - y;
        x -= width / 2;
        y -= height / 2;

        if (point.distanceTo(offset) > 1) {
            graphics.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0));
            graphics.drawLine(start.getX(), start.getY(), end.getX(), end.getY());
        } else {
            Point cursorPos = client.getMouseCanvasPosition();
            if (checkHover &&
                cursorPos.getX() >= x && cursorPos.getX() <= (end.getX() - width / 2) &&
                cursorPos.getY() >= y && cursorPos.getY() <= (end.getY() - width / 2)) {
                graphics.setColor(graphics.getColor().darker());
            }
            graphics.fillRect(x, y, width, height);
        }
    }

    private Area getWorldMapClipArea(Rectangle baseRectangle) {
        final Widget overview = client.getWidget(ComponentID.WORLD_MAP_OVERVIEW_MAP);
        final Widget surfaceSelector = client.getWidget(ComponentID.WORLD_MAP_SURFACE_SELECTOR);

        Area clipArea = new Area(baseRectangle);

        if (overview != null && !overview.isHidden()) {
            clipArea.subtract(new Area(overview.getBounds()));
        }

        if (surfaceSelector != null && !surfaceSelector.isHidden()) {
            clipArea.subtract(new Area(surfaceSelector.getBounds()));
        }

        return clipArea;
    }

    private Rectangle getWorldMapExtent(Rectangle baseRectangle) {
        WorldPoint topLeft = plugin.calculateMapPoint(new Point(baseRectangle.x, baseRectangle.y));
        WorldPoint bottomRight = plugin.calculateMapPoint(
            new Point(baseRectangle.x + baseRectangle.width, baseRectangle.y + baseRectangle.height));
        return new Rectangle(topLeft.getX(), topLeft.getY(), bottomRight.getX() - topLeft.getX(), topLeft.getY() - bottomRight.getY());
    }
}
