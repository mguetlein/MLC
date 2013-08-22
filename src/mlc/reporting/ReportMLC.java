package mlc.reporting;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import mlc.MLCDataInfo;
import mlc.MLCDataInfo.StudyDuration;
import mlc.ModelInfo;
import mlc.report.HTMLReport;
import mulan.data.InvalidDataFormatException;
import mulan.data.MultiLabelInstances;
import mulan.evaluation.Settings;
import mulan.evaluation.measure.ConfidenceLevel;
import mulan.evaluation.measure.ConfidenceLevelProvider;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.CategoryPlot;

import util.ArrayUtil;
import util.CollectionUtil;
import util.FileUtil;
import util.StringUtil;
import util.SwingUtil;
import weka.core.Attribute;
import weka.core.Instance;

import com.itextpdf.text.DocumentException;

import datamining.ResultSet;
import datamining.ResultSetIO;
import freechart.FreeChartUtil;

public class ReportMLC
{
	public HTMLReport report;

	//	private List<String> getLabelProperties()
	//	{
	//		Double numLabels = Double.parseDouble(results.getUniqueValue("num-labels") + "");
	//		List<String> l = new ArrayList<String>();
	//		for (int i = 0; i < numLabels; i++)
	//			l.add("label#" + i);
	//		return l;
	//	}

	String file;
	boolean wide;

	public ReportMLC(String outfile, String title)
	{
		this(outfile, title, true);
	}

	public ReportMLC(String outfile, String title, boolean wide)
	{
		try
		{
			file = outfile + ".html";
			this.wide = wide;
			//report = new PDFReport(outfile+".pdf", "Dataset Report");
			report = new HTMLReport(file, Settings.text("title"), Settings.text("header"), title, wide);
		}
		catch (Exception e)
		{
			throw new Error(e);
		}
	}

	public int getMaxPlotWidthNarrow()
	{
		return 960;
	}

	public String getOutfile()
	{
		return file;
	}

	public void close()
	{
		report.close(Settings.text("footer"));
		System.out.println("created report: " + file);
	}

	void addDatasetOverviewTable(String... datasetNames) throws InvalidDataFormatException, IOException,
			DocumentException
	{

		report.newSection("Datasets");
		report.addTable(getDatasetOverviewTable(datasetNames));
	}

	private ResultSet getDatasetOverviewTable(String... datasetNames) throws InvalidDataFormatException, IOException,
			DocumentException
	{
		ResultSet res = new ResultSet();
		for (String datasetName : datasetNames)
		{
			MLCDataInfo di = MLCDataInfo.get(getData(datasetName));
			int r = res.addResult();
			res.setResultValue(r, "dataset-name", di.datasetName);
			res.setResultValue(r, "endpoint-file", di.endpointFile);
			res.setResultValue(r, "feature-file", di.featureFile);
			res.setResultValue(r, "num-endpoints", di.numEndpoints);
			res.setResultValue(r, "num-features", di.dataset.getDataSet().numAttributes() - di.dataset.getNumLabels());
			res.setResultValue(r, "num-missing-allowed", di.numMissingAllowed);
			res.setResultValue(r, "num-instances", di.dataset.getNumInstances());
			res.setResultValue(r, "discretization-level", di.discretizationLevel);
			res.setResultValue(r, "include-V", di.includeV);
			//			res.setResultValue(r, "class-values (inactive/active)", di.getNonMissingClassValuesString());
			//			res.setResultValue(r, "missing-values", di.getMissingClassValue());
		}
		return res;
	}

	public static enum PerformanceMeasures
	{
		accuracy, fmeasure, auc, appdomain, all, all_ad, accuracy_confidence
	}

	public static String[] getSingleMacroPropNames(PerformanceMeasures measures)
	{
		switch (measures)
		{
			case accuracy:
				return new String[] { "accuracy", "auc" };
			case accuracy_confidence:
				return new String[] { "accuracy" };
			case appdomain:
				return new String[] { "accuracy", "auc", "inside-ad" };
			case auc:
				return new String[] { "auc" };
			case all:
				return new String[] { "accuracy", "auc", "sensitivity", "specificity", "ppv", "npv" };
			case all_ad:
				return new String[] { "accuracy", "auc", "sensitivity", "specificity", "ppv", "npv", "inside-ad" };
			case fmeasure:
				return new String[] { "f-measure" };
		}
		return null;
	}

	public static String[] getProps(PerformanceMeasures measures)
	{
		switch (measures)
		{
			case accuracy:
				return new String[] { "macro-accuracy", "weighted-macro-accuracy", "macro-auc", "weighted-macro-auc",
						"subset-accuracy" }; //"micro-accuracy", "1-hamming-loss"
			case accuracy_confidence:
				return new String[] { "macro-accuracy", "macro-accuracy-ch", "macro-accuracy-cm", "macro-accuracy-cl" }; //"micro-accuracy", "1-hamming-loss"
			case appdomain:
				return new String[] { "macro-accuracy", "weighted-macro-accuracy", "macro-auc", "weighted-macro-auc",
						"subset-accuracy", "macro-appdomain" }; //"micro-accuracy", "1-hamming-loss"
			case auc:
				return new String[] { "macro-auc", "weighted-macro-auc", "subset-accuracy" };
			case fmeasure:
				return new String[] { "micro-f-measure", "macro-f-measure", "weighted-macro-f-measure",
						"subset-accuracy" };
			case all:
				return new String[] { "macro-accuracy", "macro-auc", "macro-sensitivity", "macro-specificity",
						"macro-ppv", "macro-npv", "subset-accuracy", "macro-inside-ad" };//"weighted-macro-auc","weighted-macro-accuracy","weighted-macro-auc",
			case all_ad:
				return new String[] { "macro-accuracy", "macro-auc", "macro-sensitivity", "macro-specificity",
						"macro-ppv", "macro-npv", "subset-accuracy" };//"weighted-macro-auc","weighted-macro-accuracy","weighted-macro-auc",

				//				return new String[] { "micro-accuracy", "macro-accuracy", "weighted-macro-accuracy", "1-hamming-loss",
				//						"micro-auc", "macro-auc", "weighted-macro-auc", "micro-mcc", "macro-mcc", "weighted-macro-mcc",
				//						"micro-sensitivity", "macro-sensitivity", "micro-specificity", "macro-specificity",
				//						"subset-accuracy" };
		}
		return null;
	}

	public static ResultSet getInfo(PerformanceMeasures measures, String classZeroValue, String classOneValue)
	{
		ResultSet set = new ResultSet();

		for (String p : getProps(measures))
		{
			p = p.replaceAll("macro-", "");

			int r = set.addResult();
			set.setResultValue(r, "measure", p);
			for (String s : new String[] { "full-name", "synonyms", "description", "details" })
				set.setResultValue(r, s, Settings.text(p + "." + s, classZeroValue, classOneValue));
			//			case accuracy:
			//				s.add("micro-accuracy: overall accuracy (each single prediction)");
			//				s.add("macro-accuracy: accuracy averaged by endpoint");
			//				s.add("1-hamming-loss: accuracy averaged by compound");
			//				s.add("subset-accuracy: average number of compounds where all enpoints are predicted correctly");
			//				s.add();
			//				s.add("micro-accuracy > macro-accuracy: endpoints with few missing values are predicted better than endpoints with many missing values");
			//				s.add("micro-accuracy > 1-hamming-loss: compounds with few missing values are predicted better than compounds with many missing values");
			//				break;
			//			case auc:
			//				s.add("auc:");
			//				s.add("* area-under-(the-roc)-curve, performance measure suitable for unbalanced classes, range: 0 - 1, random guessing: 0.5, perfect prediction: 1.0");
			//				s.add("* auc is the probability that the classifier ranks a compound with class '" + classOneValue
			//						+ "' higher than with class '" + classZeroValue + "'");
			//				s.add("* in more detail: predictions are ranked according to probabilities(=confidences) "
			//						+ "given by the classifier for each prediction, i.e. first the compounds with high probability for class '"
			//						+ classOneValue
			//						+ "', than the compounds the classifier is unsure about, than the compounds with high probability for class '"
			//						+ classZeroValue + "'");
			//				s.add();
			//				s.add("micro-auc: auc computed with each single prediction");
			//				s.add("macro-auc: auc averaged by endpoint");
			//				s.add("subset-accuracy: average number of compounds where all enpoints are predicted correctly");
			//				s.add();
			//				s.add("micro-auc > macro-auc: compounds with few missing values are predicted better than compounds with many missing values");
			//				break;
			//			case fmeasure:
			//				s.add("f-measure: harmonic mean of 'precision' and 'recall', performance measure for in-balanced data with less active than in-active compounds, ignores correct in-active predictions (true negatives tn)");
			//				s.add("precision: same as 'positive predictive value', ratio of active compounds within comopunds that are classified as actives ( tp/(tp+fp) )");
			//				s.add("recall: same as 'sensitiviy', ratio of active-classified compounds within all active comopunds ( tp/(tp+fn) )");
			//				s.add();
			//				s.add("micro-f-measure: f-measure computed with each single prediction");
			//				s.add("macro-f-measure: f-measure averaged by endpoint");
			//				s.add("subset-accuracy: average number of compounds where all enpoints are predicted correctly");
			//				s.add();
			//				s.add("micro-f-measure > macro-f-measure: compounds with few missing values are predicted better than compounds with many missing values");
			//				break;
			//			case all:
			//				s.add("accuracy: the ratio of correct predictions");
			//				s.add("auc: the ratio of correct predictions");
			//				break;
		}
		return set;
	}

	//	public ReportMLC(String outfile, ResultSet results, PerformanceMeasures measures)
	//	{
	//		try
	//		{
	//			report = new HTMLReport(outfile + ".html", "Multi-Label-Classification (MLC) Results");
	//			String[] datasetNames = ArrayUtil.toArray(ListUtil.cast(String.class,
	//					results.getResultValues("dataset-name").values()));
	//			addDatasetOverviewTable(datasetNames);
	//
	//			CountedSet<String> classZero = new CountedSet<String>();
	//			CountedSet<String> classOne = new CountedSet<String>();
	//			for (Object datasetName : datasetNames)
	//			{
	//				MLCDataInfo di = MLCDataInfo.get(getData(datasetName.toString()));
	//				classZero.add(di.getClassValuesZero());
	//				classOne.add(di.getClassValuesOne());
	//			}
	//			//			if (classZero.size() != 1 || classOne.size() != 1)
	//			//				throw new IllegalStateException("take care of different class values");
	//			String classZeroStr = ArrayUtil.toString(ArrayUtil.toArray(classZero.values()), "/", "", "", "");
	//			String classOneStr = ArrayUtil.toString(ArrayUtil.toArray(classOne.values()), "/", "", "", "");
	//
	//			report.newSection("Performance measures");
	//			report.addParagraph(getInfo(measures, classZeroStr, classOneStr));
	//			report.newPage();
	//
	//			this.results = results;
	//			//			System.out.println(results.toNiceString());
	//
	//			for (int i = 0; i < results.getNumResults(); i++)
	//				if (results.getResultValue(i, "mlc-algorithm").toString().equals("BR"))
	//					results.setResultValue(i, "mlc-algorithm", "Single endpoint prediction");
	//				else if (results.getResultValue(i, "mlc-algorithm").toString().equals("ECC"))
	//					results.setResultValue(i, "mlc-algorithm", "Ensemble of classfier chains");
	//
	//			String compareProps[] = new String[] { "dataset-name", "mlc-algorithm", "mlc-algorithm-params",
	//					"classifier", "imputation", "app-domain", "app-domain-params" };
	//
	//			results.sortProperties(compareProps);
	//			for (String p : compareProps)
	//				results.sortResults(p);
	//			for (String p : getProps(measures))
	//				results.movePropertyBack(p);
	//			results.movePropertyBack("runtime");
	//
	//			String cmp1 = null;
	//			CountedSet<Object> cmpSet1 = null;
	//			String cmp2 = null;
	//			CountedSet<Object> cmpSet2 = null;
	//			for (String p : compareProps)
	//			{
	//				CountedSet<Object> set = results.getResultValues(p);
	//				if (set.size() > 1)
	//				{
	//					if (cmp1 == null)
	//					{
	//						cmp1 = p;
	//						cmpSet1 = set;
	//					}
	//					else if (cmp2 == null)
	//					{
	//						if (cmp1.equals("mlc-algorithm") && p.equals("mlc-algorithm-params")) // >1 mlc-alg, ignore different mlc-params
	//							continue;
	//						if (cmp1.equals("app-domain") && p.equals("app-domain-params"))
	//							continue;
	//						cmp2 = p;
	//						cmpSet2 = set;
	//					}
	//					else
	//					{
	//						if (cmp2.equals("mlc-algorithm") && p.equals("mlc-algorithm-params")) // >1 mlc-alg, ignore different mlc-params
	//							continue;
	//						if (cmp2.equals("app-domain") && p.equals("app-domain-params"))
	//							continue;
	//						throw new IllegalStateException("compare only two of those plz: "
	//								+ ArrayUtil.toString(compareProps) + ", different: " + cmp1 + " : " + cmpSet1 + ", "
	//								+ cmp2 + " : " + cmpSet2 + ", " + p + " : " + set);
	//					}
	//				}
	//			}
	//			if (cmp1 == null)
	//			{
	//				addBoxPlots(results, "mlc-algorithm", "", measures);
	//			}
	//			else if (cmp2 == null)
	//			{
	//				addBoxPlots(results, cmp1, "", measures);
	//			}
	//			else
	//			{
	//				for (Object val : cmpSet1.values())
	//				{
	//					ResultSet res = results.copy();
	//					res.exclude(cmp1, val);
	//					addBoxPlots(res, cmp2, " (" + cmp1 + ": " + val + ")", measures);
	//				}
	//				for (Object val : cmpSet2.values())
	//				{
	//					ResultSet res = results.copy();
	//					res.exclude(cmp2, val);
	//					addBoxPlots(res, cmp1, " (" + cmp2 + ": " + val + ")", measures);
	//				}
	//			}
	//			report.close();
	//
	//			System.out.println("report stored in " + outfile + ".html");
	//		}
	//		catch (Exception e)
	//		{
	//			e.printStackTrace();
	//		}
	//	}

	private void formatPlot(ChartPanel p)
	{
		StandardChartTheme chartTheme = (StandardChartTheme) StandardChartTheme.createJFreeTheme();
		Font extraLargeFont = new Font("Trebuchet MS", Font.PLAIN, chartTheme.getExtraLargeFont().getSize());
		Font largeFont = new Font("Trebuchet MS", Font.PLAIN, chartTheme.getLargeFont().getSize());
		Font regularFont = new Font("Trebuchet MS", Font.PLAIN, chartTheme.getRegularFont().getSize());
		Font smallFont = new Font("Trebuchet MS", Font.PLAIN, chartTheme.getSmallFont().getSize());
		chartTheme.setExtraLargeFont(extraLargeFont);
		chartTheme.setLargeFont(largeFont);
		chartTheme.setRegularFont(regularFont);
		chartTheme.setSmallFont(smallFont);
		chartTheme.setChartBackgroundPaint(Color.WHITE);
		chartTheme.setPlotBackgroundPaint(Color.decode("#eeeeee"));
		chartTheme.setRangeGridlinePaint(Color.decode("#eeeeee").darker().darker());
		chartTheme.apply(p.getChart());

		if (p.getChart().getLegend() != null)
			p.getChart().getLegend().setFrame(BlockBorder.NONE);
	}

	void addBoxPlots(ResultSet results, String compareProp, String titleSuffix, String fileSuffix,
			PerformanceMeasures measures) throws IOException, DocumentException
	{
		addBoxPlots(results, compareProp, titleSuffix, fileSuffix, measures, true);
	}

	void addBoxPlots(ResultSet results, String compareProp, String titleSuffix, String fileSuffix,
			PerformanceMeasures measures, boolean addPerEndpointPlots) throws IOException, DocumentException
	{
		String numCompounds = CollectionUtil.toString(results.getResultValues("num-compounds").values());
		String numLabels = CollectionUtil.toString(results.getResultValues("num-labels").values());
		int numCVSeeds = results.getResultValues("cv-seed").size();
		Double numCVFolds = Double.parseDouble(results.getUniqueValue("num-folds") + "");
		String title;
		String[] subtitle;
		if (results.getResultValues(compareProp).size() == 1)
		{
			title = "Performance measures" + titleSuffix;
			subtitle = new String[0];
		}
		else
		{
			report.newSection("Compare " + compareProp + titleSuffix);
			title = "Performance measures for different " + compareProp + titleSuffix;
			subtitle = new String[] { "compounds: " + numCompounds + ", labels: " + numLabels + ", " + numCVSeeds
					+ " x " + numCVFolds + "-fold CV" };
		}

		List<String> catProps = ArrayUtil.toList(getProps(measures));
		List<String> dispProps = new ArrayList<String>();
		for (String p : catProps)
			dispProps.add(p.replaceAll("macro-", ""));
		ChartPanel boxPlot1 = results.boxPlot(title, "Performance", subtitle, compareProp, catProps, dispProps, 0.05);
		formatPlot(boxPlot1);

		//	    }

		int extraHeight = 0;
		if (getProps(measures).length > 6)
		{
			CategoryPlot plot = (CategoryPlot) boxPlot1.getChart().getPlot();
			CategoryAxis xAxis = (CategoryAxis) plot.getDomainAxis();
			xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
			extraHeight += 75;
		}
		if (results.getResultValues(compareProp).size() == 1)
			extraHeight -= 100;

		//		{
		//			//			for (int i = 0; i < Double.parseDouble(results.getUniqueValue("num-labels") + ""); i++)
		//			//				catProps.add("macro-sensitivity#" + i);
		//			results.excludeProperties(catProps);
		//			System.out.println(results.toNiceString());
		//			System.exit(1);
		//		}

		ResultSet rs = results.join(ArrayUtil.toList(new String[] { compareProp }), null, catProps);
		List<String> tableProps = new ArrayList<String>();
		if (results.getResultValues(compareProp).size() > 1)
			tableProps.add(compareProp);
		tableProps.addAll(ArrayUtil.toList(getProps(measures)));
		if (results.getResultValues(compareProp).size() > 1)
			tableProps.add("runtime");
		rs.excludeProperties(tableProps);
		for (String p : tableProps)
		{
			rs.movePropertyBack(p);
			rs.setNicePropery(p, report.encodeLink("#" + p.replace("macro-", ""), p.replace("macro-", "")));
		}

		report.addParagraph(Settings.text("macro-measures.description"));
		report.addGap();
		report.addTable(rs);
		report.addGap();
		addImage("boxplot_" + measures + "_" + compareProp + "_" + fileSuffix, boxPlot1, new Dimension(wide ? 1800
				: getMaxPlotWidthNarrow(), 400 + extraHeight));

		//		if (getProps(measures).length > 3)
		//		{
		//			CategoryPlot plot = (CategoryPlot) boxPlot1.getChart().getPlot();
		//			CategoryAxis xAxis = (CategoryAxis) plot.getDomainAxis();
		//			xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
		//		}
		//		FreeChartUtil.toFile(new File("/home/martin/tmp/boxplot_" + compareProp + "_" + measures + ".png"), boxPlot1,
		//				new Dimension(600, getProps(measures).length > 3 ? 400 : 300));
		//		System.out.println("stored: " + "/home/martin/tmp/boxplot_" + compareProp + "_" + measures + ".png");

		//		if (results.getResultValues("dataset-name").size() == 1)
		//		{

		if (addPerEndpointPlots)
			for (String prop : getSingleMacroPropNames(measures))
			{
				Double numLabelsInt = Double.parseDouble(results.getUniqueValue("num-labels") + "");
				catProps.clear();
				for (int i = 0; i < numLabelsInt; i++)
					catProps.add("macro-" + prop + "#" + i);
				List<String> catPropsDisp = new ArrayList<String>();
				for (int i = 0; i < numLabelsInt; i++)
					catPropsDisp.add(results.getUniqueValue("label#" + i).toString());
				System.out.println("Endpoint " + prop + " for different " + compareProp + titleSuffix);
				ChartPanel boxPlot2 = results.boxPlot("Endpoint " + prop + " for different " + compareProp
						+ titleSuffix, "Performance", new String[] { "compounds: " + numCompounds + ", labels: "
						+ numLabels + ", " + numCVSeeds + " x " + numCVFolds + "-fold CV" }, compareProp, catProps,
						catPropsDisp, 0.05);
				formatPlot(boxPlot2);
				CategoryPlot plot = (CategoryPlot) boxPlot2.getChart().getPlot();
				CategoryAxis xAxis = (CategoryAxis) plot.getDomainAxis();
				xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
				addImage("boxplot_perLabel-" + prop + "_" + compareProp + "_" + fileSuffix, boxPlot2, new Dimension(
						1800, 800));
			}

		//			ResultSet rs2 = results.join(ArrayUtil.toList(new String[] { compareProp }), null, catProps);
		//			rs2.excludeProperties(ArrayUtil.toList(ArrayUtil.concat(new String[] { compareProp },
		//					ArrayUtil.toArray(catProps))));
		//			tables = ArrayUtil.concat(tables, new ResultSet[] { rs2 });
		//		}

	}

	private static HashMap<String, MultiLabelInstances> dataMap = new HashMap<String, MultiLabelInstances>();

	public static MultiLabelInstances getData(String datasetName)
	{
		if (!dataMap.containsKey(datasetName))
		{
			System.out.println("reading " + datasetName);
			try
			{
				dataMap.put(datasetName,
						new MultiLabelInstances(Settings.arffFile(datasetName), Settings.xmlFile(datasetName)));
			}
			catch (InvalidDataFormatException e)
			{
				throw new Error(e);
			}
		}
		return dataMap.get(datasetName);
	}

	//	private void addDatasetInfo(String datasetName) throws InvalidDataFormatException, IOException, DocumentException
	//	{
	//		addDatasetInfo(getDatasetInfo(datasetName));
	//	}

	private void addDatasetInfo(String datasetName) throws InvalidDataFormatException, IOException, DocumentException
	{
		MLCDataInfo di = MLCDataInfo.get(getData(datasetName));
		report.newSection("Dataset " + di.datasetName);
		report.addTable(getDatasetOverviewTable(di.datasetName));
		report.addTable(di.getMissingPerLabel());

		addImage(datasetName + "_missingPerLabel", di.plotMissingPerLabel(), new Dimension(1200, 800));

		addImage(datasetName + "_missingPerCompound", di.plotMissingPerCompound(), new Dimension(1200, 600));

		addImage(datasetName + "_correlationHistogramm", di.plotCorrelationHistogramm(), new Dimension(1200, 600));

		if (di.hasRealData())
			addImage(datasetName + "correlationMatrixReal", di.plotCorrelationMatrix(false), new Dimension(1200, 1150));

		addImage(datasetName + "_correlationMatrixClass", di.plotCorrelationMatrix(true), new Dimension(1200, 1150));

		//		di.plotCorrelationMatrix(true, csv, true);

		//		List<String> smallImages = new ArrayList<String>();
		//		if (!di.includeV && di.hasRealData())
		//		{
		//			for (int j = 0; j < di.dataset.getNumLabels(); j++)
		//			{
		//				smallImages.add(createImage(datasetName + "_realValueHistogramm_" + j,
		//						di.plotRealValueHistogram(j, false), new Dimension(600, 300)));
		//				smallImages.add(createImage(datasetName + "_realValueHistogrammZoom_" + j,
		//						di.plotRealValueHistogram(j, true), new Dimension(600, 300)));
		//			}
		//			report.addSmallImages(ArrayUtil.toArray(smallImages));
		//		}

	}

	public static void chooseWithDialog() throws Exception
	{
		File results[] = Settings.listResultFiles();
		File datasets[] = Settings.listArffFiles();
		DefaultTableModel m = new DefaultTableModel()
		{
			public java.lang.Class<?> getColumnClass(int columnIndex)
			{
				if (columnIndex == 0)
					return String.class;
				if (columnIndex == 1)
					return String.class;
				if (columnIndex == 2)
					return Long.class;
				return null;
			}

			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};
		final JTable t = new JTable(m);
		t.setDefaultRenderer(Long.class, new DefaultTableCellRenderer()
		{
			SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column)
			{
				if (value instanceof Long)
					value = f.format(new Date((Long) value));
				return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			}
		});
		m.addColumn("File");
		m.addColumn("Type");
		m.addColumn("Date");
		List<File> files = ArrayUtil.toList(ArrayUtil.concat(results, datasets));
		Collections.sort(files, new Comparator<File>()
		{
			@Override
			public int compare(File o1, File o2)
			{
				return (int) (o2.lastModified() - o1.lastModified());
			}
		});
		for (File file : files)
			m.addRow(new Object[] { file.getName(), ArrayUtil.indexOf(results, file) == -1 ? "dataset" : "result",
					file.lastModified() });
		packColumn(t, 0, 5);
		packColumn(t, 1, 5);
		packColumn(t, 2, 5);
		final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<DefaultTableModel>();
		t.setRowSorter(sorter);
		sorter.setModel(m);
		JScrollPane s = new JScrollPane(t);
		t.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() > 1)
					((JDialog) t.getTopLevelAncestor()).setVisible(false);
			}
		});
		t.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
					((JDialog) t.getTopLevelAncestor()).setVisible(false);
			}
		});
		t.setRowSelectionInterval(0, 0);
		SwingUtil.showInDialog(s, "Create Report", new Dimension(650, 800));

		List<File> selectedDatasets = new ArrayList<File>();
		List<File> selectedResults = new ArrayList<File>();
		for (int r : t.getSelectedRows())
		{
			int row = sorter.convertRowIndexToModel(r);
			File f = files.get(row);
			if (ArrayUtil.indexOf(datasets, f) == -1)
				selectedResults.add(f);
			else
				selectedDatasets.add(f);
		}

		if (selectedDatasets.size() > 0)
		{
			String names[] = new String[selectedDatasets.size()];
			for (int i = 0; i < names.length; i++)
			{
				String name = selectedDatasets.get(i).getName();
				names[i] = name.replace(".arff", "");
			}
			System.out.println("create dataset report for " + ArrayUtil.toString(names));
			datasetReport("dataset_report_" + ArrayUtil.toString(names, "-", "", "", ""), names);
		}
		if (selectedResults.size() > 0)
		{
			for (File resultFile : selectedResults)
			{
				String name = resultFile.getName().replace(".results", "");
				System.out.println("create result report for " + name);
				System.out.println("reading results:");
				ResultSet rs = ResultSetIO.parseFromFile(new File("results/" + name + ".results"));
				PerformanceMeasures measures = PerformanceMeasures.accuracy;
				MultiValidationReport.multiValidationReport(name + "_" + measures + "_report", name, rs, measures);
			}
		}
	}

	public static void datasetReport(String datasetNames[]) throws Exception
	{
		datasetReport(Settings.reportFile(datasetNames), datasetNames);
	}

	public static void datasetReport(String outfile, String datasetNames[]) throws Exception
	{
		if (datasetNames == null || datasetNames.length == 0)
			throw new IllegalArgumentException("please give a least one dataset name (-d, comma seperated)");

		ReportMLC rep = new ReportMLC(outfile, "Dataset Report");
		if (datasetNames.length > 1)
			rep.addDatasetOverviewTable(datasetNames);
		for (String datasetName : datasetNames)
			rep.addDatasetInfo(datasetName);
		rep.close();

	}

	public static void endpointTableFromModel(String modelName) throws Exception
	{
		String datasetName = ModelInfo.get(modelName).getDataset();
		String reportFile = Settings.endpointTableFile(modelName);
		endpointTable(datasetName, reportFile, modelName);
	}

	public static void endpointTableFromDataset(String datasetName) throws Exception
	{
		String reportFile = Settings.reportFileEndpoints(datasetName);
		endpointTable(datasetName, reportFile, null);
	}

	public static void endpointTable(String datasetName, String reportFile, String modelName) throws Exception
	{
		MultiLabelInstances data = getData(datasetName);
		MLCDataInfo di = MLCDataInfo.get(data);

		ReportMLC rep = new ReportMLC(reportFile, "Predicted endpoints", di.hasRealData() && di.hasCompoundInfoData());
		String info;
		if (modelName != null)
			info = "This is the predicted enpoint table for model " + rep.report.encodeLink(".", modelName) + ".";
		else
			info = "This is the predicted enpoint table for dataset " + datasetName + ".";
		rep.report.addParagraph(info);
		rep.report.addGap();

		ResultSet res = new ResultSet();
		for (int l = 0; l < data.getNumLabels(); l++)
		{
			Attribute a = data.getDataSet().attribute(data.getLabelIndices()[l]);
			String labelName = a.name();
			int r = res.addResult();
			res.setResultValue(r, "endpoint", labelName);
			res.setResultValue(r, "#active", di.ones_per_label[l]);
			res.setResultValue(r, "#inactive", di.zeros_per_label[l]);
			res.setResultValue(r, "#missing", di.missings_per_label[l]);
			if (di.hasRealData())
			{
				//				DoubleArraySummary active = DoubleArraySummary.create(di.getRealValues(labelName, "1"));
				//				DoubleArraySummary inactive = DoubleArraySummary.create(di.getRealValues(labelName, "0"));
				//				res.setResultValue(r, "mmol active range", StringUtil.formatDouble(active.getMin()) + " - "
				//						+ StringUtil.formatDouble(active.getMax()));
				//				res.setResultValue(r, "mmol inactive range", StringUtil.formatDouble(inactive.getMin()) + " - "
				//						+ StringUtil.formatDouble(inactive.getMax()));
				res.setResultValue(r, "active", di.getEndpointDiscDescription(true, l));
				res.setResultValue(r, "inactive", di.getEndpointDiscDescription(false, l));

				String uri = rep.createImage(datasetName + "_realValueHistogrammZoom_" + l,
						di.plotRealValueHistogram(l, true), new Dimension(600, 300));
				res.setResultValue(r, "histogram", rep.report.getImage(uri, 300, 150));

				if (di.hasCompoundInfoData())
				{
					List<Double> acc = di.getStudyDurationValues(l, StudyDuration.accute);
					res.setResultValue(r, "#sub-" + StudyDuration.accute, acc.size());// + " " + DoubleArraySummary.create(acc));
					List<Double> chr = di.getStudyDurationValues(l, StudyDuration.chronic);
					res.setResultValue(r, "#sub-" + StudyDuration.chronic, chr.size());// + " " + DoubleArraySummary.create(chr));
					String uri2 = rep.createImage(datasetName + "_realValueStudyDurationHistogrammZoom_" + l,
							di.plotRealValueStudyDurationHistogram(l, true), new Dimension(600, 300));
					res.setResultValue(r, "histogram-study", rep.report.getImage(uri2, 300, 150));

					//					List<Double> inh = di.getRouteValues(l, Route.inhalation);
					//					res.setResultValue(r, Route.inhalation.toString(),
					//							inh.size() + " " + DoubleArraySummary.create(inh));
					//					List<Double> oral = di.getRouteValues(l, Route.oral);
					//					res.setResultValue(r, Route.oral.toString(), oral.size() + " " + DoubleArraySummary.create(oral));

				}
			}

		}
		rep.report.addTable(res);

		rep.close();
	}

	public static void compoundTable(String modelName) throws Exception
	{
		ModelInfo mi = ModelInfo.get(modelName);
		ReportMLC rep = new ReportMLC(Settings.compoundTableFile(modelName), "Training dataset compounds");
		rep.report.addParagraph("This is the training dataset compound table for model "
				+ rep.report.encodeLink(".", modelName) + ".");
		rep.report.addGap();

		ResultSet res = new ResultSet();
		MultiLabelInstances data = getData(mi.getDataset());
		MLCDataInfo di = MLCDataInfo.get(data);

		for (int i = 0; i < data.getNumInstances(); i++)
		{
			Instance instance = data.getDataSet().get(i);
			int r = res.addResult();
			res.setResultValue(r, "Index", i + 1);

			res.setResultValue(r, "SMILES", di.getSmiles(i));
			res.setResultValue(r, "InChI", di.getInchi(i));

			if (di.hasCompoundInfoData())
				for (String f : di.getCompoundInfoFields())
					res.setResultValue(r, f, di.getCompoundInfoValue(i, f));

			for (int l = 0; l < data.getNumLabels(); l++)
			{
				String labelName = data.getDataSet().attribute(data.getLabelIndices()[l]).name();

				String val;
				if (instance.isMissing(data.getLabelIndices()[l]))
					val = MLCDataInfo.MISSING;
				else if (instance.value(data.getLabelIndices()[l]) == 0)
					val = MLCDataInfo.INACTIVE;
				else if (instance.value(data.getLabelIndices()[l]) == 1)
					val = MLCDataInfo.ACTIVE;
				else
					throw new Error("WTF");

				if (di.hasRealData())
					if (!instance.isMissing(data.getLabelIndices()[l]))
						val += " (" + StringUtil.formatDouble(di.getRealValue(i, labelName)) + ")";

				res.setResultValue(r, labelName, val);
			}
		}
		rep.report.addTable(res);

		rep.close();
	}

	public static void modelReport()
	{
		throw new Error("not yet implemeted");
	}

	public static void main(String args[]) throws Exception
	{
		if (args == null || args.length == 0)
		{
			//			Settings.PWD = "/home/martin/workspace/BMBF-MLC";
			chooseWithDialog();
		}
		System.exit(0);

		//
		//		System.out.println("reading results:");
		//		//String infile = "ECC_BR_dataA-dataB-dataC";
		//		//String infile = "BR_alg_dataC";
		//		//String infile = "ECC_BR_cpdb";
		//		String infile = "BR_IBk_dataAsmall";
		//		//		String infile = "BR_ECC_dataAdisc-dataAclusterH-dataAclusterL";
		//		//String infile = "BR_ECC_dataAdisc-dataAclusterB";
		//		//		String infile = "BR_ECC_dataApc-dataAfp1-dataAfp2-dataAfp3-dataAfp4";
		//		//		String infile = "ECC_imputation_dataAsmall";
		//		//		String infile = "imputation_dataA";
		//		ResultSet rs = ResultSetIO.parseFromFile(new File("tmp/" + infile + ".results"));
		//		System.out.println(rs.getNumResults() + " single results, creating report");
		//		PerformanceMeasures measures = PerformanceMeasures.auc;
		//		new ReportMLC(infile + "_" + measures + "_report.pdf", rs, measures);

		//		//		new ReportMLC("dataset_report.pdf", "dataA", "dataB", "dataC", "dataD");
		//		//new ReportMLC("dataset_report_dataDall.pdf", "dataAsmallC", "dataAsmall");
		//		//new ReportMLC("dataset_report_dataA_pc_fp.pdf", "dataApc", "dataAfp1", "dataAfp2", "dataAfp3", "dataAfp4");
		//		//				new ReportMLC("dataset_report_dataA_disc.pdf", "dataAdisc", "dataAclusterB");
		//		new ReportMLC("dataset_report_dataAV.pdf", "dataAV", "dataA");
		//		//				new ReportMLC("dataset_report.pdf", "cpdb");

		//		List<String> equalProps = ArrayUtil.toList(new String[] { "cv-seed" });
		//		List<String> ommitProps = ArrayUtil.toList(new String[] { "label#0", "label#1", "label#2" });
		//		List<String> varianceProps = ArrayUtil.toList(new String[] {});
		//
		//		Double numCompounds = Double.parseDouble(rs.getUniqueValue("num-compounds") + "");
		//		Double numLabels = Double.parseDouble(rs.getUniqueValue("num-labels") + "");
		//		int numCVSeeds = rs.getResultValues("cv-seed").size();
		//		Double numCVFolds = Double.parseDouble(rs.getUniqueValue("num-folds") + "");
		//
		//		List<String> catProps = ArrayUtil.toList(new String[] { "hamming-loss", "macro-accuracy", "micro-accuracy",
		//				"subset-accuracy" });
		//		rs.showBoxPlot("Performance for different MLC-algorithms", "Performance", new String[] { "compounds: "
		//				+ numCompounds + ", labels: " + numLabels + ", " + numCVSeeds + " x " + numCVFolds + "-fold CV" },
		//				"mlc-algorithm", catProps);
		//
		//		catProps = ArrayUtil.toList(new String[] { "macro-accuracy#0", "macro-accuracy#1", "macro-accuracy#2" });
		//		List<String> catPropsDisp = ArrayUtil.toList(new String[] { rs.getUniqueValue("label#0") + "",
		//				rs.getUniqueValue("label#1") + "", rs.getUniqueValue("label#2") + "" });
		//		rs.showBoxPlot("Performance for different MLC-algorithms", "Performance", new String[] { "compounds: "
		//				+ numCompounds + ", labels: " + numLabels + ", " + numCVSeeds + " x " + numCVFolds + "-fold CV" },
		//				"mlc-algorithm", catProps, catPropsDisp);
		//
		//		ResultSet rs2 = rs.join(equalProps, ommitProps, varianceProps);
		//		System.out.println(rs2.toNiceString());
		//
		//		//		List<String> catProps = ArrayUtil.toList(new String[] { "hamming-loss", "macro-accuracy" });
		//		//		rs2.showBarPlot("test", "yAxis", "cv-seed", catProps, null, null);

	}

	public static int packColumn(JTable table, int vColIndex, int margin)
	{
		return packColumn(table, vColIndex, margin, Integer.MAX_VALUE);
	}

	public static int packColumn(JTable table, int vColIndex, int margin, int max)
	{
		DefaultTableColumnModel colModel = (DefaultTableColumnModel) table.getColumnModel();
		TableColumn col = colModel.getColumn(vColIndex);
		int width = 0;

		// Get width of column header
		TableCellRenderer renderer = col.getHeaderRenderer();
		if (renderer == null)
		{
			renderer = table.getTableHeader().getDefaultRenderer();
		}
		Component comp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0);
		width = comp.getPreferredSize().width;

		// Get maximum width of column data
		for (int r = 0; r < table.getRowCount(); r++)
		{
			renderer = table.getCellRenderer(r, vColIndex);
			comp = renderer.getTableCellRendererComponent(table, table.getValueAt(r, vColIndex), false, false, r,
					vColIndex);
			width = Math.max(width, comp.getPreferredSize().width);
		}

		// Add margin
		width += 2 * margin;
		if (width > max)
			width = max;

		// Set the width
		col.setPreferredWidth(width);
		//		col.setMinWidth(width);
		//		col.setMaxWidth(width);
		return width;
	}

	public String createImage(String imageName, ChartPanel plot, Dimension dim)
	{
		String dir = FileUtil.getParent(getOutfile());
		System.out.println("created image: " + FreeChartUtil.toFile(dir + "/images/" + imageName + ".png", plot, dim));
		return "images/" + imageName + ".png";
	}

	public void addImage(String imageName, ChartPanel plot, Dimension dim)
	{
		report.addImage(createImage(imageName, plot, dim));
	}

	public void addImage(String imageName, JPanel p, Dimension dim)
	{
		String dir = FileUtil.getParent(getOutfile());
		System.out.println("created image: " + SwingUtil.toFile(dir + "/images/" + imageName + ".png", p, dim));
		report.addImage("images/" + imageName + ".png");
	}

	public void addSingleEndpointConfidencePlot(ResultSet results, int l, String labelName, PerformanceMeasures measures)
	{
		List<String> catProps = new ArrayList<String>();
		List<String> dispProps = ArrayUtil.toList(ReportMLC.getSingleMacroPropNames(measures));
		for (String p : ReportMLC.getSingleMacroPropNames(measures))
			catProps.add("macro-" + p + "#" + l);

		String confStr = "model confidence";
		ResultSet rs = new ResultSet();
		for (ConfidenceLevel confLevel : ConfidenceLevelProvider.LEVELS)
		{
			for (int i = 0; i < results.getNumResults(); i++)
			{
				int n = rs.addResult();
				if (confLevel == ConfidenceLevelProvider.CONFIDENCE_LEVEL_ALL)
					rs.setResultValue(n, confStr, "all predictions (" + confLevel.getNiceName() + ")");
				else
					rs.setResultValue(n, confStr, "predictions with " + confLevel.getNiceName());

				for (int p = 0; p < catProps.size(); p++)
				{
					String prop = catProps.get(p) + confLevel.getShortName();
					String pNice = dispProps.get(p);
					if (results.getResultValue(i, prop) == null)
						throw new Error("result value is null: " + prop);
					Double val = (Double) results.getResultValue(i, prop) * 100;
					rs.setResultValue(n, pNice, val);
				}
			}
		}

		//			System.out.println(rs.toNiceString());
		//			System.out.println(rs);

		rs.setNicePropery(confStr, report.encodeLink("description#model-confidence", confStr));
		for (String p : dispProps)
			rs.setNicePropery(p, report.encodeLink("#" + p, p));

		report.addTable(rs.join(confStr));

		report.addGap();

		ChartPanel boxPlot = rs.boxPlot("Performance for endpoint " + labelName, "Performance", null, confStr,
				dispProps, null, 5.0);
		formatPlot(boxPlot);
		//			ChartPanel boxPlot = results.boxPlot("Performance for endpoint " + labelName, "Performance", null,
		//					"dataset-name", catProps, dispProps, 0.05);

		addImage("validation_boxplot_" + labelName + "_" + measures, boxPlot, new Dimension(getMaxPlotWidthNarrow(),
				400));

		//			for (ConfidenceLevel confLevel : ConfidenceLevelProvider.LEVELS)
		//			{
		//				rep.report.addTable(ResultSet.build(di.getConfusionMatrix(joined, l, confLevel)), "Confusion Matrix "
		//						+ confLevel.getName());
		//			}

		//			if (l > 2)
		//				break;
	}

}
