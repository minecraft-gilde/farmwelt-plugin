# Farmwelt-Plugin – Spezifikation

## Ziel des Plugins

Das Plugin soll auf dem Minecraft-Gilde-Server einen zentralen Einstieg für Farmwelten bereitstellen und gleichzeitig Ressourcenabbau in normalen Welten erkennen.

Das Plugin soll Spieler freundlich in die Farmwelten lenken und Moderatoren entlasten. Es soll nicht primär als hartes Strafsystem gebaut werden, sondern als Lenkungs-, Warn- und Moderationshilfe.

## Server-Kontext

- Server läuft unter Folia.
- Zielplattform: Paper/Folia-Plugin.
- Hauptbefehl: `/farmwelt`
- Aktuell gibt es mehrere Alias-Befehle wie `/farmwelt`, `/netherfarm`, `/endfarm`.
- Zukünftig soll es nur noch den zentralen Befehl `/farmwelt` geben.
- Das Plugin muss wirklich Folia-kompatibel entwickelt werden.

## Grundsatz zu Folia

Das Plugin darf nicht wie ein klassisches Paper-Plugin gebaut werden, wenn dadurch Folia-Probleme entstehen.

Wichtig:

- Keine klassische Bukkit-Scheduler-Logik für welt- oder spielerbezogene Aktionen verwenden.
- Keine unsicheren Zugriffe auf Welt-, Block- oder Spielerzustand aus falschen Threads.
- Für spielerbezogene Aktionen geeignete Folia-Mechanismen verwenden.
- Für Teleports bevorzugt `teleportAsync` verwenden.
- Geteilte Datenstrukturen müssen thread-sicher sein.
- `folia-supported: true` darf nur gesetzt werden, wenn der Code tatsächlich Folia-sicher ist.

## Funktionsbereich 1: `/farmwelt`-GUI

Der Befehl `/farmwelt` soll eine GUI öffnen.

Die GUI soll mit Standard-Minecraft-Elementen umgesetzt werden, z. B. als Kisten-GUI.

In der GUI werden Farmwelten durch Items symbolisiert:

- Normale Farmwelt: `GRASS_BLOCK`
- Netherfarm: `NETHERRACK`
- Endfarm: `END_STONE`

Beim Klick auf ein Item wird der Spieler zur konfigurierten Zielposition teleportiert.

Die GUI soll über die Config steuerbar sein:

- Anzeigename
- Icon/Material
- Slot
- Lore
- Zielwelt
- X/Y/Z
- Yaw/Pitch

## Funktionsbereich 2: Ressourcenabbau-Erkennung

Das Plugin soll erkennen, wenn Spieler in normalen Welten Ressourcen abbauen.

Die überwachten Welten sollen konfigurierbar sein.

Farmwelten und andere Ausnahmewelten sollen ignoriert werden.

Spieler mit Bypass-Permission sollen ignoriert werden.

Es soll zwei getrennte Ressourcenkategorien geben.

### Oberirdische Ressourcen

Diese Kategorie gilt für Ressourcen, die typischerweise oberhalb der jeweiligen Seehöhe abgebaut werden.

Beispiele:

- Logs/Holz
- Sand
- Red Sand
- Gravel
- Clay

### Unterirdische Ressourcen

Diese Kategorie gilt für Ressourcen, die typischerweise unterhalb der jeweiligen Seehöhe abgebaut werden.

Beispiele:

- Coal Ore
- Iron Ore
- Copper Ore
- Gold Ore
- Redstone Ore
- Lapis Ore
- Diamond Ore
- Emerald Ore
- Ancient Debris
- jeweilige Deepslate-Varianten

Die Trennung ist wichtig, weil Erze oberirdisch auf dem Server auch als Baumaterial genutzt werden können.

## Eskalationsmodell

Das Plugin soll nicht sofort bestrafen.

Stattdessen soll es in Stufen arbeiten:

1. Auffälliger Ressourcenabbau erkannt
   - Spieler erhält eine Warnung.
   - Hinweis auf `/farmwelt`.

2. Spieler macht weiter
   - In einem aktiven Durchsetzungsmodus kann der Blockabbau verhindert werden.

3. Spieler macht weiter
   - Moderatoren werden benachrichtigt.

4. Spieler macht deutlich weiter
   - Spieler kann gekickt werden.

5. Wiederholter Verstoß
   - Optionaler Konsolenbefehl, z. B. Jail-Befehl:
     `jail {player} farmwelt`

Alle Schwellenwerte und Aktionen müssen konfigurierbar sein.

## Betriebsmodi

Das Plugin soll verschiedene Modi unterstützen:

### `audit`

Verstöße werden nur geloggt und optional Moderatoren gemeldet.

Es werden keine Blöcke blockiert, keine Spieler gekickt und keine Jail-Befehle ausgeführt.

### `warn`

Spieler werden gewarnt, aber es wird noch nicht hart eingegriffen.

### `enforce`

Warnungen, Blockabbruch, Staff-Benachrichtigungen, Kick und optionaler Jail-Befehl können aktiv werden.

## Beispiel-Config

```yaml
farmworlds:
  overworld:
    enabled: true
    display-name: "Farmwelt"
    icon: GRASS_BLOCK
    slot: 11
    world: farmwelt
    x: 0.5
    y: 80
    z: 0.5
    yaw: 0
    pitch: 0
    lore:
      - "Normale Farmwelt"
      - "Für Holz, Sand, Erde und weitere Ressourcen"

  nether:
    enabled: true
    display-name: "Netherfarm"
    icon: NETHERRACK
    slot: 13
    world: netherfarm
    x: 0.5
    y: 80
    z: 0.5
    yaw: 0
    pitch: 0
    lore:
      - "Farmwelt für Nether-Ressourcen"

  end:
    enabled: true
    display-name: "Endfarm"
    icon: END_STONE
    slot: 15
    world: endfarm
    x: 0.5
    y: 80
    z: 0.5
    yaw: 0
    pitch: 0
    lore:
      - "Farmwelt für End-Ressourcen"

resource-monitor:
  enabled: true
  mode: audit

  monitored-worlds:
    - world
    - world_nether
    - world_the_end

  ignored-worlds:
    - farmwelt
    - netherfarm
    - endfarm

  bypass-permission: farmwelt.bypass
  notify-permission: farmwelt.notify

  sea-level:
    world: 63
    world_nether: 32
    world_the_end: 50

  violation-window-seconds: 600

  surface-resources:
    - OAK_LOG
    - SPRUCE_LOG
    - BIRCH_LOG
    - JUNGLE_LOG
    - ACACIA_LOG
    - DARK_OAK_LOG
    - MANGROVE_LOG
    - CHERRY_LOG
    - SAND
    - RED_SAND
    - GRAVEL
    - CLAY

  underground-resources:
    - COAL_ORE
    - DEEPSLATE_COAL_ORE
    - IRON_ORE
    - DEEPSLATE_IRON_ORE
    - COPPER_ORE
    - DEEPSLATE_COPPER_ORE
    - GOLD_ORE
    - DEEPSLATE_GOLD_ORE
    - REDSTONE_ORE
    - DEEPSLATE_REDSTONE_ORE
    - LAPIS_ORE
    - DEEPSLATE_LAPIS_ORE
    - DIAMOND_ORE
    - DEEPSLATE_DIAMOND_ORE
    - EMERALD_ORE
    - DEEPSLATE_EMERALD_ORE
    - ANCIENT_DEBRIS

  actions:
    warning:
      after-blocks: 5
      message: "Bitte nutze für Ressourcen die Farmwelten mit /farmwelt."

    cancel-break:
      after-blocks: 8
      message: "Dieser Ressourcenabbau ist in der Hauptwelt nicht erlaubt. Nutze /farmwelt."

    notify-staff:
      after-blocks: 10
      message: "{player} baut Ressourcen in {world} bei {x} {y} {z} ab."

    kick:
      after-blocks: 18
      reason: "Bitte nutze für Ressourcen die Farmwelten: /farmwelt"

    jail:
      after-blocks: 30
      command: "jail {player} farmwelt"
Permissions
farmwelt.use
Darf /farmwelt verwenden.
farmwelt.bypass
Wird vom Ressourcenmonitor ignoriert.
farmwelt.notify
Erhält Moderator-Benachrichtigungen.
farmwelt.admin
Für spätere Admin-Funktionen vorgesehen.
Gewünschte Code-Struktur
src/main/java/de/minecraftgilde/farmwelt/
 ├─ FarmweltPlugin.java
 ├─ command/
 │   └─ FarmweltCommand.java
 ├─ gui/
 │   ├─ FarmweltMenu.java
 │   └─ FarmweltMenuHolder.java
 ├─ listener/
 │   ├─ FarmweltGuiListener.java
 │   ├─ ResourceBreakListener.java
 │   └─ BlockPlaceListener.java
 ├─ service/
 │   ├─ FarmweltTeleportService.java
 │   ├─ ResourceDetectionService.java
 │   ├─ ViolationService.java
 │   ├─ PunishmentService.java
 │   └─ MessageService.java
 └─ config/
     └─ ConfigManager.java
```
Technische Anforderungen
Java-Plugin für Paper/Folia.
Gradle-Projekt.
Package: de.minecraftgilde.farmwelt
Plugin-Name: Farmwelt
Repository-Name: farmwelt-plugin
Keine Datenbank in Version 1.
Keine harte Abhängigkeit von EssentialsX.
Jail/Kick soll über konfigurierbare Befehle möglich sein.
Keine hart codierten Weltnamen außer in der Beispiel-Config.
Konfigurierbare Nachrichten.
Saubere Fehlerbehandlung, wenn eine konfigurierte Welt nicht existiert.
Audit-Modus muss vor harten Sanktionen nutzbar sein.
Kommentare im Code auf Deutsch, falls Kommentare eingefügt werden.
Entwicklungsreihenfolge

Die Umsetzung soll schrittweise erfolgen:

Projektgrundlage erstellen.
/farmwelt-Befehl und GUI bauen.
Folia-sicheren Teleport-Service bauen.
Ressourcenmonitor im Audit-Modus bauen.
Violation-Zähler mit Zeitfenster bauen.
Warnungen, Blockabbruch und Staff-Benachrichtigung bauen.
Kick und optionalen Jail-Befehl bauen.