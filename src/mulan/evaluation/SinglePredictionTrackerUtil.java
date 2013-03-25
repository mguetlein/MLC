package mulan.evaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mulan.evaluation.SinglePredictionTracker.AllLabelPredictions;
import mulan.evaluation.SinglePredictionTracker.Predictions;
import util.ArrayUtil;
import util.FileUtil;
import util.FileUtil.CSVFile;
import util.StringUtil;

public class SinglePredictionTrackerUtil
{
	public static String attachToCsv(SinglePredictionTracker tracker)
	{
		CSVFile csvNew = FileUtil.readCSV("tmp/" + tracker.datasetName + ".csv").merge(toCsv(tracker));
		String outfile = "tmp/" + tracker.datasetName + "_" + tracker.experimentName + "_missclassified.csv";
		System.out.println("writing missclassifications to " + outfile);
		FileUtil.writeCSV(outfile, csvNew, false);
		return outfile;
	}

	public static CSVFile toCsv(SinglePredictionTracker tracker)
	{
		CSVFile csv = new CSVFile();
		List<String> h = new ArrayList<String>();
		for (int l = 0; l < tracker.data.getNumLabels(); l++)
		{
			String name = tracker.data.getDataSet().attribute(tracker.data.getLabelIndices()[l]).name();
			h.add(name + "_missclassified");
			//			h.add(name + "_missclassfied_as_1");
			//			h.add(name + "_missclassfied_as_0");
		}
		h.add("all_endpoints_missclassified");
		h.add("all_endpoints_count");
		h.add("all_endpoints_missclassified_total");
		csv.content.add(ArrayUtil.toArray(String.class, h));

		for (int i = 0; i < tracker.data.getNumInstances(); i++)
		{
			List<String> v = new ArrayList<String>();

			for (int l = 0; l < tracker.data.getNumLabels(); l++)
			{
				Predictions p = tracker.labelPredictions.get(i + "_" + l);
				if (p != null)
				{
					v.add(StringUtil.formatDouble(p.getMissclassified(), Locale.US));
					//					v.add(p.getMissclassifiedAsTrue());
					//					v.add(p.getMissclassifiedAsFalse());
				}
				else
				{
					v.add(null);
					//					v.add(null);
					//					v.add(null);
				}
			}
			AllLabelPredictions p = tracker.allLabelPredictions.get(i);
			v.add(StringUtil.formatDouble(p.getMissclassified(), Locale.US));
			v.add(p.getNumLabels());
			v.add(p.getMissclassifiedTotal());
			csv.content.add(ArrayUtil.toArray(String.class, v));
		}
		return csv;
	}

	public static void print(SinglePredictionTracker tracker)
	{
		System.out.println();
		System.out.print(": ");
		for (int l = 0; l < tracker.data.getNumLabels(); l++)
			System.out.print(tracker.data.getDataSet().attribute(tracker.data.getLabelIndices()[l]).name() + " ");
		System.out.println();
		for (int i = 0; i < tracker.data.getNumInstances(); i++)
		{
			System.out.print(StringUtil.concatWhitespace(i + ": ", 5));

			for (int l = 0; l < tracker.data.getNumLabels(); l++)
			{
				Predictions p = tracker.labelPredictions.get(i + "_" + l);
				String ratio = p == null ? "" : StringUtil.formatDouble(1 - (p.numCorrect / (double) p.numTotal));
				System.out.print(StringUtil.concatWhitespace(ratio, 5));
				//				String numTotal = p == null ? "" : p.numTotal + "";
				//				String numCorrect = p == null ? "" : p.numCorrect + "";
				//				System.out.print(StringUtil.concatWhitespace(numCorrect, 2) + "/"
				//						+ StringUtil.concatWhitespace(numTotal, 2) + " ");
			}
			System.out.println();
		}
		System.out.println();
	}
}
