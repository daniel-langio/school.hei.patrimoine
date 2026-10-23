package school.hei.patrimoine.cli;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PatrimoineCliTest {
  private static final String RESOURCE_PATH = "/famille_rakoto_cas/FamilleRakoto.patri.md";
  private static final Path RESOURCE_FILE_PATH;

  static {
    try {
      var uri = requireNonNull(PatrimoineCliTest.class.getResource(RESOURCE_PATH)).toURI();
      RESOURCE_FILE_PATH = Paths.get(uri);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private final PrintStream systemOut = System.out;
  private final ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();

  @BeforeEach
  void captureStdout() {
    System.setOut(new PrintStream(capturedOut));
  }

  @AfterEach
  void restoreStdout() {
    System.setOut(systemOut);
  }

  @Test
  void lint_accepts_a_valid_file() {
    assertDoesNotThrow(
        () -> PatrimoineCli.main(new String[] {"lint", RESOURCE_FILE_PATH.toString()}));
    assertTrue(capturedOut.toString().startsWith("OK:"));
  }

  // FamilleRakoto.patri.md's own two sub-cas each compute a finSimulation before their own
  // ajd (LocationMaison deliberately models a "retard sur les actions" scenario, ZetyPersonnel
  // likewise shifts its window the other way) - that's fine for lint/validate, but not a window
  // EvolutionPatrimoine can iterate. Pass the file's own global Dates (ajd=2025-01-10,
  // finSimulation=2025-04-10) explicitly, the way a UI user would pick a sensible range by hand.
  private static final String DEBUT = "2025-01-10";
  private static final String FIN = "2025-04-10";

  @Test
  void projeter_generates_a_chart_for_the_aggregate_patrimoine() {
    assertDoesNotThrow(
        () ->
            PatrimoineCli.main(
                new String[] {
                  "projeter", RESOURCE_FILE_PATH.toString(), "--debut", DEBUT, "--fin", FIN
                }));
    assertTrue(capturedOut.toString().contains("Image générée"));
  }

  @Test
  void recouper_lists_flux_journaliers_for_a_named_cas() {
    assertDoesNotThrow(
        () ->
            PatrimoineCli.main(
                new String[] {
                  "recouper",
                  RESOURCE_FILE_PATH.toString(),
                  "--cas",
                  "LocationMaison",
                  "--debut",
                  DEBUT,
                  "--fin",
                  FIN
                }));
  }

  @Test
  void alerter_lists_flux_impossibles_without_failing() {
    assertDoesNotThrow(
        () ->
            PatrimoineCli.main(
                new String[] {
                  "alerter", RESOURCE_FILE_PATH.toString(), "--debut", DEBUT, "--fin", FIN
                }));
  }

  @Test
  void valeur_prints_total_and_breakdown_by_type() {
    assertDoesNotThrow(
        () ->
            PatrimoineCli.main(
                new String[] {"valeur", RESOURCE_FILE_PATH.toString(), "--date", DEBUT}));
    assertTrue(capturedOut.toString().contains("Patrimoine"));
    assertTrue(capturedOut.toString().contains("TRESORERIE"));
  }

  @Test
  void valeur_prints_a_single_account_balance_with_compte_option() {
    assertDoesNotThrow(
        () ->
            PatrimoineCli.main(
                new String[] {
                  "valeur",
                  RESOURCE_FILE_PATH.toString(),
                  "--cas",
                  "LocationMaison",
                  "--date",
                  DEBUT,
                  "--compte",
                  "loyerMaison"
                }));
    assertTrue(capturedOut.toString().contains("loyerMaison"));
  }
}
