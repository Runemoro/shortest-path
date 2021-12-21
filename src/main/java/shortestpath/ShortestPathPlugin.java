package shortestpath;

import com.google.inject.Provides;
import net.runelite.api.Point;
import net.runelite.api.*;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapOverlay;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
import shortestpath.pathfinder.CollisionMap;
import shortestpath.pathfinder.Pathfinder;
import shortestpath.pathfinder.SplitFlagMap;

import javax.inject.Inject;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@PluginDescriptor(
    name = "Shortest Path",
    description = "Draws the shortest path to a chosen destination on the map (right click a spot on the world map to use)",
    tags = {"pathfinder", "map", "waypoint", "navigation"}
)
public class ShortestPathPlugin extends Plugin {
    private static final String ADD_START = "Add start";
    private static final String ADD_END = "Add end";
    private static final String CLEAR = "Clear";
    private static final String PATH = ColorUtil.wrapWithColorTag("Path", JagexColors.MENU_TARGET);
    private static final String SET = "Set";
    private static final String START = ColorUtil.wrapWithColorTag("Start", JagexColors.MENU_TARGET);
    private static final String TARGET = ColorUtil.wrapWithColorTag("Target", JagexColors.MENU_TARGET);
    private static final String TRANSPORT = ColorUtil.wrapWithColorTag("Transport", JagexColors.MENU_TARGET);
    private static final String WALK_HERE = "Walk here";
    private static final BufferedImage MARKER_IMAGE = ImageUtil.loadImageResource(ShortestPathPlugin.class, "/marker.png");

    @Inject
    public Client client;

    @Inject
    public ShortestPathConfig config;

    @Inject
    public OverlayManager overlayManager;

    @Inject
    public PathTileOverlay pathOverlay;

    @Inject
    public PathMinimapOverlay pathMinimapOverlay;

    @Inject
    public PathMapOverlay pathMapOverlay;

    @Inject
    private WorldMapPointManager worldMapPointManager;

    @Inject
    private WorldMapOverlay worldMapOverlay;

    public Pathfinder pathfinder;
    public Pathfinder.Path currentPath;

    private Point lastMenuOpenedPoint;
    public WorldMapPoint marker;
    private WorldPoint transportStart;
    private MenuEntry lastClick;

    public boolean updateScheduled = true;

    @Override
    protected void startUp() {
        Map<SplitFlagMap.Position, byte[]> compressedRegions = new HashMap<>();
        HashMap<WorldPoint, List<WorldPoint>> transports = new HashMap<>();

        try (ZipInputStream in = new ZipInputStream(ShortestPathPlugin.class.getResourceAsStream("/collision-map.zip"))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                String[] n = entry.getName().split("_");

                compressedRegions.put(
                        new SplitFlagMap.Position(Integer.parseInt(n[0]), Integer.parseInt(n[1])),
                        Util.readAllBytes(in)
                );
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        try {
            String s = new String(Util.readAllBytes(ShortestPathPlugin.class.getResourceAsStream("/transports.txt")), StandardCharsets.UTF_8);
            Scanner scanner = new Scanner(s);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }

                String[] l = line.split(" ");
                WorldPoint a = new WorldPoint(Integer.parseInt(l[0]), Integer.parseInt(l[1]), Integer.parseInt(l[2]));
                WorldPoint b = new WorldPoint(Integer.parseInt(l[3]), Integer.parseInt(l[4]), Integer.parseInt(l[5]));
                transports.computeIfAbsent(a, k -> new ArrayList<>()).add(b);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        CollisionMap map = new CollisionMap(64, compressedRegions);
        pathfinder = new Pathfinder(map, transports);

        overlayManager.add(pathOverlay);
        overlayManager.add(pathMinimapOverlay);
        overlayManager.add(pathMapOverlay);
    }

    public boolean isNearPath(WorldPoint location) {
        for (WorldPoint point : currentPath.getPath()) {
            if (location.distanceTo(point) < config.recalculateDistance()) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(pathOverlay);
        overlayManager.remove(pathMinimapOverlay);
        overlayManager.add(pathMapOverlay);
    }

    @Subscribe
    public void onMenuOpened(MenuOpened event) {
        lastMenuOpenedPoint = client.getMouseCanvasPosition();
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null || currentPath == null) {
            return;
        }

        // Synchronize currentPath with "updateScheduled"
        if (!currentPath.loading) {
            updateScheduled = false;
        }

        WorldPoint currentLocation = localPlayer.getWorldLocation();
        if (currentLocation.distanceTo(currentPath.target) < config.reachedDistance()) {
            currentPath = null;
        }

        if (!updateScheduled) {
            if (!isNearPath(currentLocation)) {
                if (config.cancelInstead()) {
                    currentPath = null;
                    return;
                }

                updateScheduled = true;
                currentPath = pathfinder.new Path(currentLocation, currentPath.target, config.avoidWilderness());
            }
        }
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        if (client.isKeyPressed(KeyCode.KC_SHIFT) && event.getOption().equals(WALK_HERE) && event.getTarget().isEmpty()) {
            if (config.drawTransports()) {
                addMenuEntry(event, ADD_START, TRANSPORT, 1);
                addMenuEntry(event, ADD_END, TRANSPORT, 1);
                // addMenuEntry(event, "Copy Position");
            }

            addMenuEntry(event, SET, TARGET, 1);
            if (currentPath != null) {
                if (currentPath.target != null) {
                    addMenuEntry(event, SET, START, 1);
                }
                WorldPoint selectedTile = getSelectedWorldPoint();
                if (currentPath.getPath() != null) {
                    for (WorldPoint tile : currentPath.getPath()) {
                        if (tile.equals(selectedTile)) {
                            addMenuEntry(event, CLEAR, PATH, 1);
                            break;
                        }
                    }
                }
            }
        }

        final Widget map = client.getWidget(WidgetInfo.WORLD_MAP_VIEW);

        if (map == null) {
            return;
        }

        if (map.getBounds().contains(client.getMouseCanvasPosition().getX(), client.getMouseCanvasPosition().getY())) {
            addMenuEntry(event, SET, TARGET, 0);
            if (currentPath != null) {
                if (currentPath.target != null) {
                    addMenuEntry(event, SET, START, 0);
                    addMenuEntry(event, CLEAR, PATH, 0);
                }
            }
        }
    }

    private void onMenuOptionClicked(MenuEntry entry) {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null) {
            return;
        }

        WorldPoint currentLocation = localPlayer.getWorldLocation();
        if (entry.getOption().equals(ADD_START) && entry.getTarget().equals(TRANSPORT)) {
            transportStart = currentLocation;
        }

        if (entry.getOption().equals(ADD_END) && entry.getTarget().equals(TRANSPORT)) {
            System.out.println(transportStart.getX() + " " + transportStart.getY() + " " + transportStart.getPlane() + " " +
                    currentLocation.getX() + " " + currentLocation.getY() + " " + currentLocation.getPlane() + " " +
                    lastClick.getOption() + " " + Text.removeTags(lastClick.getTarget()) + " " + lastClick.getIdentifier()
            );
            pathfinder.transports.computeIfAbsent(transportStart, k -> new ArrayList<>()).add(currentLocation);
        }

        if (entry.getOption().equals("Copy Position")) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new StringSelection("(" + currentLocation.getX() + ", "
                            + currentLocation.getY() + ", "
                            + currentLocation.getPlane() + ")"), null);
        }

        if (entry.getOption().equals(SET) && entry.getTarget().equals(TARGET)) {
            setTarget(getSelectedWorldPoint());
        }

        if (entry.getOption().equals(SET) && entry.getTarget().equals(START)) {
            updateScheduled = true;
            currentPath = pathfinder.new Path(getSelectedWorldPoint(), currentPath.target, config.avoidWilderness());
        }

        if (entry.getOption().equals(CLEAR) && entry.getTarget().equals(PATH)) {
            setTarget(null);
        }

        if (entry.getType() != MenuAction.WALK) {
            lastClick = entry;
        }
    }

    private WorldPoint getSelectedWorldPoint()
    {
        if (client.getWidget(WidgetInfo.WORLD_MAP_VIEW) == null) {
            if (client.getSelectedSceneTile() != null) {
                return client.getSelectedSceneTile().getWorldLocation();
            }
        } else {
            return calculateMapPoint(client.isMenuOpen() ? lastMenuOpenedPoint : client.getMouseCanvasPosition());
        }
        return null;
    }

    private void setTarget(WorldPoint target) {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null) {
            return;
        }

        updateScheduled = true;

        if (target == null) {
            worldMapPointManager.remove(marker);
            marker = null;
            currentPath = null;
        } else {
            worldMapPointManager.removeIf(x -> x == marker);
            marker = new WorldMapPoint(target, MARKER_IMAGE);
            marker.setName("Target");
            marker.setTarget(marker.getWorldPoint());
            marker.setJumpOnClick(true);
            worldMapPointManager.add(marker);
            currentPath = pathfinder.new Path(localPlayer.getWorldLocation(), target, config.avoidWilderness());
        }
    }

    private WorldPoint calculateMapPoint(Point point) {
        float zoom = client.getRenderOverview().getWorldMapZoom();
        RenderOverview renderOverview = client.getRenderOverview();
        final WorldPoint mapPoint = new WorldPoint(renderOverview.getWorldMapPosition().getX(), renderOverview.getWorldMapPosition().getY(), 0);
        final Point middle = worldMapOverlay.mapWorldPointToGraphicsPoint(mapPoint);

        if (point == null || middle == null) {
            return null;
        }

        final int dx = (int) ((point.getX() - middle.getX()) / zoom);
        final int dy = (int) ((-(point.getY() - middle.getY())) / zoom);

        return mapPoint.dx(dx).dy(dy);
    }

    @Provides
    public ShortestPathConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ShortestPathConfig.class);
    }

    private void addMenuEntry(MenuEntryAdded event, String option, String target, int position) {
        List<MenuEntry> entries = new LinkedList<>(Arrays.asList(client.getMenuEntries()));

        if (entries.stream().anyMatch(e -> e.getOption().equals(option) && e.getTarget().equals(target))) {
            return;
        }

        client.createMenuEntry(position)
            .setOption(option)
            .setTarget(target)
            .setParam0(event.getActionParam0())
            .setParam1(event.getActionParam1())
            .setIdentifier(event.getIdentifier())
            .setType(MenuAction.RUNELITE)
            .onClick(this::onMenuOptionClicked);
    }
}
