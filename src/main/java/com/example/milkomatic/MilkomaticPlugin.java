package com.maksosn1k.milkomatic;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.*;
import org.bukkit.block.data.Directional;
import org.bukkit.util.Vector;

public class MilkomaticPlugin extends JavaPlugin implements Listener {

    private double throwSpeed;
    private boolean enableSounds;
    private double detectionRadius;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Milkomatic starting!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Milkomatic off");
    }

    private void loadConfig() {
        reloadConfig();
        throwSpeed = getConfig().getDouble("settings.throw-speed", 0.5);
        enableSounds = getConfig().getBoolean("settings.enable-sounds", true);
        detectionRadius = getConfig().getDouble("settings.detection-radius", 1.2);
    }

    @EventHandler
    public void onBlockDispense(BlockDispenseEvent event) {
        if (event.getItem().getType() != Material.BUCKET) return;

        Block dispenserBlock = event.getBlock();
        if (!(dispenserBlock.getState() instanceof Dispenser)) return;

        BlockFace facing = ((Directional) dispenserBlock.getBlockData()).getFacing();
        Block targetBlock = dispenserBlock.getRelative(facing);

        if (!hasMilkableNearby(targetBlock)) return;

        event.setCancelled(true);

        // Снапшот берём ДО отложенной задачи — иначе изменения не прилипнут к блоку
        final Dispenser dispenser = (Dispenser) dispenserBlock.getState();
        final Inventory inv = dispenser.getInventory();

        Bukkit.getScheduler().runTask(this, () -> {
            if (!removeBucket(inv)) return;

            dispenser.update(true);

            if (targetBlock.getType().isSolid()) return;

            Location targetLoc = targetBlock.getLocation().add(0.5, 0.5, 0.5);
            ItemStack milkBucket = new ItemStack(Material.MILK_BUCKET);

            Item droppedItem = targetLoc.getWorld().dropItem(targetLoc, milkBucket);
            Vector velocity = new Vector(
                facing.getModX() * throwSpeed,
                facing.getModY() * throwSpeed + 0.2,
                facing.getModZ() * throwSpeed
            );
            droppedItem.setVelocity(velocity);
            droppedItem.setPickupDelay(20);

            if (enableSounds) {
                targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_COW_MILK, 1.0f, 1.0f);
            }
        });
    }

    private boolean hasMilkableNearby(Block targetBlock) {
        Location center = targetBlock.getLocation().add(0.5, 0.5, 0.5);
        for (Entity e : center.getWorld().getNearbyEntities(
                center, detectionRadius, detectionRadius + 0.3, detectionRadius)) {
            if (e instanceof Cow || e instanceof MushroomCow || e instanceof Goat) {
                return true;
            }
        }
        return false;
    }

    private boolean removeBucket(Inventory inv) {
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() == Material.BUCKET) {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    inv.setItem(i, null);
                }
                return true;
            }
        }
        return false;
    }
}