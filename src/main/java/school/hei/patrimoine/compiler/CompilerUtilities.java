package school.hei.patrimoine.compiler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import school.hei.patrimoine.google.GoogleApiUtilities;

@Slf4j
public class CompilerUtilities {
  private CompilerUtilities() {}

  public static final String APP_HOME = GoogleApiUtilities.getApplicationHomeDirectory();
  public static final String COMPILE_DIR_NAME = APP_HOME + "/compile";
  public static final String DOWNLOADS_DIRECTORY_PATH = APP_HOME + "/drive";
  private static final String PATRIMOINE_JAR_NAME = "patrimoine-1.0-SNAPSHOT.jar";

  public static final String PATRIMOINE_JAR_PATH =
      String.valueOf(Path.of(DOWNLOADS_DIRECTORY_PATH).resolve(PATRIMOINE_JAR_NAME));

  static {
    resetIfExist(COMPILE_DIR_NAME);
  }

  @SuppressWarnings("all")
  public static void resetIfExist(String directoryPath) {
    Path path = Paths.get(directoryPath);
    File directory = path.toFile();

    try {
      if (directory.exists()) {
        Files.walk(path).map(Path::toFile).forEach(File::delete);
      }
      Files.createDirectories(path);
    } catch (IOException e) {
      throw new RuntimeException("Directory reset showError : " + directoryPath, e);
    }
  }
}
