package de.cyan.fca.restore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vfs.VirtualFile;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
/// Orchestriert Capture und Restore von Changelist-Zuordnungen.
/// Verantwortlich für die Logik beim Deaktivieren/Reaktivieren von Directory-Mappings.
public class ChangeListRestoreCoordinator {
  private final Project project;
  private final ChangeListManager changeListManager;
  private final ChangeListRestoreStateService stateService;
  public ChangeListRestoreCoordinator(@NotNull Project project) {
    this.project = project;
    this.changeListManager = ChangeListManager.getInstance(project);
    this.stateService = ChangeListRestoreStateService.getInstance(project);
  }
  /// Capture Phase: Erfasst alle Changes eines Roots und speichert Changelist-Zuordnungen.
  public void captureChangesForDeactivatedRoot(@NotNull String rootPath, @NotNull String vcsType) {
    stateService.removeSessionByRootPath(rootPath);
    MappingRestoreSession session = stateService.createSession(rootPath, vcsType);
    List<LocalChangeList> allChangeLists = changeListManager.getChangeLists();
    for (LocalChangeList changeList : allChangeLists) {
      for (Change change : changeList.getChanges()) {
        VirtualFile file = change.getVirtualFile();
        if (file != null) {
          String filePath = file.getPath();
          if (filePath.startsWith(rootPath)) {
            ChangeRestoreEntry entry = new ChangeRestoreEntry(
                filePath, filePath, "MODIFICATION",
                changeList.getName(), changeList.getComment());
            session.addEntry(entry);
          }
        }
      }
    }
    session.setStatus("CAPTURED");
    stateService.updateSession(session);
  }
  /// Restore Phase: Versucht Changes in ihre urspruenglichen Changelists zurueckzuschieben.
  public void restoreChangesForReactivatedRoot(@NotNull String rootPath) {
    Optional<MappingRestoreSession> sessionOpt = stateService.findSessionByRootPath(rootPath);
    if (!sessionOpt.isPresent()) {
      return;
    }
    MappingRestoreSession session = sessionOpt.get();
    session.setStatus("RESTORING");
    session.setRestoredAt(System.currentTimeMillis());
    ChangeListRestorer restorer = new ChangeListRestorer(changeListManager);
    int successCount = 0;
    int failureCount = 0;
    for (ChangeRestoreEntry entry : session.getEntries()) {
      String changeListName = entry.getTargetChangeListName();
      String changeListComment = entry.getTargetChangeListComment();
      boolean restored = restorer.restoreChangeToTargetList(entry, changeListName, changeListComment);
      if (restored) {
        entry.setRestoreStatus("RESTORED");
        successCount++;
      } else {
        entry.setRestoreStatus("FAILED");
        failureCount++;
      }
    }
    if (failureCount > 0) {
      session.setStatus("PARTIALLY_RESTORED");
    } else {
      session.setStatus("RESTORED");
    }
    stateService.updateSession(session);
  }
  /// Hilfsmethode zum Loeschen einer Restore-Session nach erfolgreichem Restore.
  public void clearSessionForRoot(@NotNull String rootPath) {
    stateService.removeSessionByRootPath(rootPath);
  }
  /// Gibt alle aktuellen Sessions zurueck.
  public List<MappingRestoreSession> getAllActiveSessions() {
    return stateService.getAllSessions();
  }
}
