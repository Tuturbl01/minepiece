package com.minepiecefarmer.config;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class IslandConfig {

    @SerializedName("display_name")
    public String displayName = "Unknown Island";

    /** Zones de farm (polygones) */
    @SerializedName("farm_zones")
    public List<FarmZone> farmZones = new ArrayList<>();

    /** Zones d'exclusion — le bot NE VA PAS dans ces zones */
    @SerializedName("exclusion_zones")
    public List<FarmZone> exclusionZones = new ArrayList<>();

    /** Mobs connus — ordonnés par priority (1 = premier) */
    @SerializedName("known_mobs")
    public List<MobInfo> knownMobs = new ArrayList<>();

    /** Waypoints de navigation */
    @SerializedName("waypoints")
    public List<Waypoint> waypoints = new ArrayList<>();

    // ═══════════ FARM ZONE ═══════════
    public static class FarmZone {
        public String name = "";
        public boolean enabled = true;
        /** Points du polygone [[x1,z1], [x2,z2], ...] */
        public List<double[]> points = new ArrayList<>();
        public double[] center = null;

        public boolean contains(double x, double z) {
            if (points.size() < 3) return false;
            boolean inside = false;
            int j = points.size() - 1;
            for (int i = 0; i < points.size(); i++) {
                double xi = points.get(i)[0], zi = points.get(i)[1];
                double xj = points.get(j)[0], zj = points.get(j)[1];
                if ((zi > z) != (zj > z) && x < (xj - xi) * (z - zi) / (zj - zi) + xi)
                    inside = !inside;
                j = i;
            }
            return inside;
        }
    }

    // ═══════════ MOB INFO ═══════════
    public static class MobInfo {
        public String name = "";
        /** "boss", "mini_boss", "soldier" */
        public String type = "soldier";
        /** Ordre de visite (1 = premier, 2 = deuxième...) */
        public int priority = 99;
        @SerializedName("coords")
        public double[] coords = null;
        @SerializedName("respawn_seconds")
        public int respawnSeconds = 300;
        public int berries = 0;
        public int xp = 0;
    }

    // ═══════════ WAYPOINT ═══════════
    public static class Waypoint {
        public String name = "";
        public double x, y, z;
        /** "navigation", "farm", "safe", "boss" */
        public String type = "navigation";
    }

    // ═══════════ HELPERS ═══════════

    /** Vérifie si une position est dans une zone d'exclusion */
    public boolean isExcluded(double x, double z) {
        for (FarmZone zone : exclusionZones) {
            if (zone.enabled && zone.contains(x, z)) return true;
        }
        return false;
    }

    /** Mobs triés par priority */
    public List<MobInfo> getMobsByPriority() {
        List<MobInfo> sorted = new ArrayList<>(knownMobs);
        sorted.sort((a, b) -> Integer.compare(a.priority, b.priority));
        return sorted;
    }

    /** Mobs de type donné, triés par priority */
    public List<MobInfo> getMobsByType(String type) {
        List<MobInfo> result = new ArrayList<>();
        for (MobInfo mob : getMobsByPriority()) {
            if (type.equals(mob.type)) result.add(mob);
        }
        return result;
    }

    // ═══════════ PRESET ═══════════

    public static IslandConfig wholeCakeIsland() {
        IslandConfig island = new IslandConfig();
        island.displayName = "TotoLand - Whole Cake Island";

        FarmZone mainZone = new FarmZone();
        mainZone.name = "Zone principale";
        mainZone.points.add(new double[]{-1300, 2250});
        mainZone.points.add(new double[]{-1150, 2250});
        mainZone.points.add(new double[]{-1150, 2350});
        mainZone.points.add(new double[]{-1300, 2350});
        mainZone.center = new double[]{-1225, 2300};
        island.farmZones.add(mainZone);

        MobInfo bege = new MobInfo();
        bege.name = "Capone Bege";
        bege.type = "boss";
        bege.priority = 1;
        bege.coords = new double[]{-1182, 203, 2285};
        bege.respawnSeconds = 900;
        bege.berries = 5000;
        island.knownMobs.add(bege);

        MobInfo soldiers = new MobInfo();
        soldiers.name = "Soldats Biscuits";
        soldiers.type = "soldier";
        soldiers.priority = 99;
        island.knownMobs.add(soldiers);

        return island;
    }
}
