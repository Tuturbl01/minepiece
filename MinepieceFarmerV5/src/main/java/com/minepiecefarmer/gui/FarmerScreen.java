package com.minepiecefarmer.gui;

import com.minepiecefarmer.MinepieceFarmer;
import com.minepiecefarmer.config.ConfigManager;
import com.minepiecefarmer.config.IslandConfig;
import com.minepiecefarmer.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class FarmerScreen extends Screen {

    private enum Tab { COMBAT, FRUIT, SAFETY, ISLAND, MOBS, ZONES, INFO }
    private Tab currentTab = Tab.COMBAT;
    private static final int BG = 0xCC000000, PANEL = 0x80222222;

    private TextFieldWidget mobNameField, mobXField, mobYField, mobZField, mobRespawnField;
    private TextFieldWidget zoneNameField, zoneX1Field, zoneZ1Field, zoneX2Field, zoneZ2Field;
    private String selectedMobType = "mini_boss";

    public FarmerScreen() { super(Text.literal("MinepieceFarmer v5.2")); }

    @Override protected void init() { super.init(); rebuildWidgets(); }

    private void rebuildWidgets() {
        clearChildren();
        ModConfig config = MinepieceFarmer.config;
        int tabW = 52, startX = (width - tabW * Tab.values().length - (Tab.values().length - 1) * 2) / 2;
        int tabY = 18;

        for (Tab tab : Tab.values()) {
            int idx = tab.ordinal();
            String n = tabName(tab);
            if (tab == currentTab) n = "\u00A7a" + n;
            addDrawableChild(ButtonWidget.builder(Text.literal(n),
                    btn -> { currentTab = tab; rebuildWidgets(); })
                    .dimensions(startX + idx * (tabW + 2), tabY, tabW, 18).build());
        }

        int pX = startX, pY = tabY + 24, pW = tabW * Tab.values().length + (Tab.values().length - 1) * 2;

        switch (currentTab) {
            case COMBAT -> buildCombat(pX, pY, pW, config);
            case FRUIT -> buildFruit(pX, pY, pW, config);
            case SAFETY -> buildSafety(pX, pY, pW, config);
            case ISLAND -> buildIsland(pX, pY, pW, config);
            case MOBS -> buildMobs(pX, pY, pW, config);
            case ZONES -> buildZones(pX, pY, pW, config);
            case INFO -> {}
        }

        int bY = height - 28;
        addDrawableChild(ButtonWidget.builder(Text.literal("\u00A7aSave"), btn -> {
            ConfigManager.save(config);
            msg("\u00A7aConfig saved!");
        }).dimensions(startX, bY, 70, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("\u00A7cReload"), btn -> {
            MinepieceFarmer.config = ConfigManager.reload(); rebuildWidgets();
        }).dimensions(startX + 76, bY, 70, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn -> close())
                .dimensions(startX + pW - 55, bY, 55, 18).build());
    }

    // ═══ COMBAT ═══
    private void buildCombat(int x, int y, int w, ModConfig c) {
        int bw = 160, bx = x + (w - bw) / 2, cy = y + 4;
        tog(bx, cy, bw, "Auto-attaque", c.combat.autoAttack, v -> c.combat.autoAttack = v); cy += 22;
        tog(bx, cy, bw, "Farm soldats", c.combat.farmSoldiers, v -> c.combat.farmSoldiers = v); cy += 22;
        tog(bx, cy, bw, "Farm mini-boss", c.combat.farmMiniBoss, v -> c.combat.farmMiniBoss = v); cy += 22;
        tog(bx, cy, bw, "Boss patrol", c.combat.bossPatrolEnabled, v -> c.combat.bossPatrolEnabled = v); cy += 22;
        tog(bx, cy, bw, "Mini patrol", c.combat.miniBossPatrolEnabled, v -> c.combat.miniBossPatrolEnabled = v); cy += 22;
        val(bx, cy, bw, "Boss check(s)", c.combat.bossCheckIntervalSeconds, 60, 600, v -> c.combat.bossCheckIntervalSeconds = v); cy += 22;
        val(bx, cy, bw, "Epee slot", c.combat.swordSlot, 1, 9, v -> c.combat.swordSlot = v); cy += 22;
        tog(bx, cy, bw, "Show timers", c.hud.showTimers, v -> c.hud.showTimers = v);
    }

    // ═══ FRUIT ═══
    private void buildFruit(int x, int y, int w, ModConfig c) {
        int bw = 160, bx = x + (w - bw) / 2, cy = y + 4;
        tog(bx, cy, bw, "Auto-fruit", c.fruit.enabled, v -> c.fruit.enabled = v); cy += 22;
        val(bx, cy, bw, "Fruit slot", c.fruit.slot, 1, 9, v -> c.fruit.slot = v); cy += 22;
        val(bx, cy, bw, "Seuil HP", c.fruit.hpThreshold, 100, 10000, v -> c.fruit.hpThreshold = v); cy += 22;
        val(bx, cy, bw, "Cooldown", c.fruit.cooldownTicks, 20, 400, v -> c.fruit.cooldownTicks = v); cy += 22;
        tog(bx, cy, bw, "Auto-haki", c.haki.enabled, v -> c.haki.enabled = v); cy += 22;
        val(bx, cy, bw, "Haki interval", c.haki.intervalTicks, 100, 2000, v -> c.haki.intervalTicks = v);
    }

    // ═══ SAFETY ═══
    private void buildSafety(int x, int y, int w, ModConfig c) {
        int bw = 160, bx = x + (w - bw) / 2, cy = y + 4;
        val(bx, cy, bw, "HP fuite", c.safety.fleeHp, 50, 5000, v -> c.safety.fleeHp = v); cy += 22;
        val(bx, cy, bw, "HP safe", c.safety.safeHp, 100, 5000, v -> c.safety.safeHp = v); cy += 22;
        tog(bx, cy, bw, "Anti-AFK", c.safety.antiAfk, v -> c.safety.antiAfk = v); cy += 22;
        val(bx, cy, bw, "Timeout(s)", c.safety.stateTimeout / 20, 10, 120, v -> c.safety.stateTimeout = v * 20);
    }

    // ═══ ISLAND ═══
    private void buildIsland(int x, int y, int w, ModConfig c) {
        int bw = 200, bx = x + (w - bw) / 2, cy = y + 4;
        for (var e : c.islands.entrySet()) {
            boolean active = e.getKey().equals(c.activeIsland);
            final String id = e.getKey();
            addDrawableChild(ButtonWidget.builder(
                    Text.literal((active ? "\u00A7a> " : "\u00A77  ") + e.getValue().displayName),
                    btn -> { c.activeIsland = id; rebuildWidgets(); })
                    .dimensions(bx, cy, bw, 18).build());
            cy += 22;
        }
        cy += 6;
        addDrawableChild(ButtonWidget.builder(Text.literal("\u00A7a+ Nouvelle ile"), btn -> {
            String id = "island_" + c.islands.size();
            IslandConfig ni = new IslandConfig();
            ni.displayName = "Ile " + c.islands.size();
            c.islands.put(id, ni); c.activeIsland = id; rebuildWidgets();
        }).dimensions(bx, cy, bw, 18).build());
    }

    // ═══ MOBS — Priority ordering ═══
    private void buildMobs(int x, int y, int w, ModConfig config) {
        IslandConfig island = config.getActiveIsland();
        int cy = y + 2, lx = x + 2, rx = x + w / 2 + 6, hw = w / 2 - 10;

        // Trier par priority pour l'affichage
        List<IslandConfig.MobInfo> sorted = island.getMobsByPriority();

        for (int i = 0; i < sorted.size() && cy < height - 80; i++) {
            IslandConfig.MobInfo mob = sorted.get(i);
            int origIdx = island.knownMobs.indexOf(mob);

            String icon = switch (mob.type) {
                case "boss" -> "\u00A7c[B]";
                case "mini_boss" -> "\u00A7d[M]";
                default -> "\u00A77[S]";
            };
            String coord = mob.coords != null ?
                    String.format(" %.0f,%.0f,%.0f", mob.coords[0], mob.coords[1], mob.coords[2]) : "";
            String label = icon + " #" + mob.priority + " " + mob.name + coord;
            if (label.length() > 28) label = label.substring(0, 28) + "..";

            // Boutons: Haut, Bas, Supprimer
            final int fi = origIdx;

            addDrawableChild(ButtonWidget.builder(Text.literal("\u00A7a^"), btn -> {
                if (mob.priority > 1) {
                    // Trouver le mob avec priority juste en dessous et swap
                    for (IslandConfig.MobInfo other : island.knownMobs) {
                        if (other != mob && other.priority == mob.priority - 1) {
                            other.priority++; break;
                        }
                    }
                    mob.priority--;
                    rebuildWidgets();
                }
            }).dimensions(lx, cy, 15, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("\u00A7cv"), btn -> {
                mob.priority++;
                rebuildWidgets();
            }).dimensions(lx + 16, cy, 15, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("\u00A74X"), btn -> {
                island.knownMobs.remove(fi); rebuildWidgets();
            }).dimensions(lx + 32, cy, 15, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal(label), btn -> {})
                    .dimensions(lx + 49, cy, hw - 49, 16).build());

            cy += 18;
        }

        // ═══ FORMULAIRE AJOUT (droite) ═══
        int fy = y + 2;

        addDrawableChild(ButtonWidget.builder(Text.literal("Type: " + selectedMobType), btn -> {
            if ("mini_boss".equals(selectedMobType)) selectedMobType = "boss";
            else if ("boss".equals(selectedMobType)) selectedMobType = "soldier";
            else selectedMobType = "mini_boss";
            rebuildWidgets();
        }).dimensions(rx, fy, hw, 18).build());
        fy += 22;

        mobNameField = new TextFieldWidget(client.textRenderer, rx, fy, hw, 16, Text.literal("Nom"));
        mobNameField.setPlaceholder(Text.literal("Nom")); mobNameField.setMaxLength(40);
        addDrawableChild(mobNameField); fy += 20;

        int fw = (hw - 6) / 3;
        mobXField = tf(rx, fy, fw, "X");
        mobYField = tf(rx + fw + 3, fy, fw, "Y");
        mobZField = tf(rx + fw * 2 + 6, fy, fw, "Z");
        fy += 20;

        mobRespawnField = new TextFieldWidget(client.textRenderer, rx, fy, hw, 16, Text.literal("R"));
        mobRespawnField.setPlaceholder(Text.literal("Respawn (sec) ex: 300")); mobRespawnField.setMaxLength(10);
        addDrawableChild(mobRespawnField); fy += 22;

        addDrawableChild(ButtonWidget.builder(Text.literal("\u00A7a+ Ajouter"), btn -> addMob(island))
                .dimensions(rx, fy, hw / 2 - 2, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("\u00A7bMa pos"), btn -> {
            if (client.player != null) {
                mobXField.setText("" + (int) client.player.getX());
                mobYField.setText("" + (int) client.player.getY());
                mobZField.setText("" + (int) client.player.getZ());
            }
        }).dimensions(rx + hw / 2 + 2, fy, hw / 2 - 2, 18).build());
    }

    // ═══ ZONES — Exclusion zones ═══
    private void buildZones(int x, int y, int w, ModConfig config) {
        IslandConfig island = config.getActiveIsland();
        int cy = y + 2, lx = x + 4, hw = w - 8;

        // Liste zones existantes
        for (int i = 0; i < island.exclusionZones.size() && cy < height - 100; i++) {
            IslandConfig.FarmZone zone = island.exclusionZones.get(i);
            final int fi = i;
            String label = "\u00A7c[NO-GO] " + zone.name;
            if (zone.points.size() >= 2) {
                label += String.format(" (%.0f,%.0f -> %.0f,%.0f)",
                        zone.points.get(0)[0], zone.points.get(0)[1],
                        zone.points.get(zone.points.size() - 1)[0], zone.points.get(zone.points.size() - 1)[1]);
            }
            addDrawableChild(ButtonWidget.builder(Text.literal("\u00A74X"), btn -> {
                island.exclusionZones.remove(fi); rebuildWidgets();
            }).dimensions(lx, cy, 15, 16).build());
            addDrawableChild(ButtonWidget.builder(Text.literal(label), btn -> {})
                    .dimensions(lx + 17, cy, hw - 17, 16).build());
            cy += 18;
        }

        cy += 8;
        // Formulaire ajout zone rectangulaire (2 coins)
        int formX = lx;
        zoneNameField = new TextFieldWidget(client.textRenderer, formX, cy, hw, 16, Text.literal("Nom"));
        zoneNameField.setPlaceholder(Text.literal("Nom de la zone")); zoneNameField.setMaxLength(30);
        addDrawableChild(zoneNameField); cy += 20;

        int fw = (hw - 6) / 4;
        zoneX1Field = tf(formX, cy, fw, "X1");
        zoneZ1Field = tf(formX + fw + 2, cy, fw, "Z1");
        zoneX2Field = tf(formX + fw * 2 + 4, cy, fw, "X2");
        zoneZ2Field = tf(formX + fw * 3 + 6, cy, fw, "Z2");
        cy += 22;

        addDrawableChild(ButtonWidget.builder(Text.literal("\u00A7c+ Zone no-go"), btn -> addZone(island))
                .dimensions(formX, cy, hw / 2 - 2, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("\u00A7bMa pos (coin 1)"), btn -> {
            if (client.player != null) {
                zoneX1Field.setText("" + (int) client.player.getX());
                zoneZ1Field.setText("" + (int) client.player.getZ());
            }
        }).dimensions(formX + hw / 2 + 2, cy, hw / 2 - 2, 18).build());
    }

    // ═══ FORM HELPERS ═══

    private void addMob(IslandConfig island) {
        String name = mobNameField != null ? mobNameField.getText().trim() : "";
        if (name.isEmpty()) name = selectedMobType + "_" + island.knownMobs.size();
        double mx, my, mz;
        try {
            mx = Double.parseDouble(mobXField.getText().trim());
            my = Double.parseDouble(mobYField.getText().trim());
            mz = Double.parseDouble(mobZField.getText().trim());
        } catch (NumberFormatException e) { msg("\u00A7cCoords invalides !"); return; }

        int resp = 300;
        try { String r = mobRespawnField.getText().trim(); if (!r.isEmpty()) resp = Integer.parseInt(r); } catch (Exception ignored) {}

        IslandConfig.MobInfo mob = new IslandConfig.MobInfo();
        mob.name = name; mob.type = selectedMobType;
        mob.priority = island.knownMobs.size() + 1;
        mob.coords = new double[]{mx, my, mz}; mob.respawnSeconds = resp;
        island.knownMobs.add(mob);
        msg("\u00A7a+ " + name + " (" + selectedMobType + ") prio=" + mob.priority);
        rebuildWidgets();
    }

    private void addZone(IslandConfig island) {
        String name = zoneNameField != null ? zoneNameField.getText().trim() : "zone";
        if (name.isEmpty()) name = "zone_" + island.exclusionZones.size();
        double x1, z1, x2, z2;
        try {
            x1 = Double.parseDouble(zoneX1Field.getText().trim());
            z1 = Double.parseDouble(zoneZ1Field.getText().trim());
            x2 = Double.parseDouble(zoneX2Field.getText().trim());
            z2 = Double.parseDouble(zoneZ2Field.getText().trim());
        } catch (NumberFormatException e) { msg("\u00A7cCoords invalides !"); return; }

        IslandConfig.FarmZone zone = new IslandConfig.FarmZone();
        zone.name = name;
        zone.points.add(new double[]{Math.min(x1, x2), Math.min(z1, z2)});
        zone.points.add(new double[]{Math.max(x1, x2), Math.min(z1, z2)});
        zone.points.add(new double[]{Math.max(x1, x2), Math.max(z1, z2)});
        zone.points.add(new double[]{Math.min(x1, x2), Math.max(z1, z2)});
        island.exclusionZones.add(zone);
        msg("\u00A7c+ Zone no-go: " + name);
        rebuildWidgets();
    }

    private TextFieldWidget tf(int x, int y, int w, String ph) {
        TextFieldWidget f = new TextFieldWidget(client.textRenderer, x, y, w, 16, Text.literal(ph));
        f.setPlaceholder(Text.literal(ph)); f.setMaxLength(10);
        addDrawableChild(f); return f;
    }

    // ═══ WIDGET HELPERS ═══

    private void tog(int x, int y, int w, String label, boolean val, java.util.function.Consumer<Boolean> set) {
        addDrawableChild(ButtonWidget.builder(Text.literal(label + ": " + (val ? "\u00A7aON" : "\u00A7cOFF")),
                btn -> { set.accept(!val); rebuildWidgets(); }).dimensions(x, y, w, 18).build());
    }

    private void val(int x, int y, int w, String label, int val, int min, int max, java.util.function.IntConsumer set) {
        int step = (max - min > 1000) ? 50 : (max - min > 100) ? 10 : (max - min > 20) ? 5 : 1;
        addDrawableChild(ButtonWidget.builder(Text.literal("\u00A7c-"), btn -> {
            set.accept(Math.max(min, val - step)); rebuildWidgets();
        }).dimensions(x, y, 22, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(label + ": \u00A7f" + val), btn -> {})
                .dimensions(x + 24, y, w - 48, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("\u00A7a+"), btn -> {
            set.accept(Math.min(max, val + step)); rebuildWidgets();
        }).dimensions(x + w - 22, y, 22, 18).build());
    }

    private void msg(String s) {
        if (client != null && client.player != null)
            client.player.sendMessage(Text.literal("\u00A78[\u00A76Farmer\u00A78] " + s), false);
    }

    @Override public void render(DrawContext ctx, int mx, int my, float d) {
        ctx.fill(0, 0, width, height, BG);
        super.render(ctx, mx, my, d);
        if (currentTab == Tab.INFO) renderInfo(ctx);
    }

    private void renderInfo(DrawContext ctx) {
        if (client == null) return;
        var tr = client.textRenderer; var d = MinepieceFarmer.data;
        int x = (width - 280) / 2, y = 55, lh = 12;
        ctx.fill(x - 4, y - 4, x + 284, y + lh * 12, PANEL);
        dl(ctx, tr, x, y, "\u00A76\u00A7lMinepieceFarmer v5.2"); y += lh + 4;
        dl(ctx, tr, x, y, "\u00A77Ile: \u00A7f" + d.island); y += lh;
        dl(ctx, tr, x, y, "\u00A77Niv: \u00A7f" + d.level + " \u00A78(" + String.format("%.1f", d.levelPercent) + "%)"); y += lh;
        dl(ctx, tr, x, y, "\u00A77HP: \u00A7f" + d.hp + " \u00A77Mana: \u00A7f" + d.mana); y += lh;
        dl(ctx, tr, x, y, "\u00A77Berries: \u00A76" + d.berries); y += lh;
        dl(ctx, tr, x, y, "\u00A77Session: \u00A7f" + d.sessionDuration() + " \u00A7c" + d.sessionKills + "k"); y += lh;
        dl(ctx, tr, x, y, String.format("\u00A77%.1f k/m \u00A76+%dB \u00A7a+%dXP", d.killsPerMinute(), d.sessionBerriesGained, d.sessionXpGained)); y += lh;
        dl(ctx, tr, x, y, "\u00A77Haki: " + (d.hakiActive ? "\u00A7aON" : "\u00A77OFF"));
    }

    private void dl(DrawContext c, net.minecraft.client.font.TextRenderer t, int x, int y, String s) {
        c.drawText(t, s, x, y, 0xFFFFFF, true);
    }

    private String tabName(Tab t) {
        return switch (t) {
            case COMBAT -> "Combat"; case FRUIT -> "Fruit"; case SAFETY -> "Safety";
            case ISLAND -> "Iles"; case MOBS -> "Mobs"; case ZONES -> "Zones"; case INFO -> "Info";
        };
    }

    @Override public boolean shouldPause() { return false; }
}
