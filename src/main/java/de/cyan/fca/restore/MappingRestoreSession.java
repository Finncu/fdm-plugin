package de.cyan.fca.restore;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.XCollection;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
/**
 * Persistente Restore-Session für deaktiviertes Directory-Mapping.
 * Erfasst Änderungen und speichert Metadaten zur Wiederzuordnung.
 */
@Tag("mapping-restore-session")
public class MappingRestoreSession {
  @Attribute private String sessionId;
  @Attribute private String rootPath;
  @Attribute private String vcsType;
  @Attribute private long createdAt;
  @Attribute private long restoredAt;
  @Attribute private String status;
  @XCollection(elementName = "entries")
  private List<ChangeRestoreEntry> entries = new ArrayList<>();
  public MappingRestoreSession() {}
  public MappingRestoreSession(String sessionId, String rootPath, String vcsType) {
    this.sessionId = sessionId;
    this.rootPath = rootPath;
    this.vcsType = vcsType;
    this.createdAt = System.currentTimeMillis();
    this.status = "CAPTURED";
  }
  public String getSessionId() { return sessionId; }
  public void setSessionId(String sessionId) { this.sessionId = sessionId; }
  public String getRootPath() { return rootPath; }
  public void setRootPath(String rootPath) { this.rootPath = rootPath; }
  public String getVcsType() { return vcsType; }
  public void setVcsType(String vcsType) { this.vcsType = vcsType; }
  public long getCreatedAt() { return createdAt; }
  public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
  public long getRestoredAt() { return restoredAt; }
  public void setRestoredAt(long restoredAt) { this.restoredAt = restoredAt; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public List<ChangeRestoreEntry> getEntries() { return entries; }
  public void setEntries(List<ChangeRestoreEntry> entries) { this.entries = entries; }
  /// Fügt einen neuen Restore-Eintrag zur Session hinzu.
  public void addEntry(@NotNull ChangeRestoreEntry entry) {
    entries.add(entry);
  }
  /// Prüft, ob diese Session für den Pfad zuständig ist.
  public boolean appliesToPath(@NotNull String path) {
    return path != null && path.startsWith(rootPath);
  }
  @Override
  public String toString() {
    return "MappingRestoreSession{" + "sessionId='" + sessionId + '\'' + ", rootPath='" + rootPath + '\'' + ", status='" + status + '\'' + "}";
  }
}
