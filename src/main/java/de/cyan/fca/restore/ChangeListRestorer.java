package de.cyan.fca.restore;

import java.util.Collection;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vfs.VirtualFile;

/// Hilfsklasse zur Wiederherstellung von Changes in ihre Ziel-Changelists.
/// Verwaltet die Logik zum Matchen von Dateipfaden und zum Verschieben von Changes.
public class ChangeListRestorer {

   private final ChangeListManager changeListManager;

   public ChangeListRestorer(@NotNull ChangeListManager changeListManager) {
      this.changeListManager = changeListManager;
   }

   /// Findet einen Change basierend auf Dateipfad in der Changelist.
   private Optional<Change> findChangeByPath(@NotNull Collection<Change> changes, @NotNull String targetPath) {
      return changes.stream().filter(c -> {
         VirtualFile file = c.getVirtualFile();
         if (file == null) {
            return false;
         }
         String filePath = file.getPath();
         return targetPath.equals(filePath);
      }).findFirst();
   }

   /// Versucht, einen erstellen oder existierenden Change in die Ziel-Changelist zu verschieben.
   /// Diese Methode ist Kern der Restore-Logik nach Reaktivierung.
   public boolean restoreChangeToTargetList(@NotNull ChangeRestoreEntry entry,
                                            @NotNull String targetChangeListName,
                                            @Nullable String targetChangeListComment) {
      try {
         String filePath = entry.getBeforePath();
         // Ziel-Changelist sicherstellen
         LocalChangeList targetList = changeListManager.findChangeList(targetChangeListName);
         if (targetList == null) {
            targetList = changeListManager
                  .addChangeList(targetChangeListName, targetChangeListComment != null ? targetChangeListComment : "");
         }
         // Aktuellen DefaultChangeList durchsuchen
         LocalChangeList defaultList = changeListManager.getDefaultChangeList();
         Collection<Change> defaultChanges = defaultList.getChanges();
         // Change nach Pfad finden
         Optional<Change> foundChange = findChangeByPath(defaultChanges, filePath);
         if (foundChange.isPresent()) {
            // Change verschieben
            Change changeToMove = foundChange.get();
            changeListManager.moveChangesTo(targetList, changeToMove);
            return true;
         }
         // Change existiert nicht mehr - könnte auf Patch-Fallback hindeuten
         return false;
      } catch (Exception e) {
         // Fehler bei Restore - markierung für Partial-Restore
         return false;
      }
   }

   /// Erstellt die Default-Changelist, falls diese nicht existiert.
   public void ensureDefaultChangeList() {
      LocalChangeList defaultList = changeListManager.findChangeList(null);
      if (defaultList == null) {
         changeListManager.addChangeList("Default", "");
      }
   }
}
