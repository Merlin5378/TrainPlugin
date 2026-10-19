package de.merlin.swordtrainer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MerlinSwordTrainer extends JavaPlugin implements Listener {
    private final Map<UUID, String> active = new HashMap<>();
    private final Map<UUID, Integer> hits = new HashMap<>(), bestHits = new HashMap<>(), jumpResets = new HashMap<>(), spacingChecks = new HashMap<>();
    private final Map<UUID, BukkitTask> tasks = new HashMap<>();

    @Override public void onEnable() { getServer().getPluginManager().registerEvents(this, this); }
    @Override public void onDisable() { tasks.values().forEach(BukkitTask::cancel); tasks.clear(); }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Nur im Spiel verfügbar."); return true; }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            msg(p,"&b&lMerlin Sword Trainer"); msg(p,"&7/st start jump &8- Jump-Reset-Training"); msg(p,"&7/st start spacing &8- Spacing-Training"); msg(p,"&7/st start combo &8- Combo-Start-Training"); msg(p,"&7/st stats &8- Statistiken"); msg(p,"&7/st stop &8- Training beenden"); msg(p,"&7/st reset &8- Statistiken zurücksetzen"); return true;
        }
        switch (args[0].toLowerCase()) {
            case "start" -> {
                if (args.length < 2) { msg(p,"&cNutze: /st start <jump|spacing|combo>"); return true; }
                String mode=args[1].toLowerCase();
                if (!mode.equals("jump") && !mode.equals("spacing") && !mode.equals("combo")) { msg(p,"&cModus: jump, spacing oder combo."); return true; }
                stop(p); active.put(p.getUniqueId(),mode); hits.put(p.getUniqueId(),0); msg(p,"&aTraining gestartet: &b"+mode);
                if (mode.equals("spacing")) {
                    BukkitTask task=Bukkit.getScheduler().runTaskTimer(this,()->{
                        if (!p.isOnline() || !active.containsKey(p.getUniqueId())) return;
                        Entity target=nearestTarget(p,5.0);
                        if (target==null) p.sendActionBar(ChatColor.YELLOW+"Spacing: Kein Ziel in 5 Blöcken");
                        else { double d=p.getLocation().distance(target.getLocation()); String hint=d<2.2?"Zu nah":d>3.6?"Zu weit":"Gute Distanz!"; p.sendActionBar(ChatColor.AQUA+"Spacing "+String.format("%.1f",d)+" Blöcke: "+hint); spacingChecks.merge(p.getUniqueId(),1,Integer::sum); }
                    },0L,5L); tasks.put(p.getUniqueId(),task);
                } else if (mode.equals("combo")) p.sendMessage(ChatColor.GRAY+"Tipp: Laufe an, triff erneut und halte dein Crosshair auf Brusthöhe.");
                else p.sendMessage(ChatColor.GRAY+"Tipp: Übe Sprung-Timing und kontrollierte Landungen.");
                return true;
            }
            case "stop" -> { stop(p); msg(p,"&eTraining beendet."); return true; }
            case "stats" -> { msg(p,"&b&lTrainingsstatistik"); msg(p,"&7Treffer Runde: &f"+hits.getOrDefault(p.getUniqueId(),0)); msg(p,"&7Beste Treffer-Serie: &f"+bestHits.getOrDefault(p.getUniqueId(),0)); msg(p,"&7Jump-Reset-Treffer: &f"+jumpResets.getOrDefault(p.getUniqueId(),0)); msg(p,"&7Spacing-Messungen: &f"+spacingChecks.getOrDefault(p.getUniqueId(),0)); return true; }
            case "reset" -> { UUID id=p.getUniqueId(); hits.remove(id); bestHits.remove(id); jumpResets.remove(id); spacingChecks.remove(id); msg(p,"&aStatistiken zurückgesetzt."); return true; }
            default -> { msg(p,"&cUnbekannter Befehl. Nutze /st help."); return true; }
        }
    }
    @EventHandler public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player p)) return; String mode=active.get(p.getUniqueId()); if (mode==null) return;
        if (p.getInventory().getItemInMainHand().getType()==Material.AIR) return;
        int count=hits.merge(p.getUniqueId(),1,Integer::sum); bestHits.merge(p.getUniqueId(),count,Math::max);
        if (mode.equals("jump") && !p.isOnGround()) { int n=jumpResets.merge(p.getUniqueId(),1,Integer::sum); p.sendActionBar(ChatColor.GREEN+"Jump-Reset-Treffer! #"+n); p.playSound(p.getLocation(),Sound.ENTITY_EXPERIENCE_ORB_PICKUP,0.5f,1.5f); }
        else if (mode.equals("combo")) p.sendActionBar(ChatColor.AQUA+"Combo-Training: Treffer #"+count);
    }
    private Entity nearestTarget(Player p,double range) { Entity best=null; double bestDist=range; for(Entity e:p.getNearbyEntities(range,range,range)) { if (!(e instanceof LivingEntity) || e==p || e.isDead()) continue; double d=p.getLocation().distance(e.getLocation()); if(d<bestDist){best=e;bestDist=d;} } return best; }
    private void stop(Player p) { active.remove(p.getUniqueId()); BukkitTask task=tasks.remove(p.getUniqueId()); if(task!=null) task.cancel(); }
    private void msg(Player p,String s) { p.sendMessage(ChatColor.translateAlternateColorCodes('&',s)); }
}
