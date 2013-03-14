import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;

import mulan.data.MultiLabelInstances;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.title.Title;

import util.ArrayUtil;
import util.IntegerUtil;
import util.ListUtil;
import util.StringLineAdder;
import util.StringUtil;
import util.SwingUtil;
import weka.core.Attribute;
import datamining.ResultSet;
import freechart.BarPlotPanel;
import freechart.HistogramPanel;
import freechart.StackedBarPlot;

public class MLCData
{
	public static class DatasetInfo
	{
		MultiLabelInstances dataset;
		HashMap<String, Integer> distinct;
		ArrayList<String> zeroOnes;
		private int[] numMissing;

		double[] histData;
		String[] labels;
		String[] labelNames;

		List<Double> histData2;
		HashMap<String, Integer> histData2b;

		String endpointFile;
		String featureFile;
		int numEndpoints;
		int numMissingAllowed;

		int[] ones_per_label;
		int[] zeros_per_label;
		int[] missings_per_label;

		public DatasetInfo(MultiLabelInstances dataset)
		{
			String s[] = dataset.getDataSet().relationName().split("#");
			for (String string : s)
			{
				if (string.startsWith("endpoint-file:"))
					endpointFile = string.substring("endpoint-file:".length());
				if (string.startsWith("feature-file:"))
					featureFile = string.substring("feature-file:".length());
				if (string.startsWith("num-endpoints:"))
					numEndpoints = IntegerUtil.parseInteger(string.substring("num-endpoints:".length()));
				if (string.startsWith("num-missing-allowed:"))
					numMissingAllowed = IntegerUtil.parseInteger(string.substring("num-missing-allowed:".length()));
			}
			if (numEndpoints != dataset.getNumLabels())
				throw new IllegalStateException();

			this.dataset = dataset;

			zeroOnes = new ArrayList<String>();

			ones_per_label = new int[dataset.getNumLabels()];
			zeros_per_label = new int[dataset.getNumLabels()];
			missings_per_label = new int[dataset.getNumLabels()];
			labelNames = new String[dataset.getNumLabels()];

			for (int j = 0; j < dataset.getNumLabels(); j++)
			{
				Attribute labelAttr = dataset.getDataSet().attribute(dataset.getLabelIndices()[j]);

				int one = 0;
				int zero = 0;
				int missing = 0;
				for (int i = 0; i < dataset.getNumInstances(); i++)
				{
					if (dataset.getDataSet().get(i).isMissing(labelAttr))
						missing++;
					else if (dataset.getDataSet().get(i).stringValue(labelAttr).equals("1"))
						one++;
					else
						zero++;
				}
				zeroOnes.add("(" + zero + "/" + one + " missing:" + missing + ")");

				labelNames[j] = labelAttr.name();
				ones_per_label[j] = one;
				zeros_per_label[j] = zero;
				missings_per_label[j] = missing;
			}

			distinct = new HashMap<String, Integer>();
			histData = new double[dataset.getNumLabels() + 1];
			histData2 = new ArrayList<Double>();
			histData2b = new HashMap<String, Integer>();
			labels = new String[dataset.getNumLabels() + 1];

			for (int j = 0; j < dataset.getNumLabels() + 1; j++)
				labels[j] = j + "";
			//labels[j] = j + "/" + dataset.getNumLabels() + " active";

			numMissing = new int[dataset.getNumLabels() + 1];

			for (int i = 0; i < dataset.getNumInstances(); i++)
			{
				String combination = "";
				int nonZeroCount = 0;
				int numMissingI = 0;
				for (int j = 0; j < dataset.getNumLabels(); j++)
				{
					String val;
					if (dataset.getDataSet().get(i).isMissing((Attribute) dataset.getLabelAttributes().toArray()[j]))
					{
						val = "?";
						numMissingI++;
					}
					else
						val = dataset.getDataSet().get(i)
								.stringValue((Attribute) dataset.getLabelAttributes().toArray()[j]);
					combination += val;
					if (val.equals("1"))
						nonZeroCount++;
				}
				if (distinct.containsKey(combination))
					distinct.put(combination, distinct.get(combination) + 1);
				else
					distinct.put(combination, 1);
				histData[nonZeroCount]++;
				numMissing[numMissingI]++;

				if ((dataset.getNumLabels() - numMissingI) >= 2)
				{
					double ratio = nonZeroCount / (double) (dataset.getNumLabels() - numMissingI);
					histData2.add(ratio);
					String ratioStr = StringUtil.formatDouble(ratio);
					if (!histData2b.containsKey(ratioStr))
						histData2b.put(ratioStr, 0);
					histData2b.put(ratioStr, histData2b.get(ratioStr) + 1);
				}
			}
		}

		public String toString(boolean details)
		{
			StringLineAdder s = new StringLineAdder();
			s.add("endpoint-file: " + endpointFile);
			s.add("feature-file: " + featureFile);
			s.add("num-endpoints: " + numEndpoints);
			s.add("num-missing-allowed: " + numMissingAllowed);
			s.add("num-instances: " + dataset.getNumInstances());
			if (details)
			{
				s.add();
				s.add("labels: ");
				for (int j = 0; j < dataset.getNumLabels(); j++)
				{
					Attribute labelAttr = dataset.getDataSet().attribute(dataset.getLabelIndices()[j]);
					s.add(labelAttr.name() + " " + zeroOnes.get(j));
				}
				s.add();
				s.add("cardinality: " + dataset.getCardinality());
				s.add("#distinct combinations: " + distinct.size() + " / " + (int) Math.pow(2, dataset.getNumLabels()));
				//			s.add("distinct combinations: ");
				//			String distinctA[] = new String[distinct.size()];
				//			distinct.keySet().toArray(distinctA);
				//			Arrays.sort(distinctA);
				//			for (String comb : distinctA)
				//				System.out.print(comb + "(" + distinct.get(comb) + "), ");
				s.add("\n");
			}
			return s.toString();
		}

		public String toString()
		{
			return toString(true);
		}

		public void print()
		{
			System.out.println(toString(true));
		}

		public ChartPanel plotCorrelation() throws IOException
		{
			if (histData2.size() > 0)
			{
				//			System.out.println(ArrayUtil.toString(histData));
				System.out.println(ListUtil.toString(histData2));

				Dimension dim = new Dimension(400, 300);
				HistogramPanel p = new HistogramPanel("Correlation of " + dataset.getNumLabels() + " endpoint values",
						null, "Ratio active-compounds", "num compounds",
						"All compounds with at least 2 non-missing endpoint values (" + histData2.size() + ")",
						ArrayUtil.toPrimitiveDoubleArray(ListUtil.toArray(histData2)), 9);

				XYPlot plot = ((XYPlot) p.getChart().getPlot());
				plot.getDomainAxis().setRange(0.0, 1.0);

				//				String keys[] = CollectionUtil.toArray(histData2b.keySet());
				//				Arrays.sort(keys);
				//				double values[] = new double[keys.length];
				//				for (int i = 0; i < values.length; i++)
				//					values[i] = histData2b.get(keys[i]);
				//				BarPlotPanel p = new BarPlotPanel(title, "num compounds", values, keys);

				//			BarPlotPanel p = new BarPlotPanel(title, "num compounds", histData, labels);

				return p.getChartPanel();
			}
			else
				return null;
		}

		public ChartPanel plotMissingPerCompound() throws IOException
		{
			BarPlotPanel p = new BarPlotPanel("Num missing values for each compound", "num compounds",
					ArrayUtil.toPrimitiveDoubleArray(ArrayUtil.toDoubleArray(ArrayUtil.toIntegerArray(numMissing))),
					labels);

			JFreeChart c = p.getChartPanel().getChart();
			c.setSubtitles(ArrayUtil.toList(new Title[] { new TextTitle(dataset.getNumLabels() + " endpoint values"),
					new TextTitle(dataset.getNumInstances() + " compounds"), }));

			return p.getChartPanel();
		}

		public ChartPanel plotMissingPerLabel() throws IOException
		{
			LinkedHashMap<String, List<Double>> plotData = new LinkedHashMap<String, List<Double>>();
			plotData.put("0", ArrayUtil.toList(ArrayUtil.toDoubleArray(ArrayUtil.toIntegerArray(zeros_per_label))));
			plotData.put("1", ArrayUtil.toList(ArrayUtil.toDoubleArray(ArrayUtil.toIntegerArray(ones_per_label))));
			plotData.put("missing",
					ArrayUtil.toList(ArrayUtil.toDoubleArray(ArrayUtil.toIntegerArray(missings_per_label))));

			StackedBarPlot sbp = new StackedBarPlot("Missing values", "endpoints", "num compounds", plotData,
					labelNames);

			CategoryPlot plot = (CategoryPlot) sbp.getChartPanel().getChart().getPlot();
			CategoryAxis xAxis = (CategoryAxis) plot.getDomainAxis();
			xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);

			return sbp.getChartPanel();
		}

		public ResultSet getMissingPerLabel()
		{
			ResultSet rs = new ResultSet();
			for (int i = 0; i < labelNames.length; i++)
			{
				int r = rs.addResult();
				rs.setResultValue(r, "Endpoint", labelNames[i]);
				rs.setResultValue(r, "0/1", zeros_per_label[i] + "/" + ones_per_label[i]);
				rs.setResultValue(r, "Missing", missings_per_label[i]);
			}
			return rs;
		}

	}

	//	public static void plotCorrelation(List<File> files) throws Exception
	//	{
	//		for (File file : files)
	//		{
	//			String data = file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf("."));
	//			MultiLabelInstances dataset = new MultiLabelInstances(data + ".arff", data + ".xml");
	//			DatasetInfo di = new DatasetInfo(dataset);
	//			di.print();
	//			di.plotCorrelation();
	//		}
	//	}
	//
	//	public static void plotMissingPerFile(List<File> files) throws Exception
	//	{
	//		for (File file : files)
	//		{
	//			String data = file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf("."));
	//			MultiLabelInstances dataset = new MultiLabelInstances(data + ".arff", data + ".xml");
	//			DatasetInfo di = new DatasetInfo(dataset);
	//			di.print();
	//			di.plotMissingPerCompound();
	//		}
	//	}

	public static void plotMissing(List<File> files, List<String> titles, int maxNumMissing) throws Exception
	{
		LinkedHashMap<String, List<Double>> plotData = new LinkedHashMap<String, List<Double>>();

		String categories[];
		if (titles != null)
			categories = ListUtil.toArray(titles);
		else
			categories = new String[files.size()];

		int index = 0;
		for (File file : files)
		{
			System.out.println("\n---------------");
			System.out.println(file + "\n");

			String data = file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf("."));
			MultiLabelInstances dataset = new MultiLabelInstances(data + ".arff", data + ".xml");

			DatasetInfo di = new DatasetInfo(dataset);
			di.print();
			//di.plotCorrelation();

			if (dataset.getNumLabels() > 1)
			{
				if (index == 0)
					for (int i = 0; i <= maxNumMissing; i++)
						plotData.put("num-missing=" + i, new ArrayList<Double>());
				for (int i = 0; i <= maxNumMissing; i++)
					plotData.get("num-missing=" + i).add((double) di.numMissing[i]);
			}
			else
			{
				if (index == 0)
				{
					plotData.put("real-value", new ArrayList<Double>());
					plotData.put("missing", new ArrayList<Double>());
				}
				plotData.get("real-value").add((double) di.numMissing[0]);
				plotData.get("missing").add((double) di.numMissing[1]);
			}

			if (titles == null)
			{
				String s = getEndpoint(file);
				if (s.indexOf("_") == -1)
				{
					s = Integer.parseInt(s) + "";
					while (s.endsWith("."))
						s = s.substring(0, s.length() - 1);
					s = s.replaceAll("\\.", "-");
				}
				else
				{
					s = (StringUtil.numOccurences(s, "_") + 1) + " endpoints";
				}
				categories[index++] = s;

			}
		}
		System.out.println(ArrayUtil.toString(categories));
		for (String s : plotData.keySet())
		{
			System.out.println(ListUtil.toString(plotData.get(s)));
		}
		StackedBarPlot sbp = new StackedBarPlot("Missing values", "endpoints", "num compounds", plotData, categories);

		if (files.size() > 5)
		{
			CategoryPlot plot = (CategoryPlot) sbp.getChartPanel().getChart().getPlot();
			CategoryAxis xAxis = (CategoryAxis) plot.getDomainAxis();
			xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
		}

		//sbp.setSeriesColor(0, Color.BLACK);
		SwingUtil.showInDialog(sbp);
	}

	public static String getEndpoint(File f)
	{
		String endpoint = f.getName().substring(0, f.getName().lastIndexOf("."));
		for (String s : new String[] { "_withoutMissing", "_withMissing", "_allMissing", "_withV", "_withoutV" })
			if (endpoint.endsWith(s))
				endpoint = endpoint.substring(0, endpoint.length() - s.length());
		return endpoint.trim();
	}

	public static void simulateCorrelation(int n, int x, double ratioMissing)
	{
		Random r = new Random();
		List<Double> histdata = new ArrayList<Double>();
		for (int i = 0; i < n; i++)
		{
			int count = 0;
			int missingCount = 0;
			for (int j = 0; j < x; j++)
			{
				if (r.nextDouble() < ratioMissing)
				{
					if (r.nextBoolean())
						count++;
				}
				else
					missingCount++;
			}
			if (x - missingCount >= 1)
			{
				double ratio = count / (double) (x - missingCount);
				histdata.add(ratio);
			}
		}
		Dimension dim = new Dimension(400, 300);
		HistogramPanel p = new HistogramPanel("test", null, "ratio active compounds", "num compounds", "title",
				ArrayUtil.toPrimitiveDoubleArray(ListUtil.toArray(histdata)), 10);
		JFrame f = new JFrame("test");
		f.add(p);
		f.setSize(dim);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setVisible(true);
	}

}
