package de.cyan.fca.restore;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;

/// Persistenter Eintrag für eine Änderung beim Deaktivieren eines Directory-Mappings.
/// Enthält Metadaten zur Wiederzuordnung der Datei zur korrekten Changelist nach Reaktivierung.
@Tag("change-entry")
public class ChangeRestoreEntry {

   @Attribute
   private String beforePath;
   @Attribute
   private String afterPath;
   @Attribute
   private String changeType;
   @Attribute
   private String targetChangeListName;
   @Attribute
   private String targetChangeListComment;
   @Attribute
   private String filePathHash;
   @Attribute
   private String contentHashBeforeCapture;
   @Attribute
   private String restoreStatus;
   @Attribute
   private String patchReference;

   public ChangeRestoreEntry() {
   }

   /// Erstellt einen neuen Changelist-Restore-Eintrag.
   public ChangeRestoreEntry(@NotNull String beforePath, @Nullable String afterPath, @NotNull String changeType,
                             @NotNull String targetChangeListName, @Nullable String targetChangeListComment) {
      this.beforePath = beforePath;
      this.afterPath = afterPath;
      this.changeType = changeType;
      this.targetChangeListName = targetChangeListName;
      this.targetChangeListComment = targetChangeListComment;
      this.restoreStatus = "PENDING";
   }

   public String getBeforePath() {
      return beforePath;
   }

   public void setBeforePath(@NotNull String beforePath) {
      this.beforePath = beforePath;
   }

   public String getAfterPath() {
      return afterPath;
   }

   public void setAfterPath(@Nullable String afterPath) {
      this.afterPath = afterPath;
   }

   public String getChangeType() {
      return changeType;
   }

   public void setChangeType(@NotNull String changeType) {
      this.changeType = changeType;
   }

   public String getTargetChangeListName() {
      return targetChangeListName;
   }

   public void setTargetChangeListName(@NotNull String targetChangeListName) {
      this.targetChangeListName = targetChangeListName;
   }

   public String getTargetChangeListComment() {
      return targetChangeListComment;
   }

   public void setTargetChangeListComment(@Nullable String targetChangeListComment) {
      this.targetChangeListComment = targetChangeListComment;
   }

   public String getFilePathHash() {
      return filePathHash;
   }

   public void setFilePathHash(@Nullable String filePathHash) {
      this.filePathHash = filePathHash;
   }

   public String getContentHashBeforeCapture() {
      return contentHashBeforeCapture;
   }

   public void setContentHashBeforeCapture(@Nullable String contentHashBeforeCapture) {
      this.contentHashBeforeCapture = contentHashBeforeCapture;
   }

   public String getRestoreStatus() {
      return restoreStatus;
   }

   public void setRestoreStatus(@NotNull String restoreStatus) {
      this.restoreStatus = restoreStatus;
   }

   public String getPatchReference() {
      return patchReference;
   }

   public void setPatchReference(@Nullable String patchReference) {
      this.patchReference = patchReference;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o)
         return true;
      if (o == null || getClass() != o.getClass())
         return false;
      ChangeRestoreEntry entry = (ChangeRestoreEntry) o;
      return Objects.equals(beforePath, entry.beforePath) && Objects.equals(afterPath, entry.afterPath)
         && Objects.equals(changeType, entry.changeType)
         && Objects.equals(targetChangeListName, entry.targetChangeListName);
   }

   @Override
   public int hashCode() {
      return Objects.hash(beforePath, afterPath, changeType, targetChangeListName);
   }

   @Override
   public String toString() {
      return "ChangeRestoreEntry{beforePath='" + beforePath + '\'' + ", changeType='" + changeType + '\''
         + ", targetCL='" + targetChangeListName + '\'' + "}";
   }
}
