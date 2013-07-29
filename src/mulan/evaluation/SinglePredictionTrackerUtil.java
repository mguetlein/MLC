package mulan.evaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mulan.evaluation.SinglePredictionTracker.AllLabelPredictions;
import mulan.evaluation.SinglePredictionTracker.Predictions;
import util.ArrayUtil;
import util.FileUtil;
import util.FileUtil.CSVFile;
import util.FileUtil.RowRemove;
import util.StringUtil;

public class SinglePredictionTrackerUtil
{
	public static void filterCsv(SinglePredictionTracker tracker, String endpoint)
	{
		String endpoints[] = new String[tracker.getData().getNumLabels() - 1];
		int count = 0;
		for (int i = 0; i < tracker.getData().getNumLabels(); i++)
		{
			String name = tracker.data.getDataSet().attribute(tracker.data.getLabelIndices()[i]).name();
			if (!name.equals(endpoint))
				endpoints[count++] = name;
		}
		filterCsv(tracker.datasetName, tracker.experimentName, endpoints, endpoint);
	}

	public static void filterCsv(String datasetName, String experimentName, String[] endpoints, String endpoint)
	{
		CSVFile csv = FileUtil.readCSV(Settings.missclassifiedFile(datasetName, experimentName));
		final int endpointIndex = csv.getColumnIndex(endpoint);
		csv = csv.removeRow(new RowRemove()
		{
			@Override
			public boolean remove(int rowIndex, String[] row)
			{
				return row[endpointIndex] == null;
			}
		});
		for (String e : endpoints)
			csv = csv.exclude(e, e + "_real", e + "_classified", e + "_missclassified");
		String outfile = Settings.missclassifiedFile(datasetName, experimentName, endpoint);
		System.out.println("writing missclassifications for label " + endpoint + " to " + outfile);
		FileUtil.writeCSV(outfile, csv, false);
	}

	public static String attachToCsv(SinglePredictionTracker tracker)
	{
		CSVFile csvNew = FileUtil.readCSV(Settings.csvFile(tracker.datasetName)).merge(toCsv(tracker));
		String outfile = Settings.missclassifiedFile(tracker.datasetName, tracker.experimentName);
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
			h.add(name + "_classified");
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
					v.add(StringUtil.formatDouble(p.getClassified(), Locale.US));
					v.add(StringUtil.formatDouble(p.getMissclassified(), Locale.US));
					//					v.add(p.getMissclassifiedAsTrue());
					//					v.add(p.getMissclassifiedAsFalse());
				}
				else
				{
					v.add(null);
					v.add(null);
					//					v.add(null);
					//					v.add(null);
				}
			}
			AllLabelPredictions p = tracker.allLabelPredictions.get(i);
			if (p != null)
			{
				v.add(StringUtil.formatDouble(p.getMissclassified(), Locale.US));
				v.add(p.getNumLabels());
				v.add(p.getMissclassifiedTotal());
			}
			else
			{
				v.add(null);
				v.add(null);
				v.add(null);
			}
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
