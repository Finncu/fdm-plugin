package de.cyan.fca;

import com.intellij.util.BooleanValueHolder;

public record DirectoryMappingItem(String path, String vsc, BooleanValueHolder enabled) {

   public DirectoryMappingItem(String path, String vsc, boolean enabled) {
      this(path, vsc, new BooleanValueHolder(enabled));
   }

   public void toggle() {
      enabled.setValue(!enabled.getValue());
   }

   public boolean isEnabled() {
      return enabled.getValue();
   }
}
