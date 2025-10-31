package de.cyan.fca;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.swing.ListSelectionModel;
import org.jetbrains.annotations.NotNull;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.IPopupChooserBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsDirectoryMapping;
import com.intellij.openapi.vcs.roots.VcsRootDetector;
import com.intellij.ui.SimpleListCellRenderer;

public class FastDirectoryMappingHandler extends AnAction {

   private static final SimpleListCellRenderer.Customizer<? super DirectoryMappingItem> RENDERER = (label, item, i) -> {
      label.setText(item.path());
      label.setIcon(item.isEnabled() ? AllIcons.RunConfigurations.TestPassed : AllIcons.Actions.Close);
      i = item.isEnabled() ? 0 : 1;
   };
   private final String GIT = "Git";
   private static final Map<Project, JBPopup> POPUPS = new HashMap<>();

   @Override
   public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
      Project project = anActionEvent.getProject();
      if (POPUPS.containsKey(project)) {
         POPUPS.remove(project).dispose();
         return;
      }
      ProjectLevelVcsManager manager = ProjectLevelVcsManager.getInstance(Objects.requireNonNull(project));
      Map<String, DirectoryMappingItem> activeMappings = manager.getDirectoryMappings()
            .stream()
            .map(this::toDMI)
            .collect(Collectors.toMap(DirectoryMappingItem::path, item -> item));
      List<DirectoryMappingItem> items = VcsRootDetector.getInstance(project)
            .getOrDetect()
            .stream()
            .map(
               root -> activeMappings.getOrDefault(
                  root.getPath().getPath(),
                  new DirectoryMappingItem(root.getPath().getPath(), GIT, false)))
            .collect(Collectors.toCollection(ArrayList::new));
      IPopupChooserBuilder<DirectoryMappingItem> builder =
            JBPopupFactory.getInstance().createPopupChooserBuilder(items);
      items.sort(Comparator.comparing(DirectoryMappingItem::isEnabled));
      builder.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
            .setCloseOnEnter(false)
            .setCancelOnClickOutside(true)
            .setFilterAlwaysVisible(true)
            .setNamerForFiltering(DirectoryMappingItem::path)
            .setCancelCallback(() -> {
               manager.setDirectoryMappings(
                  items.stream()
                        //.filter(DirectoryMappingItem::isEnabled)
                        .map(item -> new VcsDirectoryMapping(item.path(), item.isEnabled() ? GIT : ""))
                        .toList());
               POPUPS.remove(project);
               return true;
            })
            .setRenderer(SimpleListCellRenderer.create(RENDERER));
      builder.setItemsChosenCallback(is -> {
         is.forEach(DirectoryMappingItem::toggle);
         items.sort(Comparator.comparing(DirectoryMappingItem::isEnabled));
         POPUPS.get(project).getContent().repaint();
      });
      JBPopup popup = builder.createPopup();
      POPUPS.put(project, popup);
      popup.showInBestPositionFor(anActionEvent.getDataContext());
   }

   private DirectoryMappingItem toDMI(VcsDirectoryMapping vcsDirectoryMapping) {
      return new DirectoryMappingItem(vcsDirectoryMapping.getDirectory(), vcsDirectoryMapping.getVcs(),
            !vcsDirectoryMapping.getVcs().isEmpty());
   }

}
