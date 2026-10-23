package school.hei.patrimoine.visualisation.swing.ihm.google;

import static java.awt.Toolkit.getDefaultToolkit;
import static javax.swing.SwingUtilities.invokeLater;
import static school.hei.patrimoine.visualisation.swing.ihm.google.modele.PatriLangStagingFileManager.getStagedDoneFiles;
import static school.hei.patrimoine.visualisation.swing.ihm.google.modele.PatriLangStagingFileManager.getStagedPlannedFiles;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;
import java.util.Set;
import school.hei.patrimoine.google.GoogleApiUtilities;
import school.hei.patrimoine.visualisation.swing.ihm.google.component.app.App;
import school.hei.patrimoine.visualisation.swing.ihm.google.component.app.Page;
import school.hei.patrimoine.visualisation.swing.ihm.google.component.appbar.builtin.SyncConfirmDialog;
import school.hei.patrimoine.visualisation.swing.ihm.google.component.comment.LocalCommentManager;
import school.hei.patrimoine.visualisation.swing.ihm.google.modele.GoogleLinkListDownloader;
import school.hei.patrimoine.visualisation.swing.ihm.google.pages.*;

public class PatriLangViewer extends App {
  public PatriLangViewer() {
    super(
        "patrilang-app",
        "Patrimoine",
        getDefaultToolkit().getScreenSize().width,
        getDefaultToolkit().getScreenSize().height);

    addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowClosing(WindowEvent e) {
            LocalCommentManager localCommentManager = LocalCommentManager.getInstance();

            List<File> plannedFiles = getStagedPlannedFiles();
            List<File> doneFiles = getStagedDoneFiles();
            boolean hasPendingComments = localCommentManager.hasAnyPendingComments();

            if (plannedFiles.isEmpty() && doneFiles.isEmpty() && !hasPendingComments) {
              dispose();
              return;
            }

            boolean confirmed = SyncConfirmDialog.forQuit();
            if (confirmed) {
              dispose();
            }
          }
        });
  }

  @Override
  protected String defaultPageName() {
    // -Doffline=true skips the Google sign-in flow and opens the local file browser
    // directly; sync-only features (comments, Drive upload) are simply unavailable.
    return Boolean.getBoolean("offline") ? PatriLangFilesPage.PAGE_NAME : LoginPage.PAGE_NAME;
  }

  @Override
  protected Set<Page> pages() {
    return Set.of(
        new LoginPage(),
        new SubmitLinkPage(),
        new LinkValidityPage(),
        new PatriLangFilesPage(),
        new RecoupementPage());
  }

  public static void main(String[] args) {
    App.setup();
    FlatLightLaf.setup();
    GoogleApiUtilities.setup();
    // GoogleLinkListDownloader.setup() wipes download/ and staged/ to make room for a
    // fresh Drive re-sync - destructive and pointless with no sync to follow it, so skip
    // it in offline mode (no local recovery otherwise: this is the only call site).
    if (!Boolean.getBoolean("offline")) {
      GoogleLinkListDownloader.setup();
    }
    invokeLater(PatriLangViewer::new);
  }
}
