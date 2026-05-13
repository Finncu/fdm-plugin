# Changelist Restore Feature

## Übersicht

Diese Erweiterung des FDM-Plugins implementiert eine automatische Wiederherstellung von Changelist-Zuordnungen beim Reaktivieren von deaktivierten Directory-Mappings.

### Motivation

Wenn ein Directory-Mapping deaktiviert wird, verlieren die Änderungen ihre zugehörigen Changelists, da das VCS-System sie nicht mehr verfolgt. Diese Erweiterung speichert die Changelist-Zuordnungen beim Deaktivieren und stellt sie automatisch wieder her, wenn das Mapping reaktiviert wird.

---

## Architektur

### 1. Datenmodell

#### `ChangeRestoreEntry`
```java
// Persistiert eine einzelne Dateiänderung mit Ziel-Changelist-Information
@Tag("change-entry")
public class ChangeRestoreEntry {
  String beforePath;           // Dateipfad vor Änderung
  String afterPath;            // Dateipfad nach Änderung (optional)
  String changeType;           // NEW, MODIFICATION, DELETION, MOVE, RENAME
  String targetChangeListName; // Ziel-Changelist-Name
  String restoreStatus;        // PENDING, RESTORED, FAILED, PARTIALLY_RESTORED
}
```

#### `MappingRestoreSession`
```java
// Repräsentiert eine deaktivierte Mapping-Session
@Tag("mapping-restore-session")
public class MappingRestoreSession {
  String sessionId;                    // UUID für eindeutige Identifikation
  String rootPath;                     // Pfad des deaktivierten Roots
  String vcsType;                      // VCS-Typ (git, svn, etc.)
  long createdAt;                      // Zeitstempel der Erfassung
  long restoredAt;                     // Zeitstempel der Wiederherstellung
  String status;                       // CAPTURED, RESTORING, RESTORED, PARTIALLY_RESTORED, FAILED
  List<ChangeRestoreEntry> entries;    // Alle erfassten Changes
}
```

### 2. Services

#### `ChangeListRestoreStateService` (Project Service)
- **Rolle**: Persistenter State Management via `PersistentStateComponent`
- **Storage**: `changeListRestore.xml` im Projekt-Config
- **Funktion**: Verwaltet alle Sessions, speichert/lädt Zustände

**Wichtige Methoden**:
```java
MappingRestoreSession createSession(String rootPath, String vcsType)
Optional<MappingRestoreSession> findSessionByRootPath(String rootPath)
List<MappingRestoreSession> findSessionsByStatus(String status)
void updateSession(MappingRestoreSession session)
void removeSessionByRootPath(String rootPath)
```

#### `ChangeListRestoreCoordinator`
- **Rolle**: Orchestriert Capture und Restore Phasen
- **Abhängigkeiten**: `ChangeListRestoreStateService`, `ChangeListManager`, `ChangeListRestorer`

**Wichtige Methoden**:
```java
void captureChangesForDeactivatedRoot(String rootPath, String vcsType)
void restoreChangesForReactivatedRoot(String rootPath)
void clearSessionForRoot(String rootPath)
```

#### `ChangeListRestorer`
- **Rolle**: Hilfklasse für das tatsächliche Verschieben von Changes
- **Funktion**: Matched Dateipfade und verschiebe Changes in Ziel-Changelists

**Wichtige Methoden**:
```java
boolean restoreChangeToTargetList(ChangeRestoreEntry entry, String targetChangeListName, String comment)
void ensureDefaultChangeList()
```

---

## Workflow

### Phase 1: Capture (beim Deaktivieren)

```
User schaltet Mapping über UI aus
        ?
FastDirectoryMappingHandler.setCancelCallback()
        ?
ChangeListRestoreCoordinator.captureChangesForDeactivatedRoot()
        ?
1. Alte Session (falls vorhanden) ersetzen
2. Neue Session erstellen mit UUID
3. Alle Changes unter dem Root durchsuchen
4. Für jeden Change: Metadaten speichern
   - Dateipfad
   - Zugehörige Changelist (Name, Kommentar)
   - Change-Typ
5. Session mit Status "CAPTURED" persistieren
```

**Persistenz**: XML-Format in `changeListRestore.xml`

```xml
<ChangeListRestoreState>
  <sessions>
    <mapping-restore-session sessionId="uuid-123" rootPath="/path/to/root" 
                             vcsType="git" status="CAPTURED">
      <entries>
        <change-entry beforePath="/path/to/file.java" 
                      changeType="MODIFICATION" 
                      targetChangeListName="Feature X"
                      restoreStatus="PENDING"/>
      </entries>
    </mapping-restore-session>
  </sessions>
</ChangeListRestoreState>
```

### Phase 2: Restore (beim Reaktivieren)

```
User schaltet Mapping über UI an
        ?
FastDirectoryMappingHandler.setCancelCallback()
        ?
ChangeListRestoreCoordinator.restoreChangesForReactivatedRoot()
        ?
1. Session für Root laden
2. Für jeden erfassten Change:
   a) Ziel-Changelist sicherstellen (ggf. neu anlegen)
   b) VirtualFile nach Pfad finden
   c) Change in Ziel-Changelist verschieben
3. Session-Status aktualisieren
   - "RESTORED" (alle erfolgreich)
   - "PARTIALLY_RESTORED" (manche fehlgeschlagen)
   - "FAILED" (alle fehlgeschlagen)
```

---

## Fehlerbehandlung und Edge Cases

### 1. Changelist existiert nicht mehr
- **Szenario**: Benutzer hat Changelist gelöscht zwischen Deaktivierung und Reaktivierung
- **Lösung**: Neue Changelist mit ursprünglichem Namen und Kommentar anlegen

### 2. Datei existiert nicht mehr
- **Szenario**: Datei wurde gelöscht/umbenannt zwischen Deaktivierung und Reaktivierung
- **Lösung**: Eintrag als "FAILED" markieren, Session wird "PARTIALLY_RESTORED"
- **Zukünftig**: Patch-Fallback implementieren

### 3. Change nicht mehr vorhanden
- **Szenario**: Änderung wurde bereits committed
- **Lösung**: Eintrag als "FAILED" markieren
- **Session-Status**: "PARTIALLY_RESTORED"

### 4. Cleanup-Strategie
- **Alte Sessions**: Werden beim erneuten Deaktivieren desselben Roots ERSETZT
- **Nicht automatisches Löschen**: Sessions bleiben über IDE-Restarts hinaus
- **Manuelles Cleanup**: Über API-Methode `clearSessionForRoot()`

---

## Integration mit FastDirectoryMappingHandler

Der bestehende Handler wurde um Restore-Logik erweitert:

```java
.setCancelCallback(() -> {
   ChangeListRestoreCoordinator coordinator = new ChangeListRestoreCoordinator(project);

   // Vergleiche alte vs neue Mappings
   // Für deaktivierte Roots: captureChangesForDeactivatedRoot()
   // Für reaktivierte Roots: restoreChangesForReactivatedRoot()

   manager.setDirectoryMappings(newMappings);
   return true;
})
```

---

## Persistierungsstrategie

### Storage-Location
- **Datei**: `.idea/changeListRestore.xml`
- **Format**: IntelliJ Platform XML Serialization (@XCollection, @Attribute)
- **Scope**: Pro Project

### Warum nicht `Shelve`?
1. Shelve ist für längerfristige Speicherung, nicht Metadaten
2. Weniger invasiv (keine sichtbaren Shelf-Einträge)
3. Bessere Kontrolle über Lifecycle
4. Einfachere Fehlerbehandlung

---

## Phase 2: Patch-Fallback (Zukunft)

Geplante Erweiterung für Szenarien, wo Changes nicht direkt gefunden werden:

```
Restore fehlgeschlagen (Datei/Change nicht vorhanden)
        ?
Gespeicherte Patch-Daten laden
        ?
Patch anwenden (mit ggf. Conflict-Dialog)
        ?
Change in Ziel-Changelist verschieben
        ?
Status: RESTORED (mit Patch-Fallback)
```

**Zu speichernde Daten**:
- Unified Diff gegen Basis-Revision
- Oder kompletter File-Content (für neue Dateien)
- Hash für Reconciliation

---

## Testing

### Unit Tests (geplant)
- Session-Lifecycle
- Entry-Matching nach Pfad
- Status-Transitionen

### Integration Tests (geplant)
- Capture + Restore Round-Trip
- Changelist-Neuanlage
- Error-Recovery Scenarios

---

## Debugging

Alle Sessions abrufen:
```java
ChangeListRestoreStateService service = ChangeListRestoreStateService.getInstance(project);
List<MappingRestoreSession> sessions = service.getAllSessions();
```

Session-Details:
```java
Optional<MappingRestoreSession> session = service.findSessionByRootPath(rootPath);
if (session.isPresent()) {
  System.out.println(session.get()); // toString() aufruf
}
```

---

## Performance-Überlegungen

1. **Capture**: Lineare Komplexität O(n) über alle Changes
2. **Restore**: O(m*k) wobei m=Einträge, k=Changes in Default-List
3. **Storage**: XML serialisiert Metadaten nur (kein Binary-Content in Stufe 1)
4. **Persistence**: Automatisch beim IDE-Shutdown

**Optimierungsmöglichkeiten**:
- Indizierung nach Root-Pfad
- Batch-Restore mit ChangeListManager-Transaktionen
- Lazy-Loading von Sessions

