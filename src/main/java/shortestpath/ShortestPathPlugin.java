package shortestpath;

import com.google.inject.Provides;
import net.runelite.api.*;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapOverlay;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ImageUtil;
import shortestpath.pathfinder.CollisionMap;
import shortestpath.pathfinder.Pathfinder;
import shortestpath.pathfinder.SplitFlagMap;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@PluginDescriptor(name = "Shortest Path", description = "Draws the shortest path to a chosen destination on the map (right click a spot on the world map to use)")
public class ShortestPathPlugin extends Plugin {
    private static final WorldArea WILDERNESS_ABOVE_GROUND = new WorldArea(2944, 3523, 448, 448, 0);
    private static final WorldArea WILDERNESS_UNDERGROUND = new WorldArea(2944, 9918, 320, 442, 0);
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
    public CollisionMap map;
    public List<WorldPoint> path = null;
    private WorldPoint target = null;
    private boolean running;
    private Point lastMenuOpenedPoint;
    public WorldMapPoint marker;
    private static final BufferedImage MARKER_IMAGE = ImageUtil.getResourceStreamFromClass(ShortestPathPlugin.class, "/marker.png");
    public boolean pathUpdateScheduled = false;
    public final Map<WorldPoint, List<WorldPoint>> transports = new HashMap<>();
    public Pathfinder pathfinder;
    private WorldPoint transportStart;
    private MenuOptionClicked lastClick;

    @Override
    protected void startUp() {
        Map<SplitFlagMap.Position, byte[]> compressedRegions = new HashMap<>();

        try (ZipInputStream in = new ZipInputStream(ShortestPathPlugin.class.getResourceAsStream("/collision-map"))) {
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

        map = new CollisionMap(64, compressedRegions);

        try {
            String s = new String(Util.readAllBytes(ShortestPathPlugin.class.getResourceAsStream("/transports.txt")), StandardCharsets.UTF_8);
            Scanner scanner = new Scanner(s);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                if (line.startsWith("#")) {
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

        running = true;

        new Thread(() -> {
            while (running) {
                if (pathUpdateScheduled) {
                    if (target == null) {
                        path = null;
                    } else {
                        pathfinder = new Pathfinder(map, transports, client.getLocalPlayer().getWorldLocation(), target, config.avoidWilderness() && !isInWilderness(target));
                        path = pathfinder.find();
                        pathUpdateScheduled = false;
                    }
                }

                Util.sleep(10);
            }
        }).start();

        overlayManager.add(pathOverlay);
        overlayManager.add(pathMinimapOverlay);
        overlayManager.add(pathMapOverlay);
    }

    @Override
    protected void shutDown() {
        map = null;
        running = false;
        overlayManager.remove(pathOverlay);
        overlayManager.remove(pathMinimapOverlay);
        overlayManager.add(pathMapOverlay);
    }

    public static boolean isInWilderness(WorldPoint p) {
        return WILDERNESS_ABOVE_GROUND.distanceTo(p) == 0 ||
                WILDERNESS_UNDERGROUND.distanceTo(p) == 0;
    }

    @Subscribe
    public void onMenuOpened(MenuOpened event) {
        lastMenuOpenedPoint = client.getMouseCanvasPosition();
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        if (path != null) {
            if (!isNearPath()) {
                if (config.cancelInstead()) {
                    target = null;
                }

                pathUpdateScheduled = true;
            }

            if (client.getLocalPlayer().getWorldLocation().distanceTo(target) < config.reachedDistance()) {
                target = null;
                pathUpdateScheduled = true;
            }
        }
    }

    private boolean isNearPath() {
        for (WorldPoint point : path) {
            if (client.getLocalPlayer().getWorldLocation().distanceTo(point) < config.recalculateDistance()) {
                return true;
            }
        }
        return false;
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        if (config.drawDebugInfo()) {
            addMenuEntry(event, "Start");
            addMenuEntry(event, "End");
        }

        final Widget map = client.getWidget(WidgetInfo.WORLD_MAP_VIEW);

        if (map == null) {
            return;
        }

        if (map.getBounds().contains(client.getMouseCanvasPosition().getX(), client.getMouseCanvasPosition().getY())) {
            addMenuEntry(event, "Set Target");
            addMenuEntry(event, "Clear Target");
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (event.getMenuOption().equals("Start")) {
            transportStart = client.getLocalPlayer().getWorldLocation();
        }

        if (event.getMenuOption().equals("End")) {
            WorldPoint transportEnd = client.getLocalPlayer().getWorldLocation();
            System.out.println(
                    "transports.add(objectTransport(" +
                            "new Position(" + transportStart.getX() + ", " + transportStart.getY() + ", " + transportStart.getPlane() + ")," +
                            " new Position(" + transportEnd.getX() + ", " + transportEnd.getY() + ", " + transportEnd.getPlane() + "), "
                            + lastClick.getId() + ", \"" + lastClick.getMenuOption() + "\")); // " + lastClick.getMenuTarget());
//            System.out.println(+" " + lastClick.getMenuTarget() + " (" + +"): " + +" -> " + transportEnd);
            transports.computeIfAbsent(transportStart, k -> new ArrayList<>()).add(transportEnd);
        }

        if (event.getMenuOption().equals("Set Target")) {
            setTarget(calculateMapPoint(client.isMenuOpen() ? lastMenuOpenedPoint : client.getMouseCanvasPosition()));
        }

        if (event.getMenuOption().equals("Clear Target")) {
            setTarget(null);
        }

        if (event.getMenuAction() != MenuAction.WALK) {
            lastClick = event;
        }
    }

    private void setTarget(WorldPoint target) {
        this.target = target;
        pathUpdateScheduled = true;

        if (target == null) {
            worldMapPointManager.remove(marker);
            marker = null;
        } else {
            worldMapPointManager.removeIf(x -> x == marker);
            marker = new WorldMapPoint(target, MARKER_IMAGE);
            marker.setTarget(marker.getWorldPoint());
            marker.setJumpOnClick(true);
            worldMapPointManager.add(marker);
        }
    }

    private WorldPoint calculateMapPoint(Point point) {
        float zoom = client.getRenderOverview().getWorldMapZoom();
        RenderOverview renderOverview = client.getRenderOverview();
        final WorldPoint mapPoint = new WorldPoint(renderOverview.getWorldMapPosition().getX(), renderOverview.getWorldMapPosition().getY(), 0);
        final Point middle = worldMapOverlay.mapWorldPointToGraphicsPoint(mapPoint);

        final int dx = (int) ((point.getX() - middle.getX()) / zoom);
        final int dy = (int) ((-(point.getY() - middle.getY())) / zoom);

        return mapPoint.dx(dx).dy(dy);
    }

    @Provides
    public ShortestPathConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ShortestPathConfig.class);
    }

    private void addMenuEntry(MenuEntryAdded event, String option) {
        List<MenuEntry> entries = new LinkedList<>(Arrays.asList(client.getMenuEntries()));

        if (entries.stream().anyMatch(e -> e.getOption().equals(option))) {
            return;
        }

        MenuEntry entry = new MenuEntry();
        entry.setOption(option);
        entry.setTarget(event.getTarget());
        entry.setType(MenuAction.RUNELITE.getId());
        entries.add(0, entry);

        client.setMenuEntries(entries.toArray(new MenuEntry[0]));
    }
}
