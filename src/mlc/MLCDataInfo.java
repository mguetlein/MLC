package mlc;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;

import mlc.report.DiscMethod;
import mlc.reporting.ReportMLC;
import mulan.data.MultiLabelInstances;
import mulan.evaluation.Settings;
import mulan.evaluation.measure.ConfidenceLevel;
import mulan.evaluation.measure.ConfidenceLevelProvider;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.title.Title;

import util.ArrayUtil;
import util.CorrelationMatrix;
import util.CorrelationMatrix.DoubleCorrelationMatrix;
import util.CorrelationMatrix.PearsonBooleanCorrelationMatrix;
import util.CorrelationMatrix.PearsonDoubleCorrelationMatrix;
import util.CorrelationMatrix.SpearmanDoubleCorrelationMatrix;
import util.DoubleArraySummary;
import util.FileUtil;
import util.FileUtil.CSVFile;
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
import gui.MatrixPanel;

public class MLCDataInfo
{
	public MultiLabelInstances dataset;
	HashMap<String, Integer> distinct;
	ArrayList<String> zeroOnes;
	private int[] numMissing;

	double[] histData;
	String[] labels;
	String[] labelNames;

	List<Double> histData2;
	HashMap<String, Integer> histData2b;

	public String datasetName;
	public String endpointFile;
	public String featureFile;
	public int numEndpoints;
	public int numMissingAllowed;
	public int discretizationLevel;
	public boolean includeV;
	//	private HashMap<String, String> classValues = new HashMap<String, String>();

	public int[] ones_per_label;
	public int[] zeros_per_label;
	public int[] missings_per_label;

	private static HashMap<MultiLabelInstances, MLCDataInfo> map = new HashMap<MultiLabelInstances, MLCDataInfo>();

	private HashMap<String, DoubleArraySummary> endpointSummaryMap;

	public static MLCDataInfo get(String datasetName)
	{
		return get(ReportMLC.getData(datasetName));
	}

	public static MLCDataInfo get(MultiLabelInstances dataset)
	{
		if (!map.containsKey(dataset))
			map.put(dataset, new MLCDataInfo(dataset));
		return map.get(dataset);
	}

	private static HashMap<MultiLabelInstances, CSVFile> csvFiles = new HashMap<MultiLabelInstances, FileUtil.CSVFile>();

	private CSVFile getCSV()
	{
		return getCSV(dataset);
	}

	private static CSVFile getCSV(MultiLabelInstances dataset)
	{
		if (!csvFiles.containsKey(dataset))
		{
			System.out.println("reading csv: " + Settings.csvFile(get(dataset).datasetName));
			csvFiles.put(dataset, FileUtil.readCSV(Settings.csvFile(get(dataset).datasetName)));
			if (dataset.getDataSet().numInstances() != csvFiles.get(dataset).content.size() - 1)
				throw new Error("num instances does not fit, csv file: " + (csvFiles.get(dataset).content.size() - 1)
						+ ", arff file: " + dataset.getDataSet().numInstances());
		}
		return csvFiles.get(dataset);
	}

	private HashMap<String, String> idInchiMap;

	private String getInchiViaID(String compoundID)
	{
		if (idInchiMap == null)
		{
			idInchiMap = new HashMap<String, String>();
			System.out.println("reading inchi: " + Settings.inchiFile(get(dataset).datasetName));
			for (String line : FileUtil.readStringFromFile(Settings.inchiFile(get(dataset).datasetName)).split("\n"))
			{
				String vals[] = line.split("\t");
				if (!(vals.length == 2 && (vals[0].equals("") || vals[0].toLowerCase().startsWith("inchi"))))
					throw new IllegalStateException("not a inchi file: " + ArrayUtil.toString(vals));
				idInchiMap.put(vals[1], vals[0]);
			}
		}
		return idInchiMap.get(compoundID);
	}

	private HashMap<String, String> idSmilesMap;

	private String getSmilesViaID(String compoundID)
	{
		if (idSmilesMap == null)
		{
			idSmilesMap = new HashMap<String, String>();
			System.out.println("reading smiles: " + Settings.smilesFile(get(dataset).datasetName));
			for (String line : FileUtil.readStringFromFile(Settings.smilesFile(get(dataset).datasetName)).split("\n"))
			{
				String vals[] = line.split("\t");
				if (vals.length != 2)
					throw new IllegalStateException("not a smiles file: " + ArrayUtil.toString(vals));
				idSmilesMap.put(vals[1], vals[0]);
			}
		}
		return idSmilesMap.get(compoundID);
	}

	public DoubleArraySummary getEndpointInfo(String endpoint)
	{
		return getEndpointInfo(dataset, endpoint);
	}

	public DoubleArraySummary getEndpointInfo(MultiLabelInstances dataset, String endpoint)
	{
		if (endpointSummaryMap == null)
			endpointSummaryMap = new HashMap<String, DoubleArraySummary>();
		if (!endpointSummaryMap.containsKey(endpoint))
			endpointSummaryMap.put(endpoint, DoubleArraySummary.create(getRealValues(endpoint)));
		return endpointSummaryMap.get(endpoint);
	}

	public String getInchi(int instanceIndex)
	{
		CSVFile csv = getCSV();
		String id = csv.content.get(instanceIndex + 1)[csv.getColumnIndex("id")];
		return getInchiViaID(id);
	}

	public String getSmiles(int instanceIndex)
	{
		CSVFile csv = getCSV();
		String id = csv.content.get(instanceIndex + 1)[csv.getColumnIndex("id")];
		return getSmilesViaID(id);
	}

	private MLCDataInfo(MultiLabelInstances dataset)
	{
		String s[] = dataset.getDataSet().relationName().split("#");
		for (String string : s)
		{

			if (string.startsWith("dataset-name:"))
				datasetName = string.substring("dataset-name:".length());
			else if (string.startsWith("endpoint-file:"))
				endpointFile = string.substring("endpoint-file:".length());
			else if (string.startsWith("feature-file:"))
				featureFile = string.substring("feature-file:".length());
			else if (string.startsWith("num-endpoints:"))
				numEndpoints = IntegerUtil.parseInteger(string.substring("num-endpoints:".length()));
			else if (string.startsWith("num-features:"))
				numEndpoints = dataset.getDataSet().numAttributes() - dataset.getNumLabels();
			else if (string.startsWith("num-missing-allowed:"))
				numMissingAllowed = IntegerUtil.parseInteger(string.substring("num-missing-allowed:".length()));
			else if (string.startsWith("discretization-level:"))
				discretizationLevel = IntegerUtil.parseInteger(string.substring("discretization-level:".length()));
			else if (string.startsWith("include-v:"))
				includeV = Boolean.parseBoolean(string.substring("include-v:".length()));
			//			else if (string.startsWith("class-value-0:"))
			//				classValues.put("0", string.substring("class-value-0:".length()).replace("_", " "));
			//			else if (string.startsWith("class-value-1:"))
			//				classValues.put("1", string.substring("class-value-1:".length()).replace("_", " "));
			//			else if (string.startsWith("class-value-missing:"))
			//				classValues.put("missing", string.substring("class-value-missing:".length()).replace("_", " "));
		}
		if (numEndpoints != dataset.getNumLabels())
			throw new IllegalStateException();
		//		if (classValues.size() != 3)
		//			throw new IllegalStateException("class values missing");

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

	public static String ACTIVE = "active";
	public static String INACTIVE = "inactive";
	public static String MISSING = "missing";

	public String toString(boolean details)
	{
		StringLineAdder s = new StringLineAdder();
		s.add("endpoint-file: " + endpointFile);
		s.add("feature-file: " + featureFile);
		s.add("num-endpoints: " + numEndpoints);
		s.add("num-missing-allowed: " + numMissingAllowed);
		s.add("num-instances: " + dataset.getNumInstances());
		s.add("discretization-level: " + discretizationLevel);
		s.add("include-V: " + includeV);
		//		s.add("class-values: " + getNonMissingClassValuesString());
		//		s.add("missing-values: " + getMissingClassValue());

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

	public ChartPanel plotRealValueHistogram(int j, boolean zoom)
	{
		return plotRealValueHistogram(j, zoom, null);
	}

	public ChartPanel plotRealValueHistogram(int j, boolean zoom, StudyDuration dur)
	{
		Attribute labelAttr = dataset.getDataSet().attribute(dataset.getLabelIndices()[j]);

		System.out.println("create clazz histogram for " + labelAttr.name() + " study-duration: " + dur);

		double[] all;
		double[] active;
		if (dur == null)
		{
			all = getRealValues(labelAttr.name());
			active = getRealValues(labelAttr.name(), "1");
		}
		else
		{
			all = ArrayUtil.toPrimitiveDoubleArray(getStudyDurationValues(j, dur));
			active = ArrayUtil.toPrimitiveDoubleArray(getStudyDurationValues(j, dur, "1"));
		}

		List<String> clazz = ArrayUtil.toList(new String[] { INACTIVE, ACTIVE });

		if (dur == null && zeros_per_label[j] + ones_per_label[j] != all.length)
			throw new IllegalStateException("should contain both " + (zeros_per_label[j] + ones_per_label[j]) + " != "
					+ all.length);
		if (dur == null && ones_per_label[j] != active.length)
			throw new IllegalStateException("should contain only ones " + ones_per_label[j] + " != " + active.length);

		String missingStr = "";
		if (dur == null)
			missingStr = " missing: " + missings_per_label[j];
		List<String> subtitles = ArrayUtil.toList(new String[] { "#active: " + active.length + " / " + all.length
				+ missingStr });
		if (zoom)
		{
			int cut = (int) Math.floor(all.length * 9.0 / 10.0);
			if (active[active.length - 1] >= all[cut])
				throw new Error("not yet implemented, remove values from both arrays!");
			subtitles.add("Zoomed in: without " + (all.length - cut) + " top compounds");
			all = Arrays.copyOfRange(all, 0, cut);
		}
		List<double[]> vals = new ArrayList<double[]>();
		vals.add(all);
		vals.add(active);
		String details = "";
		if (dur != null)
			details = " (duration: sub-" + dur.toString() + ")";

		HistogramPanel h = new HistogramPanel(labelAttr.name() + details, subtitles, "LOEL (mmol)", "num compounds",
				clazz, vals, 50);
		h.setIntegerTickUnits();

		return h.getChartPanel();
	}

	public ChartPanel plotRealValueStudyDurationHistogram(int j, boolean zoom)
	{
		Attribute labelAttr = dataset.getDataSet().attribute(dataset.getLabelIndices()[j]);

		System.out.println("create clazz histogram for " + labelAttr.name());

		double[] all = getRealValues(labelAttr.name());
		double[] subset = ArrayUtil.toPrimitiveDoubleArray(getStudyDurationValues(j, StudyDuration.chronic));

		List<String> clazz = ArrayUtil
				.toList(new String[] { StudyDuration.accute.name(), StudyDuration.chronic.name() });

		List<String> subtitles = ArrayUtil
				.toList(new String[] { (all.length - subset.length) + " / " + subset.length });
		if (zoom)
		{
			int cut = (int) Math.floor(all.length * 9.0 / 10.0);
			double cutValue = all[cut];
			all = Arrays.copyOfRange(all, 0, cut);
			int cutSubset = -1;
			for (int i = 0; i < subset.length; i++)
				if (subset[i] >= cutValue)
				{
					cutSubset = i;
					break;
				}
			if (cutSubset != -1)
				subset = Arrays.copyOfRange(subset, 0, cutSubset);
			subtitles.add("Zoomed in: without " + (all.length - cut) + " top compounds");

		}
		List<double[]> vals = new ArrayList<double[]>();
		vals.add(all);
		vals.add(subset);
		HistogramPanel h = new HistogramPanel("Real values for " + labelAttr.name() + (zoom ? " (zoom)" : ""),
				subtitles, "LOEL (mmol)", "num compounds", clazz, vals, 50);
		h.setIntegerTickUnits();

		return h.getChartPanel();
	}

	public ChartPanel plotCorrelationHistogramm() throws IOException
	{
		if (histData2.size() > 0)
		{
			//			System.out.println(ArrayUtil.toString(histData));
			System.out.println(ListUtil.toString(histData2));

			//			Dimension dim = new Dimension(400, 300);
			HistogramPanel p = new HistogramPanel("Class distributions of " + dataset.getNumLabels() + " endpoints",
					null, "Class distribution ratio, 0.0 -> class is '" + INACTIVE
							+ "' for all endpoints, 1.0 -> class is '" + ACTIVE + "' for all endpoints",
					"num compounds", "All compounds with at least 2 non-missing endpoint values (" + histData2.size()
							+ ")", ArrayUtil.toPrimitiveDoubleArray(ListUtil.toArray(histData2)), 9);

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
		BarPlotPanel p = new BarPlotPanel("Num missing values for each compound"// (missing = '" + getMissingClassValue()+ "')
				, "num compounds", ArrayUtil.toPrimitiveDoubleArray(ArrayUtil.toDoubleArray(ArrayUtil
						.toIntegerArray(numMissing))), labels);

		JFreeChart c = p.getChartPanel().getChart();
		c.setSubtitles(ArrayUtil.toList(new Title[] { new TextTitle(dataset.getNumLabels() + " endpoint values"),
				new TextTitle(dataset.getNumInstances() + " compounds"), }));

		return p.getChartPanel();
	}

	public ChartPanel plotMissingPerLabel() throws IOException
	{
		LinkedHashMap<String, List<Double>> plotData = new LinkedHashMap<String, List<Double>>();
		plotData.put(INACTIVE, ArrayUtil.toList(ArrayUtil.toDoubleArray(ArrayUtil.toIntegerArray(zeros_per_label))));
		plotData.put(ACTIVE, ArrayUtil.toList(ArrayUtil.toDoubleArray(ArrayUtil.toIntegerArray(ones_per_label))));
		plotData.put(MISSING, ArrayUtil.toList(ArrayUtil.toDoubleArray(ArrayUtil.toIntegerArray(missings_per_label))));

		StackedBarPlot sbp = new StackedBarPlot("Class and missing values", "endpoints", "num compounds", plotData,
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
			rs.setResultValue(r, "endpoint", labelNames[i]);
			rs.setResultValue(r, INACTIVE + "/" + ACTIVE, zeros_per_label[i] + "/" + ones_per_label[i]);
			rs.setResultValue(r, INACTIVE + "/" + ACTIVE + " ratio", zeros_per_label[i] / (double) ones_per_label[i]);
			rs.setResultValue(r, MISSING, missings_per_label[i]);
		}
		return rs;
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
			MultiLabelInstances dataset = new MultiLabelInstances(Settings.arffFile(data), Settings.xmlFile(data));

			MLCDataInfo di = new MLCDataInfo(dataset);
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

	public CorrelationMatrix<Boolean> getClassCorrelationMatrix()
	{
		List<Boolean[]> matrixValues = new ArrayList<Boolean[]>();
		for (int j = 0; j < dataset.getNumLabels(); j++)
		{
			Attribute labelAttr = dataset.getDataSet().attribute(dataset.getLabelIndices()[j]);
			Boolean[] d = new Boolean[dataset.getNumInstances()];
			for (int i = 0; i < dataset.getNumInstances(); i++)
			{
				Double v = dataset.getDataSet().get(i).value(labelAttr);
				d[i] = (Double.isNaN(v) ? null : (v == 1.0 ? true : false));
			}
			matrixValues.add(d);
		}
		PearsonBooleanCorrelationMatrix m = new PearsonBooleanCorrelationMatrix();
		m.setMinNumValues(10);
		m.computeMatrix(matrixValues);
		return m;
	}

	public static enum RealMatrixType
	{
		Pearson, PearsonLog, Spearman
	}

	public CorrelationMatrix<Double> getRealValueCorrelationMatrix(RealMatrixType type)
	{
		List<Double[]> matrixValues = new ArrayList<Double[]>();
		for (int j = 0; j < dataset.getNumLabels(); j++)
		{
			Attribute labelAttr = dataset.getDataSet().attribute(dataset.getLabelIndices()[j]);
			String s[] = getCSV().getColumn(labelAttr.name() + "_real");
			Double d[] = new Double[s.length];
			for (int i = 0; i < d.length; i++)
				if (s[i] != null && !s[i].equals("V"))
				{
					d[i] = Double.parseDouble(s[i]);
					if (type == RealMatrixType.PearsonLog)
						d[i] = Math.log(d[i]);
				}
			matrixValues.add(d);
		}
		DoubleCorrelationMatrix m = (type == RealMatrixType.Spearman) ? new SpearmanDoubleCorrelationMatrix()
				: new PearsonDoubleCorrelationMatrix();
		m.setMinNumValues(10);
		m.computeMatrix(matrixValues);
		return m;
	}

	public MatrixPanel plotCorrelationMatrix(boolean clazz, RealMatrixType type)
	{
		MatrixPanel p = new MatrixPanel();
		p.setMinNumValues(10);
		p.setTitleFont(p.getFont().deriveFont(p.getFont().getSize() + 12.0F).deriveFont(Font.BOLD));
		p.setBackground(Color.WHITE);
		//		if (numEndpoints > 25)
		//			p.setFont(p.getFont().deriveFont(10.0F));

		String typeStr = "";
		if (clazz)
			typeStr = "Pearson";
		else
			typeStr = type.toString();
		p.setTitleString(typeStr + " Correlation Matrix for " + (clazz ? "class" : "real") + " values from dataset "
				+ datasetName);
		if (clazz)
			p.setSubtitleString("1 := high correlation, 0 := no correlation, -1 := inverse correlation, the small numbers below correspond to the number of class values of "
					+ INACTIVE + "/" + ACTIVE + " per endpoint or endpoint-pair");
		else
			p.setSubtitleString("1 := high correlation, 0 := no correlation, -1 := inverse correlation, the small numbers below correspond to the number of real values per endpoint or endpoint-pair");

		List<String> attributeNames = new ArrayList<String>();
		for (int j = 0; j < dataset.getNumLabels(); j++)
		{
			Attribute labelAttr = dataset.getDataSet().attribute(dataset.getLabelIndices()[j]);
			attributeNames.add(labelAttr.name());
		}
		p.fill(clazz ? getClassCorrelationMatrix() : getRealValueCorrelationMatrix(type),
				ArrayUtil.toArray(attributeNames));

		//		if (numEndpoints > 25)
		//			p.setPreferredSize(new Dimension(1500, 1450));
		//		else
		//			p.setPreferredSize(new Dimension(1000, 950));

		return p;
	}

	public String getRealValueUnit()
	{
		return "mmol";
	}

	public String getConfidenceLevelNice(double confidence)
	{
		double p;
		if (confidence > 0.5)
			p = Math.max(0, (confidence - 0.5) * 2);
		else
			p = (0.5 - confidence) * 2;
		return ConfidenceLevelProvider.getConfidence(confidence).getName().trim() + ": "
				+ StringUtil.formatDouble(p * 100, 1) + "%";
	}

	public double getRealValue(Integer idx, String labelName)
	{
		CSVFile csv = getCSV();
		return Double.parseDouble(csv.content.get(idx + 1)[csv.getColumnIndex(labelName + "_real")]);
	}

	public Object getCompoundValue(int idx, String s)
	{
		CSVFile csv = getCSV();
		return csv.content.get(idx + 1)[csv.getColumnIndex(s)];
	}

	private HashMap<String, double[]> realVals = new HashMap<String, double[]>();

	public double[] getRealValues(String endpoint)
	{
		return getRealValues(endpoint, null);
	}

	public double[] getRealValues(String endpoint, String clazz)
	{
		String key = datasetName + "#" + endpoint + "#" + clazz;
		if (!realVals.containsKey(key))
		{
			CSVFile csv = getCSV();
			Double d[] = csv.getDoubleColumn(endpoint + "_real");
			String s[] = csv.getColumn(endpoint);
			List<Double> vals = new ArrayList<Double>();
			for (int i = 0; i < s.length; i++)
			{
				if (s[i] == null)
				{
					if (d[i] != null)
						throw new IllegalArgumentException();
				}
				else
				{
					if (d[i] == null)
						throw new IllegalArgumentException();
					if (clazz == null || s[i].equals(clazz))
						vals.add(d[i]);
				}
			}
			double[] vs = ArrayUtil.toPrimitiveDoubleArray(vals);
			Arrays.sort(vs);
			realVals.put(key, vs);
		}
		return realVals.get(key);
	}

	HashMap<String, List<Double>> studyDurationValues = new HashMap<String, List<Double>>();
	HashMap<String, List<Double>> routeValues = new HashMap<String, List<Double>>();

	public static enum StudyDuration
	{
		accute, chronic
	}

	public static enum Route
	{
		inhalation, oral
	}

	public boolean includesStudyDuration()
	{
		return includesStudyDuration(datasetName);
	}

	public static boolean includesStudyDuration(String datasetName)
	{
		try
		{
			getCompoundInfoValue(datasetName, 0, "study-duration");
			return true;
		}
		catch (IllegalArgumentException e)
		{
			return false;
		}
	}

	public StudyDuration getStudyDuration(int compoundIndex)
	{
		return getStudyDuration(datasetName, compoundIndex);
	}

	public static StudyDuration getStudyDuration(String datasetName, int compoundIndex)
	{
		//		Attribute a = dataset.getDataSet().attribute(dataset.getLabelIndices()[label]);
		//		if (dataset.getDataSet().get(compoundIndex).isMissing(a))
		//			throw new IllegalStateException("missing!");
		String dur = getCompoundInfoValue(datasetName, compoundIndex, "study-duration").toString();
		if (dur.contains(StudyDuration.accute.toString()))
			return StudyDuration.accute;
		else if (dur.contains(StudyDuration.chronic.toString()))
			return StudyDuration.chronic;
		else
			throw new Error("wtf: " + dur);
	}

	public Route getRoute(int compoundIndex)
	{
		return getRoute(datasetName, compoundIndex);
	}

	public static Route getRoute(String datasetName, int compoundIndex)
	{
		String r = getCompoundInfoValue(datasetName, compoundIndex, "route").toString();
		if (r.contains(Route.inhalation.toString()))
			return Route.inhalation;
		else if (r.contains(Route.oral.toString()))
			return Route.oral;
		else
			throw new Error("wtf: " + r);
	}

	public List<Double> getStudyDurationValues(int label, StudyDuration studyDuration)
	{
		return getStudyDurationValues(label, studyDuration, null);
	}

	public List<Double> getStudyDurationValues(int label, StudyDuration studyDuration, String clazz)
	{
		String key = label + "#" + studyDuration + "#" + clazz;
		if (!studyDurationValues.containsKey(key))
		{
			Attribute a = dataset.getDataSet().attribute(dataset.getLabelIndices()[label]);
			String name = a.name();

			String s[] = getCSV().getColumn(name);

			for (StudyDuration d : StudyDuration.values())
				for (String c : new String[] { "1", "0", null })
					studyDurationValues.put(label + "#" + d + "#" + c, new ArrayList<Double>());
			for (int i = 0; i < dataset.getNumInstances(); i++)
				if (!dataset.getDataSet().get(i).isMissing(a))
				{
					double v = getRealValue(i, name);
					StudyDuration d = getStudyDuration(i);
					studyDurationValues.get(label + "#" + d + "#" + null).add(v);
					studyDurationValues.get(label + "#" + d + "#" + s[i]).add(v);
				}
			for (StudyDuration d : StudyDuration.values())
				for (String c : new String[] { "1", "0", null })
					Collections.sort(studyDurationValues.get(label + "#" + d + "#" + c));
			for (StudyDuration d : StudyDuration.values())
				if (ListUtil.last(studyDurationValues.get(label + "#" + d + "#1")) >= studyDurationValues.get(
						label + "#" + d + "#0").get(0))
				{

					System.err.println(name + " " + d + "   active: "
							+ ListUtil.toString(studyDurationValues.get(label + "#" + d + "#1")));
					System.err.println(name + " " + d + " inactive: "
							+ ListUtil.toString(studyDurationValues.get(label + "#" + d + "#0")));
					throw new Error("data inconsistent!!!!");
				}
		}
		return studyDurationValues.get(label + "#" + studyDuration + "#" + clazz);
	}

	public List<Double> getRouteValues(int label, Route route)
	{
		return getRouteValues(label, route, null);
	}

	public List<Double> getRouteValues(int label, Route route, String clazz)
	{
		String key = label + "#" + route + "#" + clazz;
		if (!routeValues.containsKey(key))
		{
			Attribute a = dataset.getDataSet().attribute(dataset.getLabelIndices()[label]);
			String name = a.name();

			String s[] = getCSV().getColumn(name);

			for (Route d : Route.values())
				for (String c : new String[] { "1", "0", null })
					routeValues.put(label + "#" + d + "#" + c, new ArrayList<Double>());
			for (int i = 0; i < dataset.getNumInstances(); i++)
				if (!dataset.getDataSet().get(i).isMissing(a))
				{
					double v = getRealValue(i, name);
					Route d = getRoute(i);
					routeValues.get(label + "#" + d + "#" + null).add(v);
					routeValues.get(label + "#" + d + "#" + s[i]).add(v);
				}
			for (Route d : Route.values())
				for (String c : new String[] { "1", "0", null })
					Collections.sort(routeValues.get(label + "#" + d + "#" + c));
		}
		return routeValues.get(label + "#" + route + "#" + clazz);
	}

	public List<Object[]> getConfusionMatrix(ResultSet results, int l, ConfidenceLevel confLevel)
	{
		if (results.getNumResults() > 1)
			throw new IllegalArgumentException("merge first pls");
		String s = confLevel.getShortName();
		return ConfusionMatrix.buildMatrix((Double) results.getUniqueValue("TP#" + l + s),
				(Double) results.getUniqueValue("TN#" + l + s), (Double) results.getUniqueValue("FP#" + l + s),
				(Double) results.getUniqueValue("FN#" + l + s), ACTIVE, INACTIVE);
	}

	Boolean realData = null;

	public boolean hasRealData()
	{
		if (realData == null)
		{
			Attribute labelAttr = dataset.getDataSet().attribute(dataset.getLabelIndices()[0]);
			realData = (getCSV().getColumnIndex(labelAttr.name() + "_real") != -1);
			if (realData)
				if (!includesStudyDuration())
					realData = false;
		}
		return realData;
	}

	private static HashMap<String, CSVFile> csvCompoundInfo = new HashMap<String, FileUtil.CSVFile>();

	private static CSVFile getCSVCompoundInfo(String datasetName)
	{
		if (!csvCompoundInfo.containsKey(datasetName))
		{
			boolean base = ClusterEndpoint.numCompounds != 0;
			System.out.println("reading csv-compoundInfo: " + Settings.csvCompoundInfo(datasetName, base));
			csvCompoundInfo.put(datasetName, FileUtil.readCSV(Settings.csvCompoundInfo(datasetName, base)));
			if (!csvCompoundInfo.get(datasetName).getHeader()[0].equals("id"))
				throw new IllegalStateException("no id column");
			if (base)
			{
				if (ClusterEndpoint.numCompounds != csvCompoundInfo.get(datasetName).content.size() - 1)
					throw new Error("num instances does not fit, info file: "
							+ (csvCompoundInfo.get(datasetName).content.size() - 1) + ", cluster file: "
							+ ClusterEndpoint.numCompounds);
			}
			else
			{
				if (ReportMLC.getData(datasetName).getDataSet().numInstances() != csvCompoundInfo.get(datasetName).content
						.size() - 1)
					throw new Error("num instances does not fit, info file: "
							+ (csvCompoundInfo.get(datasetName).content.size() - 1) + ", arff file: "
							+ ReportMLC.getData(datasetName).getDataSet().numInstances());
			}
		}
		return csvCompoundInfo.get(datasetName);
	}

	public boolean hasCompoundInfoData()
	{
		return hasCompoundInfoData(datasetName);
	}

	public static boolean hasCompoundInfoData(String datasetName)
	{
		return new File(Settings.csvCompoundInfo(datasetName, ClusterEndpoint.numCompounds != 0)).exists();
	}

	public String[] getCompoundInfoFields()
	{
		return getCompoundInfoFields(datasetName);
	}

	public static String[] getCompoundInfoFields(String datasetName)
	{
		return ArrayUtil.removeAt(String.class, getCSVCompoundInfo(datasetName).getHeader(), 0);
	}

	public String getCompoundInfoValue(int compoundIndex, String f)
	{
		return getCompoundInfoValue(datasetName, compoundIndex, f);
	}

	public static String getCompoundInfoValue(String datasetName, int compoundIndex, String f)
	{
		CSVFile compoundInfo = getCSVCompoundInfo(datasetName);
		int colIndex = compoundInfo.getColumnIndex(f);
		if (colIndex == -1)
			throw new IllegalArgumentException("column " + f + " not found in compound info");
		return compoundInfo.content.get(compoundIndex + 1)[colIndex];
	}

	public String getClassRatio()
	{
		return getClassRatio(false);
	}

	public String getClassRatio(boolean withInterval)
	{
		List<Double> ratios = new ArrayList<Double>();
		for (int l = 0; l < numEndpoints; l++)
			ratios.add(ones_per_label[l] / (double) (ones_per_label[l] + zeros_per_label[l]));
		DoubleArraySummary summ = DoubleArraySummary.create(ratios);
		String s = summ.toString();
		if (withInterval)
			s += " [" + StringUtil.formatDouble(summ.getMin()) + "-" + StringUtil.formatDouble(summ.getMax()) + "]";
		return s;
	}

	DiscMethod discMethod;

	public Object getEndpointDiscDescription(boolean b, int l)
	{
		if (discMethod == null)
		{
			String s[] = datasetName.split("_");
			discMethod = DiscMethod.fromString(s[2]);
			if (discMethod == null)
				throw new Error("no disc method " + s[2]);
		}

		return discMethod.getEndpointDescription(b, l, this);

	}

	public String getLabelName(int label)
	{
		return dataset.getDataSet().attribute(dataset.getLabelIndices()[label]).name();
	}
}
