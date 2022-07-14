package shortestpath;

import com.google.inject.Inject;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.stream.Stream;

import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.worldmap.WorldMapOverlay;
import shortestpath.pathfinder.OrdinalDirection;

public class PathMapOverlay extends Overlay {
    private final Client client;
    private final ShortestPathPlugin plugin;
    private final ShortestPathConfig config;

    @Inject
    private WorldMapOverlay worldMapOverlay;

    private Area mapClipArea;

    @Inject
    private PathMapOverlay(Client client, ShortestPathPlugin plugin, ShortestPathConfig config) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.LOW);
        setLayer(OverlayLayer.MANUAL);
        drawAfterLayer(WidgetInfo.WORLD_MAP_VIEW);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.drawMap()) {
            return null;
        }

        if (client.getWidget(WidgetInfo.WORLD_MAP_VIEW) == null) {
            return null;
        }

        mapClipArea = getWorldMapClipArea(client.getWidget(WidgetInfo.WORLD_MAP_VIEW).getBounds());
        graphics.setClip(mapClipArea);

        if (config.drawCollisionMap()) {
            graphics.setColor(config.colourCollisionMap());
            Rectangle extent = getWorldMapExtent(client.getWidget(WidgetInfo.WORLD_MAP_VIEW).getBounds());
            final int z = client.getPlane();
            for (int x = extent.x; x < (extent.x + extent.width + 1); x++) {
                for (int y = extent.y - extent.height; y < (extent.y + 1); y++) {
                    final int finalX = x;
                    final int finalY = y;
                    final boolean isBlocked = Stream.of(OrdinalDirection.values()).noneMatch(dir -> plugin.getMap().checkDirection(finalX, finalY, z, dir));
                    if (isBlocked) {
                        drawOnMap(graphics, new WorldPoint(x, y, z));
                    }
                }
            }
        }

        if (config.drawTransports()) {
            graphics.setColor(Color.WHITE);
            for (WorldPoint a : plugin.getTransports().keySet()) {
                Point mapA = worldMapOverlay.mapWorldPointToGraphicsPoint(a);
                if (mapA == null) {
                    continue;
                }

                for (Transport b : plugin.getTransports().get(a)) {
                    Point mapB = worldMapOverlay.mapWorldPointToGraphicsPoint(b.getOrigin());
                    if (mapB == null) {
                        continue;
                    }

                    graphics.drawLine(mapA.getX(), mapA.getY(), mapB.getX(), mapB.getY());
                }
            }
        }

        if (plugin.currentPath != null) {
            boolean done = plugin.currentPath.isDone();
            graphics.setColor(done ? config.colourPath() : config.colourPathCalculating());
            Path path = plugin.currentPath.getPath();
            if (path != null) {
                for (WorldPoint point : path.points) {
                    drawOnMap(graphics, point);
                }
            }
        }

        return null;
    }

    private void drawOnMap(Graphics2D graphics, WorldPoint point) {
        Point start = plugin.mapWorldPointToGraphicsPoint(point);
        Point end = plugin.mapWorldPointToGraphicsPoint(point.dx(1).dy(-1));

        if (start == null || end == null) {
            return;
        }

        int x = start.getX();
        int y = start.getY();
        final int width = end.getX() - x;
        final int height = end.getY() - y;
        x -= width / 2;
        y -= height / 2;

        graphics.fillRect(x, y, width, height);
    }

    private Area getWorldMapClipArea(Rectangle baseRectangle) {
        final Widget overview = client.getWidget(WidgetInfo.WORLD_MAP_OVERVIEW_MAP);
        final Widget surfaceSelector = client.getWidget(WidgetInfo.WORLD_MAP_SURFACE_SELECTOR);

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
