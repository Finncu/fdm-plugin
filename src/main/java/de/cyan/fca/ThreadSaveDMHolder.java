package de.cyan.fca;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import com.intellij.util.BooleanValueHolder;

public record ThreadSaveDMHolder(BooleanValueHolder idle, Map<String, DirectoryMappingItem> mappings) {

   public ThreadSaveDMHolder(@NotNull Map<String, DirectoryMappingItem> mappings) {
      this(new BooleanValueHolder(true), new HashMap<>(mappings));
   }

   public void insertMappings(Function<Void, List<DirectoryMappingItem>> detector) {
      idle.setValue(false);
      @NotNull
      Map<String, DirectoryMappingItem> newMappings =
            detector.apply(null).stream().collect(Collectors.toMap(DirectoryMappingItem::path, it -> it));
      mappings.clear();
      mappings.putAll(newMappings);
      idle.setValue(true);
   }

   public boolean isIdle() {
      return idle.getValue();
   }
}
