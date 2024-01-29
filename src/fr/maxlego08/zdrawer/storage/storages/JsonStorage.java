package fr.maxlego08.zdrawer.storage.storages;

import fr.maxlego08.zdrawer.DrawerPlugin;
import fr.maxlego08.zdrawer.ZDrawer;
import fr.maxlego08.zdrawer.api.Drawer;
import fr.maxlego08.zdrawer.api.storage.IStorage;
import fr.maxlego08.zdrawer.api.storage.DrawerContainer;
import fr.maxlego08.zdrawer.zcore.utils.nms.ItemStackUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JsonStorage implements IStorage {

    private final List<DrawerContainer> waitingDrawers = new ArrayList<>();
    private transient DrawerPlugin plugin;
    private transient Map<String, Drawer> drawerMap = new HashMap<>();
    private List<DrawerContainer> drawers = new ArrayList<>();

    public JsonStorage(DrawerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Optional<Drawer> getDrawer(Location location) {
        return Optional.ofNullable(drawerMap.getOrDefault(locationToString(location), null));
    }

    @Override
    public void storeDrawer(Drawer drawer) {

        String stringLocation = locationToString(drawer.getLocation());
        this.drawerMap.put(stringLocation, drawer);

        DrawerContainer drawerContainer = new DrawerContainer(stringLocation, drawer.getBlockFace(), drawer.getItemStackAsString(), drawer.getUpgradeName(), drawer.getAmount());
        this.drawers.add(drawerContainer);
    }

    @Override
    public void removeDrawer(Location location) {

        String stringLocation = locationToString(location);
        this.drawerMap.remove(stringLocation);

        this.drawers.removeIf(dc -> dc.getLocation().equals(stringLocation));
    }

    @Override
    public void createDrawer(DrawerContainer drawerContainer) {
        Location location = stringToLocation(drawerContainer.getLocation());
        Drawer drawer = new ZDrawer(this.plugin, location, drawerContainer.getBlockFace());
        if (drawerContainer.hasItemStack()) {
            ItemStack itemStack = ItemStackUtils.deserializeItemStack(drawerContainer.getItemStack());
            drawer.setItemStack(itemStack);
            drawer.setAmount(drawerContainer.getAmount());
        }
        if (drawerContainer.hasUpgrade()) {
            this.plugin.getManager().getUpgrade(drawerContainer.getUpgrade()).ifPresent(drawer::setUpgrade);
        }
        drawerMap.put(drawerContainer.getLocation(), drawer);
    }

    @Override
    public void load() {
        this.drawerMap = new HashMap<>();
        this.drawers.forEach(drawerContainer -> {

            if (!drawerContainer.isWorldLoaded()) {
                this.waitingDrawers.add(drawerContainer);
                return;
            }

            createDrawer(drawerContainer);
        });
    }

    @Override
    public void save() {
        this.drawers = new ArrayList<>();
        this.drawerMap.forEach((stringLocation, drawer) -> {
            drawer.onDisable();
            DrawerContainer drawerContainer = new DrawerContainer(stringLocation, drawer.getBlockFace(), drawer.getItemStackAsString(), drawer.getUpgradeName(), drawer.getAmount());
            this.drawers.add(drawerContainer);
        });
    }

    @Override
    public void purge(World world) {
        this.drawers.removeIf(drawerContainer -> stringToLocation(drawerContainer.getLocation()).getWorld().getName().equals(world.getName()));
        this.drawerMap.entrySet().removeIf(entry -> {
            boolean needToDelete = stringToLocation(entry.getKey()).getWorld().getName().equals(world.getName());
            if (needToDelete) entry.getValue().onDisable();
            return needToDelete;
        });
    }

    @Override
    public List<DrawerContainer> getWaitingWorldDrawers() {
        return this.waitingDrawers;
    }

    private String locationToString(Location location) {
        return location.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private Location stringToLocation(String string) {
        String[] locationArray = string.split(",");
        return new Location(Bukkit.getServer().getWorld(locationArray[0]), Integer.parseInt(locationArray[1]), Integer.parseInt(locationArray[2]), Integer.parseInt(locationArray[3]));
    }

    public void setPlugin(DrawerPlugin plugin) {
        this.plugin = plugin;
    }
}
