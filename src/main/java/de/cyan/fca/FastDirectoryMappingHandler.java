package de.cyan.fca;

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
   };
   private final String GIT = "Git";
   private static final Map<Project, ThreadSaveDMHolder> HOLDER = new HashMap<>();
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
      if (!HOLDER.containsKey(project))
         HOLDER.put(project, new ThreadSaveDMHolder(activeMappings));
      ThreadSaveDMHolder holder = HOLDER.get(project);
      if (holder.isIdle())
         detectAllRootsAsync(project, activeMappings);
      List<DirectoryMappingItem> items = holder.mappings().values().stream().toList();
      IPopupChooserBuilder<DirectoryMappingItem> builder =
            JBPopupFactory.getInstance().createPopupChooserBuilder(items);
      builder.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
            .setCloseOnEnter(false)
            .setCancelOnClickOutside(true)
            .setFilterAlwaysVisible(true)
            .setNamerForFiltering(DirectoryMappingItem::path)
            .setCancelCallback(() -> {
               manager.setDirectoryMappings(
                  items.stream()
                        .map(item -> new VcsDirectoryMapping(item.path(), item.isEnabled() ? GIT : ""))
                        .toList());
               POPUPS.remove(project);
               return true;
            })
            .setRenderer(SimpleListCellRenderer.create(RENDERER));
      builder.setItemChosenCallback(item -> {
         item.toggle();
         POPUPS.get(project).getContent().repaint();
      });
      JBPopup popup = builder.createPopup();
      POPUPS.put(project, popup);
      popup.showInBestPositionFor(anActionEvent.getDataContext());
   }

   private void detectAllRootsAsync(@NotNull Project project, Map<String, DirectoryMappingItem> activeMappings) {
      new Thread(() -> {
         HOLDER.get(project)
               .insertMappings(
                  voi -> VcsRootDetector.getInstance(project)
                        .getOrDetect()
                        .stream()
                        .map(
                           root -> activeMappings.getOrDefault(
                              root.getPath().getPath(),
                              new DirectoryMappingItem(root.getPath().getPath(),
                                    root.getVcs() != null ? root.getVcs().getName() : "", false)))
                        .toList());
      }).start();
   }

   private void detectAllRoots(Project project, Map<String, DirectoryMappingItem> activeMappings) {
      HOLDER.get(project)
            .insertMappings(
               voi -> VcsRootDetector.getInstance(project)
                     .getOrDetect()
                     .stream()
                     .map(
                        root -> activeMappings.getOrDefault(
                           root.getPath().getPath(),
                           new DirectoryMappingItem(root.getPath().getPath(),
                                 root.getVcs() != null ? root.getVcs().getName() : "", false)))
                     .toList());

   }

   private DirectoryMappingItem toDMI(VcsDirectoryMapping vcsDirectoryMapping) {
      return new DirectoryMappingItem(vcsDirectoryMapping.getDirectory(), vcsDirectoryMapping.getVcs(),
            !vcsDirectoryMapping.getVcs().isEmpty());
   }

}
