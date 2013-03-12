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

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.title.Title;

import util.ArrayUtil;
import util.ListUtil;
import util.StringUtil;
import util.SwingUtil;
import weka.core.Attribute;
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

		List<Double> histData2;
		HashMap<String, Integer> histData2b;

		public DatasetInfo(MultiLabelInstances dataset)
		{
			this.dataset = dataset;

			zeroOnes = new ArrayList<String>();
			for (int j = 0; j < dataset.getNumLabels(); j++)
			{
				int one = 0;
				int zero = 0;
				int missing = 0;
				for (int i = 0; i < dataset.getNumInstances(); i++)
				{
					if (dataset.getDataSet().get(i).isMissing((Attribute) dataset.getLabelAttributes().toArray()[j]))
						missing++;
					else if (dataset.getDataSet().get(i)
							.stringValue((Attribute) dataset.getLabelAttributes().toArray()[j]).equals("1"))
						one++;
					else
						zero++;
				}
				zeroOnes.add("(" + zero + "/" + one + " missing:" + missing + ")");
			}

			distinct = new HashMap<String, Integer>();
			histData = new double[dataset.getNumLabels() + 1];
			histData2 = new ArrayList<Double>();
			histData2b = new HashMap<String, Integer>();
			labels = new String[dataset.getNumLabels() + 1];
			for (int j = 0; j < dataset.getNumLabels() + 1; j++)
				labels[j] = j + "";
			//labels[j] = j + "/" + dataset.getNumLabels() + " active";

			numMissing = new int[50];

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

		public void print()
		{
			System.out.println("#instances: " + dataset.getNumInstances());
			System.out.println();
			System.out.println("#labels: " + dataset.getNumLabels());
			System.out.println("labels: ");
			for (int j = 0; j < dataset.getNumLabels(); j++)
			{
				System.out.print((Attribute) dataset.getLabelAttributes().toArray()[j] + " ");
				System.out.println(zeroOnes.get(j));
			}
			System.out.println("cardinality: " + dataset.getCardinality());
			System.out.println();
			System.out.println("#distinct combinations: " + distinct.size() + " / "
					+ (int) Math.pow(2, dataset.getNumLabels()));
			//			System.out.println("distinct combinations: ");
			//			String distinctA[] = new String[distinct.size()];
			//			distinct.keySet().toArray(distinctA);
			//			Arrays.sort(distinctA);
			//			for (String comb : distinctA)
			//				System.out.print(comb + "(" + distinct.get(comb) + "), ");
			System.out.println("\n");
		}

		public void plotCorrelation() throws IOException
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

				//			ChartUtilities.saveChartAsPNG(new File("/home/martin/workspace/BMBFUtil/results/" + title + ".png"), p
				//					.getChartPanel().getChart(), dim.width, dim.height);
				//HistogramPanel p = new HistogramPanel("test", null, "x", "y", "caption", histData, dataset.getNumLabels());
				JFrame f = new JFrame("Correlation of " + dataset.getNumLabels() + " endpoint values");
				f.add(p);
				f.setSize(dim);
				f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				f.setVisible(true);
			}
		}

		public void plotMissing() throws IOException
		{
			//			System.out.println(ArrayUtil.toString(histData));

			Dimension dim = new Dimension(800, 600);
			//					HistogramPanel p = new HistogramPanel("Correlation of " + dataset.getNumLabels() + " endpoint values",
			//							null, "Ratio active-compounds", "num compounds",
			//							"All compounds with at least 2 non-missing endpoint values (" + histData2.size() + ")",
			//							ArrayUtil.toPrimitiveDoubleArray(ListUtil.toArray(histData2)), 9);
			//					XYPlot plot = ((XYPlot) p.getChart().getPlot());
			//					plot.getDomainAxis().setRange(0.0, 1.0);

			//				String keys[] = CollectionUtil.toArray(histData2b.keySet());
			//				Arrays.sort(keys);
			//				double values[] = new double[keys.length];
			//				for (int i = 0; i < values.length; i++)
			//					values[i] = histData2b.get(keys[i]);
			//				BarPlotPanel p = new BarPlotPanel(title, "num compounds", values, keys);

			BarPlotPanel p = new BarPlotPanel("Num missing values", "num compounds",
					ArrayUtil.toPrimitiveDoubleArray(ArrayUtil.toDoubleArray(ArrayUtil.toIntegerArray(numMissing))),
					labels);

			JFreeChart c = p.getChartPanel().getChart();
			c.setSubtitles(ArrayUtil.toList(new Title[] { new TextTitle(dataset.getNumLabels() + " endpoint values"),
					new TextTitle(dataset.getNumInstances() + " compounds"), }));

			ChartUtilities.saveChartAsPNG(new File("/home/martin/tmp/pic.png"), p.getChartPanel().getChart(),
					dim.width, dim.height);

			//HistogramPanel p = new HistogramPanel("test", null, "x", "y", "caption", histData, dataset.getNumLabels());
			JFrame f = new JFrame("Correlation of " + dataset.getNumLabels() + " endpoint values");
			f.add(p);
			f.setSize(dim);
			f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			f.setVisible(true);
		}
	}

	public static void plotCorrelation(List<File> files) throws Exception
	{
		for (File file : files)
		{
			String data = file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf("."));
			MultiLabelInstances dataset = new MultiLabelInstances(data + ".arff", data + ".xml");
			DatasetInfo di = new DatasetInfo(dataset);
			di.print();
			di.plotCorrelation();
		}
	}

	public static void plotMissingPerFile(List<File> files) throws Exception
	{
		for (File file : files)
		{
			String data = file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf("."));
			MultiLabelInstances dataset = new MultiLabelInstances(data + ".arff", data + ".xml");
			DatasetInfo di = new DatasetInfo(dataset);
			di.print();
			di.plotMissing();
		}
	}

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
