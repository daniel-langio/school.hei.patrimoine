package school.hei.patrimoine.cli;

import static java.util.Arrays.asList;
import static school.hei.patrimoine.patrilang.PatriLangTranspiler.transpileToutCas;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import school.hei.patrimoine.cas.Cas;
import school.hei.patrimoine.cas.CasSet;
import school.hei.patrimoine.cas.ToutCas;
import school.hei.patrimoine.modele.evolution.EvolutionPatrimoine;
import school.hei.patrimoine.patrilang.validator.PatriLangValidator;
import school.hei.patrimoine.visualisation.xchart.GrapheConf;
import school.hei.patrimoine.visualisation.xchart.GrapheurEvolutionPatrimoine;

/**
 * Headless counterpart to the Swing/Vaadin UI: lints a PatriLang file (Spécifier) and runs the same
 * Projeter/Recouper/Alerter operations the UI exposes, for scripting and CI use.
 */
public class PatrimoineCli {

  public static void main(String[] args) {
    if (args.length < 2) {
      printUsageAndExit();
      return;
    }
    var command = args[0];
    var file = args[1];
    var rest = args.length > 2 ? asList(args).subList(2, args.length) : List.<String>of();

    try {
      switch (command) {
        case "lint" -> lint(file);
        case "projeter" -> projeter(file, rest);
        case "recouper" -> recouper(file, rest);
        case "alerter" -> alerter(file, rest);
        default -> printUsageAndExit();
      }
    } catch (RuntimeException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }

  private static void lint(String file) {
    new PatriLangValidator().accept(file);
    System.out.println("OK: " + file + " est valide.");
  }

  private static void projeter(String file, List<String> rest) {
    var opts = Options.parse(rest);
    var ep = evolution(file, opts);
    var avecAgregat = opts.tresorerie || opts.immobilisations || opts.obligations;
    var grapheConf =
        GrapheConf.builder()
            .avecTitre(true)
            .avecAgregat(avecAgregat)
            .avecTresorerie(opts.tresorerie)
            .avecImmobilisations(opts.immobilisations)
            .avecObligations(opts.obligations)
            .build();
    new GrapheurEvolutionPatrimoine().apply(ep, grapheConf);
  }

  private static void recouper(String file, List<String> rest) {
    var ep = evolution(file, Options.parse(rest));
    ep.getFluxJournaliers().stream()
        .sorted(java.util.Comparator.comparing(f -> f.date()))
        .forEach(System.out::println);
  }

  private static void alerter(String file, List<String> rest) {
    var ep = evolution(file, Options.parse(rest));
    ep.getFluxJournaliersImpossibles().stream()
        .sorted(java.util.Comparator.comparing(f -> f.date()))
        .forEach(System.out::println);
  }

  private static EvolutionPatrimoine evolution(String file, Options opts) {
    CasSet casSet = transpileToutCas(file);
    Cas cas = opts.casNom == null ? aggregate(casSet) : find(casSet, opts.casNom);
    var patrimoine = cas.patrimoine();
    var debut = opts.debut != null ? opts.debut : cas.getAjd();
    var fin = opts.fin != null ? opts.fin : cas.getFinSimulation();
    return new EvolutionPatrimoine(patrimoine.nom(), patrimoine, debut, fin);
  }

  private static Cas aggregate(CasSet casSet) {
    // casSet.set() is unordered: pick the widest span across every Cas rather than an
    // arbitrary one, whose own (ajd, finSimulation) may not even be ordered correctly
    // for use as the aggregate's range.
    var ajd = casSet.set().stream().map(Cas::getAjd).min(Comparator.naturalOrder()).orElseThrow();
    var fin =
        casSet.set().stream()
            .map(Cas::getFinSimulation)
            .max(Comparator.naturalOrder())
            .orElseThrow();
    return new ToutCas(ajd, fin, casSet);
  }

  private static Cas find(CasSet casSet, String nom) {
    return casSet.set().stream()
        .filter(c -> c.patrimoine().nom().equals(nom))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Cas introuvable: " + nom));
  }

  private static void printUsageAndExit() {
    System.err.println(
        "Usage: PatrimoineCli <lint|projeter|recouper|alerter> <fichier.patri.md> "
            + "[--cas NOM] [--debut yyyy-MM-dd] [--fin yyyy-MM-dd] "
            + "[--tresorerie] [--immobilisations] [--obligations]");
    System.exit(2);
  }

  private record Options(
      String casNom,
      LocalDate debut,
      LocalDate fin,
      boolean tresorerie,
      boolean immobilisations,
      boolean obligations) {

    static Options parse(List<String> args) {
      String casNom = null;
      LocalDate debut = null;
      LocalDate fin = null;
      boolean tresorerie = false;
      boolean immobilisations = false;
      boolean obligations = false;
      for (int i = 0; i < args.size(); i++) {
        switch (args.get(i)) {
          case "--cas" -> casNom = args.get(++i);
          case "--debut" -> debut = LocalDate.parse(args.get(++i));
          case "--fin" -> fin = LocalDate.parse(args.get(++i));
          case "--tresorerie" -> tresorerie = true;
          case "--immobilisations" -> immobilisations = true;
          case "--obligations" -> obligations = true;
          default -> throw new IllegalArgumentException("Option inconnue: " + args.get(i));
        }
      }
      return new Options(casNom, debut, fin, tresorerie, immobilisations, obligations);
    }
  }
}
