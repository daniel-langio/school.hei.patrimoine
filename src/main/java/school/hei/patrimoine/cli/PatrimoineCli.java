package school.hei.patrimoine.cli;

import static java.util.Arrays.asList;
import static school.hei.patrimoine.patrilang.PatriLangTranspiler.transpileToutCas;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import school.hei.patrimoine.cas.Cas;
import school.hei.patrimoine.cas.CasSet;
import school.hei.patrimoine.cas.ToutCas;
import school.hei.patrimoine.modele.Argent;
import school.hei.patrimoine.modele.evolution.EvolutionPatrimoine;
import school.hei.patrimoine.modele.possession.TypeAgregat;
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
        case "valeur" -> valeur(file, rest);
        default -> printUsageAndExit();
      }
    } catch (RuntimeException e) {
      // ParseCancellationException (syntax errors) already carries "ligne X, colonne Y
      // dans le fichier ..." from PatrilangErrorListener; other RuntimeExceptions carry
      // whatever message the semantic validation raised (e.g. missing variable), with no
      // line/column since that's not plumbed through the visitors.
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }

  private static void lint(String file) {
    new PatriLangValidator().accept(file);
    System.out.println("OK: " + file + " est valide.");
  }

  private static void valeur(String file, List<String> rest) {
    var opts = Options.parse(rest);
    var casSet = transpileToutCas(file);
    var cas = opts.casNom == null ? aggregate(casSet) : find(casSet, opts.casNom);
    var date = opts.debut != null ? opts.debut : LocalDate.now();
    var patrimoine = cas.patrimoine().projectionFuture(date);
    var symbole = patrimoine.getDevise().symbole();

    if (opts.compte != null) {
      var possession =
          patrimoine.getPossessions().stream()
              .filter(p -> p.nom().equals(opts.compte))
              .findFirst()
              .orElseThrow(
                  () -> new IllegalArgumentException("Compte introuvable: " + opts.compte));
      System.out.printf(
          "%s au %s: %s%s%n", opts.compte, date, montant(possession.valeurComptable()), symbole);
      return;
    }

    var total = montant(patrimoine.getValeurComptable());
    System.out.printf("Patrimoine (%s) au %s: %s%s%n", patrimoine.nom(), date, total, symbole);
    for (var type :
        List.of(TypeAgregat.TRESORERIE, TypeAgregat.IMMOBILISATION, TypeAgregat.OBLIGATION)) {
      var sousTotal =
          patrimoine.getPossessions().stream()
              .filter(p -> p.typeAgregat() == type)
              .mapToDouble(p -> montant(p.valeurComptable()))
              .sum();
      var pct = total == 0 ? 0 : (sousTotal / total) * 100;
      System.out.printf("  %-15s %12.0f%s (%.1f%%)%n", type, sousTotal, symbole, pct);
    }
  }

  private static double montant(Argent a) {
    // Argent exposes no numeric getter outside its package; ppMontant() is its own
    // double-to-string formatting, parsed back rather than duplicating that logic.
    return Double.parseDouble(a.ppMontant());
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
        "Usage: PatrimoineCli <lint|projeter|recouper|alerter|valeur> <fichier.patri.md> "
            + "[--cas NOM] [--debut yyyy-MM-dd] [--fin yyyy-MM-dd] [--compte NOM] "
            + "[--tresorerie] [--immobilisations] [--obligations]\n"
            + "  valeur: --debut is the query date (default: today); with --compte, prints just"
            + " that account's balance, else total + %% breakdown by type.");
    System.exit(2);
  }

  private record Options(
      String casNom,
      LocalDate debut,
      LocalDate fin,
      String compte,
      boolean tresorerie,
      boolean immobilisations,
      boolean obligations) {

    static Options parse(List<String> args) {
      String casNom = null;
      LocalDate debut = null;
      LocalDate fin = null;
      String compte = null;
      boolean tresorerie = false;
      boolean immobilisations = false;
      boolean obligations = false;
      for (int i = 0; i < args.size(); i++) {
        switch (args.get(i)) {
          case "--cas" -> casNom = args.get(++i);
          case "--debut", "--date" -> debut = LocalDate.parse(args.get(++i));
          case "--fin" -> fin = LocalDate.parse(args.get(++i));
          case "--compte" -> compte = args.get(++i);
          case "--tresorerie" -> tresorerie = true;
          case "--immobilisations" -> immobilisations = true;
          case "--obligations" -> obligations = true;
          default -> throw new IllegalArgumentException("Option inconnue: " + args.get(i));
        }
      }
      return new Options(casNom, debut, fin, compte, tresorerie, immobilisations, obligations);
    }
  }
}
