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

  @Test
  void projeter_generates_a_chart_for_the_aggregate_patrimoine() {
    assertDoesNotThrow(
        () -> PatrimoineCli.main(new String[] {"projeter", RESOURCE_FILE_PATH.toString()}));
    assertTrue(capturedOut.toString().contains("Image générée"));
  }

  @Test
  void recouper_lists_flux_journaliers_for_a_named_cas() {
    assertDoesNotThrow(
        () ->
            PatrimoineCli.main(
                new String[] {
                  "recouper", RESOURCE_FILE_PATH.toString(), "--cas", "LocationMaison"
                }));
  }

  @Test
  void alerter_lists_flux_impossibles_without_failing() {
    assertDoesNotThrow(
        () -> PatrimoineCli.main(new String[] {"alerter", RESOURCE_FILE_PATH.toString()}));
  }
}
