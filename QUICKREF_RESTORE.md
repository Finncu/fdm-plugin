# Quick Reference: Changelist Restore Feature

## ? Paket-Struktur

```
de.cyan.fca
??? FastDirectoryMappingHandler.java (erweitert)
??? restore/
    ??? ChangeRestoreEntry.java
    ??? MappingRestoreSession.java
    ??? ChangeListRestoreStateService.java (Project Service)
    ??? ChangeListRestoreCoordinator.java
    ??? ChangeListRestorer.java
```

---

## ? Verwendung (für Entwickler)

### Session abrufen
```java
ChangeListRestoreStateService service = 
    ChangeListRestoreStateService.getInstance(project);

Optional<MappingRestoreSession> session = 
    service.findSessionByRootPath("/path/to/root");
```

### Capture manuell anstoßen
```java
ChangeListRestoreCoordinator coordinator = 
    new ChangeListRestoreCoordinator(project);
    
coordinator.captureChangesForDeactivatedRoot(
    "/path/to/root", "git");
```

### Restore manuell anstoßen
```java
coordinator.restoreChangesForReactivatedRoot(
    "/path/to/root");
```

### Session-Status abfragen
```java
List<MappingRestoreSession> sessions = 
    service.getAllSessions();

for (MappingRestoreSession s : sessions) {
    System.out.println(s.getRootPath() + " -> " + s.getStatus());
    // CAPTURED, RESTORING, RESTORED, PARTIALLY_RESTORED, FAILED
}
```

---

## ? Key Methoden

### `ChangeListRestoreCoordinator`

| Methode | Beschreibung | Nutzer |
|---------|-------------|--------|
| `captureChangesForDeactivatedRoot(root, vcs)` | Speichert Changes beim Deaktivieren | Automatisch via UI |
| `restoreChangesForReactivatedRoot(root)` | Stellt Changes beim Reaktivieren wieder her | Automatisch via UI |
| `clearSessionForRoot(root)` | Löscht Session Manual | Optionale API |
| `getAllActiveSessions()` | Gibt alle Sessions zurück | Debugging |

### `ChangeListRestorer`

| Methode | Beschreibung |
|---------|-------------|
| `restoreChangeToTargetList(entry, listName, comment)` | Kernlogik: Verschiebe Change in Changelist |
| `ensureDefaultChangeList()` | Sicherstelle Default-Changelist existiert |

---

## ? Status-Transitions

```
CAPTURED
    ?
[Reaktivierung]
    ?
RESTORING
    ?
Versuche für jeden Entry einen Change zu finden
    ?
?????????????????????????????????????????
?             ?              ?
RESTORED      PARTIALLY      FAILED
(alle OK)     RESTORED       (alle NOK)
              (manche OK)
```

---

## ? Debugging

### Alle Sessions ausgeben
```java
var service = ChangeListRestoreStateService.getInstance(project);
service.getAllSessions().forEach(System.out::println);
```

### Session-Einträge prüfen
```java
Optional<MappingRestoreSession> s = service.findSessionByRootPath("/path");
if (s.isPresent()) {
    for (ChangeRestoreEntry e : s.get().getEntries()) {
        System.out.println(e); // toString() zeigt: beforePath, changeType, targetCL
    }
}
```

### Session löschen (manuell)
```java
service.removeSessionByRootPath("/path/to/root");
```

---

## ?? Edge Cases

| Fall | Verhalten | Status |
|------|-----------|--------|
| Changelist gelöscht | Neue CL mit ursprünglichem Namen anlegen | RESTORED |
| Datei nicht mehr vorhanden | Eintrag markiert als FAILED | PARTIALLY_RESTORED |
| Change nicht findbar | Eintrag markiert als FAILED | PARTIALLY_RESTORED |
| IDE-Restart | Session bleibt in .idea/changeListRestore.xml | ? Persistiert |
| Erneutes Deaktivieren | Alte Session wird ersetzt | Cleanup |

---

## ? Persistenzdatei

**Ort**: `.idea/changeListRestore.xml`

**Format**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<ChangeListRestoreState>
  <sessions>
    <mapping-restore-session sessionId="uuid" rootPath="/path" 
                             vcsType="git" status="CAPTURED">
      <entries>
        <change-entry beforePath="/file.java" changeType="MODIFICATION"
                      targetChangeListName="Feature X" restoreStatus="PENDING"/>
      </entries>
    </mapping-restore-session>
  </sessions>
</ChangeListRestoreState>
```

---

## ? Zukünftige Erweiterungen (Stufe 2)

### Patch-Storage
```java
// Geplant für nicht mehr findbare Dateien
public class PatchStorage {
    void savePatch(String patchId, String unifiedDiff)
    String loadPatch(String patchId)
    void applyPatchWithConflictResolution(...)
}
```

### Merge-Handling
```java
// Geplant für Konflikte beim Patch-Apply
public class MergeCoordinator {
    void showMergeDialog(...)
    void resolveMergeConflict(...)
}
```

---

## ? Support

- **Mehr Details**: Siehe `RESTORE_FEATURE.md`
- **Build-Status**: `IMPLEMENTATION_SUMMARY.md`
- **JavaDoc**: Inline in allen Klassen

