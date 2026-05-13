package de.cyan.fca.restore;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.XCollection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
/// Project-scoped Service für persistente Changelist-Restore-Sessions.
/// Verwaltet Sessions für deaktivierte Directory-Mappings und deren Change-Zuordnungen.
@Service
@State(
    name = "ChangeListRestoreState",
    storages = @Storage("changeListRestore.xml"))
public final class ChangeListRestoreStateService implements
    PersistentStateComponent<ChangeListRestoreStateService> {
  @XCollection(elementName = "sessions")
  private List<MappingRestoreSession> sessions = new ArrayList<>();
  public static ChangeListRestoreStateService getInstance(@NotNull Project project) {
    return project.getService(ChangeListRestoreStateService.class);
  }
  /// Erstellt und speichert eine neue Session für einen deaktivierten Root.
  public MappingRestoreSession createSession(String rootPath, String vcsType) {
    String sessionId = UUID.randomUUID().toString();
    MappingRestoreSession session = new MappingRestoreSession(sessionId, rootPath, vcsType);
    sessions.add(session);
    return session;
  }
  /// Findet die Session für einen Root-Pfad.
  public Optional<MappingRestoreSession> findSessionByRootPath(@NotNull String rootPath) {
    return sessions.stream()
        .filter(s -> rootPath.equals(s.getRootPath()))
        .findFirst();
  }
  /// Findet alle Sessions mit einem bestimmten Status.
  public List<MappingRestoreSession> findSessionsByStatus(@NotNull String status) {
    List<MappingRestoreSession> result = new ArrayList<>();
    for (MappingRestoreSession s : sessions) {
      if (status.equals(s.getStatus())) {
        result.add(s);
      }
    }
    return result;
  }
  /// Gibt alle Sessions zurück.
  public List<MappingRestoreSession> getAllSessions() {
    return new ArrayList<>(sessions);
  }
  /// Entfernt die Session für einen Root-Pfad (beim erneuten Deaktivieren).
  public void removeSessionByRootPath(@NotNull String rootPath) {
    sessions.removeIf(s -> rootPath.equals(s.getRootPath()));
  }
  /// Aktualisiert eine bestehende Session.
  public void updateSession(@NotNull MappingRestoreSession session) {
    findSessionByRootPath(session.getRootPath())
        .ifPresent(existing -> {
          existing.setStatus(session.getStatus());
          existing.setRestoredAt(session.getRestoredAt());
          existing.setEntries(session.getEntries());
        });
  }
  @Nullable
  @Override
  public ChangeListRestoreStateService getState() {
    return this;
  }
  @Override
  public void loadState(@NotNull ChangeListRestoreStateService state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}
