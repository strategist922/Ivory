package ivory.regression.basic;

import static ivory.regression.RegressionUtils.loadScoresIntoMap;
import static org.junit.Assert.assertEquals;
import ivory.core.eval.Qrels;
import ivory.core.eval.RankedListEvaluator;
import ivory.smrf.retrieval.Accumulator;
import ivory.smrf.retrieval.BatchQueryRunner;

import java.util.Map;

import junit.framework.JUnit4TestAdapter;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.log4j.Logger;
import org.junit.Test;

import com.google.common.collect.Maps;

import edu.umd.cloud9.collection.DocnoMapping;

public class Robust04_WSD {
  private static final Logger LOG = Logger.getLogger(Robust04_WSD.class);

  private static String[] sDir_WSD_SD_RawAP = new String[] { "601", "0.4951", "602", "0.3076",
          "603", "0.2561", "604", "0.8518", "605", "0.0680", "606", "0.5995", "607", "0.2564",
          "608", "0.0766", "609", "0.3063", "610", "0.0294", "611", "0.2613", "612", "0.4517",
          "613", "0.2492", "614", "0.2043", "615", "0.0634", "616", "0.7377", "617", "0.2553",
          "618", "0.2091", "619", "0.5563", "620", "0.0736", "621", "0.3446", "622", "0.1210",
          "623", "0.2954", "624", "0.2713", "625", "0.0251", "626", "0.2027", "627", "0.0213",
          "628", "0.2213", "629", "0.2001", "630", "0.7169", "631", "0.1831", "632", "0.2441",
          "633", "0.4995", "634", "0.7412", "635", "0.5952", "636", "0.1971", "637", "0.5269",
          "638", "0.0624", "639", "0.1589", "640", "0.3741", "641", "0.3269", "642", "0.1632",
          "643", "0.5512", "644", "0.2277", "645", "0.5907", "646", "0.3490", "647", "0.2457",
          "648", "0.3688", "649", "0.6840", "650", "0.0845", "651", "0.0491", "652", "0.3279",
          "653", "0.5957", "654", "0.4214", "655", "0.0011", "656", "0.5310", "657", "0.4787",
          "658", "0.1321", "659", "0.3972", "660", "0.6448", "661", "0.6152", "662", "0.6510",
          "663", "0.4894", "664", "0.5061", "665", "0.1623", "666", "0.0186", "667", "0.3714",
          "668", "0.3651", "669", "0.1597", "670", "0.1270", "671", "0.4059", "672", "0.0000",
          "673", "0.2950", "674", "0.1371", "675", "0.3248", "676", "0.2861", "677", "0.8840",
          "678", "0.2190", "679", "0.8556", "680", "0.0926", "681", "0.5499", "682", "0.2641",
          "683", "0.0242", "684", "0.1308", "685", "0.2044", "686", "0.2071", "687", "0.2285",
          "688", "0.1157", "689", "0.0149", "690", "0.0080", "691", "0.3478", "692", "0.4843",
          "693", "0.3228", "694", "0.4819", "695", "0.2510", "696", "0.2824", "697", "0.1709",
          "698", "0.4457", "699", "0.5590", "700", "0.5904" };

  private static String[] sDir_WSD_SD_RawP10 = new String[] { "601", "0.3000", "602", "0.3000",
          "603", "0.2000", "604", "0.6000", "605", "0.2000", "606", "0.5000", "607", "0.3000",
          "608", "0.1000", "609", "0.6000", "610", "0.0000", "611", "0.5000", "612", "0.6000",
          "613", "0.5000", "614", "0.2000", "615", "0.0000", "616", "0.9000", "617", "0.5000",
          "618", "0.3000", "619", "0.6000", "620", "0.1000", "621", "0.8000", "622", "0.2000",
          "623", "0.6000", "624", "0.4000", "625", "0.1000", "626", "0.3000", "627", "0.1000",
          "628", "0.4000", "629", "0.4000", "630", "0.3000", "631", "0.1000", "632", "0.7000",
          "633", "1.0000", "634", "0.8000", "635", "0.8000", "636", "0.3000", "637", "0.8000",
          "638", "0.2000", "639", "0.3000", "640", "0.6000", "641", "0.6000", "642", "0.2000",
          "643", "0.4000", "644", "0.2000", "645", "0.9000", "646", "0.4000", "647", "0.7000",
          "648", "0.7000", "649", "1.0000", "650", "0.2000", "651", "0.1000", "652", "0.7000",
          "653", "0.6000", "654", "1.0000", "655", "0.0000", "656", "0.7000", "657", "0.6000",
          "658", "0.4000", "659", "0.5000", "660", "0.9000", "661", "0.9000", "662", "0.8000",
          "663", "0.6000", "664", "0.6000", "665", "0.3000", "666", "0.0000", "667", "0.8000",
          "668", "0.5000", "669", "0.1000", "670", "0.3000", "671", "0.5000", "672", "0.0000",
          "673", "0.6000", "674", "0.1000", "675", "0.6000", "676", "0.2000", "677", "0.7000",
          "678", "0.4000", "679", "0.6000", "680", "0.3000", "681", "0.8000", "682", "0.6000",
          "683", "0.4000", "684", "0.3000", "685", "0.2000", "686", "0.3000", "687", "0.8000",
          "688", "0.5000", "689", "0.0000", "690", "0.0000", "691", "0.5000", "692", "0.7000",
          "693", "0.6000", "694", "0.5000", "695", "0.8000", "696", "0.6000", "697", "0.3000",
          "698", "0.3000", "699", "0.7000", "700", "0.7000" };

  private static String[] sDir_WSD_FD_RawAP = new String[] { "601", "0.6823", "602", "0.3070",
          "603", "0.2825", "604", "0.8538", "605", "0.0615", "606", "0.6033", "607", "0.2294",
          "608", "0.1096", "609", "0.3282", "610", "0.0202", "611", "0.2402", "612", "0.4517",
          "613", "0.2521", "614", "0.2287", "615", "0.0512", "616", "0.7377", "617", "0.2343",
          "618", "0.1937", "619", "0.5567", "620", "0.0811", "621", "0.4508", "622", "0.1210",
          "623", "0.2525", "624", "0.2855", "625", "0.0254", "626", "0.2027", "627", "0.0154",
          "628", "0.2213", "629", "0.1868", "630", "0.7885", "631", "0.2742", "632", "0.2205",
          "633", "0.4995", "634", "0.7412", "635", "0.5750", "636", "0.1890", "637", "0.5455",
          "638", "0.0624", "639", "0.1397", "640", "0.3584", "641", "0.3205", "642", "0.1659",
          "643", "0.3963", "644", "0.2612", "645", "0.5907", "646", "0.3579", "647", "0.2457",
          "648", "0.2937", "649", "0.6840", "650", "0.0798", "651", "0.0491", "652", "0.3278",
          "653", "0.5802", "654", "0.4214", "655", "0.0012", "656", "0.5486", "657", "0.4694",
          "658", "0.1321", "659", "0.4778", "660", "0.6404", "661", "0.6217", "662", "0.6510",
          "663", "0.4885", "664", "0.6416", "665", "0.1482", "666", "0.0168", "667", "0.3751",
          "668", "0.3651", "669", "0.1597", "670", "0.1270", "671", "0.3937", "672", "0.0000",
          "673", "0.2920", "674", "0.1371", "675", "0.2878", "676", "0.2861", "677", "0.8938",
          "678", "0.1951", "679", "0.8000", "680", "0.1028", "681", "0.5690", "682", "0.2597",
          "683", "0.0242", "684", "0.1119", "685", "0.2136", "686", "0.2376", "687", "0.3285",
          "688", "0.1060", "689", "0.0149", "690", "0.0064", "691", "0.3467", "692", "0.4899",
          "693", "0.3526", "694", "0.4819", "695", "0.2101", "696", "0.2983", "697", "0.1789",
          "698", "0.4651", "699", "0.5590", "700", "0.5904" };

  private static String[] sDir_WSD_FD_RawP10 = new String[] { "601", "0.4000", "602", "0.2000",
          "603", "0.3000", "604", "0.6000", "605", "0.3000", "606", "0.5000", "607", "0.4000",
          "608", "0.1000", "609", "0.7000", "610", "0.0000", "611", "0.4000", "612", "0.6000",
          "613", "0.5000", "614", "0.3000", "615", "0.0000", "616", "0.9000", "617", "0.5000",
          "618", "0.2000", "619", "0.7000", "620", "0.1000", "621", "0.8000", "622", "0.2000",
          "623", "0.5000", "624", "0.5000", "625", "0.1000", "626", "0.3000", "627", "0.1000",
          "628", "0.4000", "629", "0.4000", "630", "0.3000", "631", "0.8000", "632", "0.7000",
          "633", "1.0000", "634", "0.8000", "635", "0.8000", "636", "0.3000", "637", "0.7000",
          "638", "0.2000", "639", "0.2000", "640", "0.6000", "641", "0.7000", "642", "0.3000",
          "643", "0.4000", "644", "0.3000", "645", "0.9000", "646", "0.4000", "647", "0.7000",
          "648", "0.6000", "649", "1.0000", "650", "0.0000", "651", "0.1000", "652", "0.7000",
          "653", "0.6000", "654", "1.0000", "655", "0.0000", "656", "0.7000", "657", "0.6000",
          "658", "0.4000", "659", "0.5000", "660", "0.9000", "661", "0.8000", "662", "0.8000",
          "663", "0.6000", "664", "0.6000", "665", "0.3000", "666", "0.0000", "667", "0.8000",
          "668", "0.5000", "669", "0.1000", "670", "0.3000", "671", "0.4000", "672", "0.0000",
          "673", "0.6000", "674", "0.1000", "675", "0.5000", "676", "0.2000", "677", "0.7000",
          "678", "0.3000", "679", "0.6000", "680", "0.4000", "681", "0.8000", "682", "0.5000",
          "683", "0.4000", "684", "0.3000", "685", "0.3000", "686", "0.3000", "687", "0.7000",
          "688", "0.5000", "689", "0.0000", "690", "0.0000", "691", "0.5000", "692", "0.7000",
          "693", "0.6000", "694", "0.5000", "695", "0.5000", "696", "0.7000", "697", "0.3000",
          "698", "0.4000", "699", "0.7000", "700", "0.7000" };

  private static Qrels sQrels;
  private static DocnoMapping sMapping;

  @Test
  public void runRegression() throws Exception {
    Map<String, Map<String, Float>> AllModelsAPScores = Maps.newHashMap();
    AllModelsAPScores.put("robust04-dir-wsd-sd", loadScoresIntoMap(sDir_WSD_SD_RawAP));
    AllModelsAPScores.put("robust04-dir-wsd-fd", loadScoresIntoMap(sDir_WSD_FD_RawAP));

    Map<String, Map<String, Float>> AllModelsP10Scores = Maps.newHashMap();
    AllModelsP10Scores.put("robust04-dir-wsd-sd", loadScoresIntoMap(sDir_WSD_SD_RawP10));
    AllModelsP10Scores.put("robust04-dir-wsd-fd", loadScoresIntoMap(sDir_WSD_FD_RawP10));

    sQrels = new Qrels("data/trec/qrels.robust04.noCRFR.txt");

    String[] params = new String[] {
            "data/trec/run.robust04.wsd.xml",
            "data/trec/queries.robust04.xml" };

    FileSystem fs = FileSystem.getLocal(new Configuration());

    BatchQueryRunner qr = new BatchQueryRunner(params, fs);

    long start = System.currentTimeMillis();
    qr.runQueries();
    long end = System.currentTimeMillis();

    LOG.info("Total query time: " + (end - start) + "ms");

    sMapping = qr.getDocnoMapping();

    for (String model : qr.getModels()) {
      LOG.info("Verifying results of model \"" + model + "\"");
      Map<String, Accumulator[]> results = qr.getResults(model);
      verifyResults(model, results, AllModelsAPScores.get(model),
          AllModelsP10Scores.get(model));
      LOG.info("Done!");
    }

  }

  private static void verifyResults(String model, Map<String, Accumulator[]> results,
      Map<String, Float> apScores, Map<String, Float> p10Scores) {
    float apSum = 0, p10Sum = 0;
    for (String qid : results.keySet()) {
      float ap = (float) RankedListEvaluator.computeAP(results.get(qid), sMapping,
          sQrels.getReldocsForQid(qid));

      float p10 = (float) RankedListEvaluator.computePN(10, results.get(qid), sMapping,
          sQrels.getReldocsForQid(qid));

      apSum += ap;
      p10Sum += p10;

      LOG.info("verifying qid " + qid + " for model " + model);
      assertEquals(apScores.get(qid), ap, 10e-5);
      assertEquals(p10Scores.get(qid), p10, 10e-5);
    }

    // one topic didn't contain qrels, so trec_eval only picked up 99 topics
    float MAP = (float) RankedListEvaluator.roundTo4SigFigs(apSum / 99f);
    float P10Avg = (float) RankedListEvaluator.roundTo4SigFigs(p10Sum / 99f);

    if (model.equals("robust04-dir-wsd-sd")) {
      assertEquals(0.3246, MAP, 10e-5);
      assertEquals(0.4626, P10Avg, 10e-5);
    } else if (model.equals("robust04-dir-wsd-fd")) {
      assertEquals(0.3286, MAP, 10e-5);
      assertEquals(0.4667, P10Avg, 10e-5);
    }
  }

  public static junit.framework.Test suite() {
    return new JUnit4TestAdapter(Robust04_WSD.class);
  }
}
