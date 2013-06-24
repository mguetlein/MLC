import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import mulan.data.InvalidDataFormatException;
import mulan.data.MultiLabelInstances;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;

import util.ArrayUtil;
import util.CollectionUtil;
import util.CountedSet;
import util.FileUtil.CSVFile;
import util.ListUtil;
import util.StringLineAdder;
import util.SwingUtil;
import weka.core.Attribute;

import com.itextpdf.text.DocumentException;

import datamining.ResultSet;
import datamining.ResultSetIO;
import freechart.FreeChartUtil;
import freechart.HistogramPanel;

public class ReportMLC
{
	ResultSet results;
	Report report;

	//	private List<String> getLabelProperties()
	//	{
	//		Double numLabels = Double.parseDouble(results.getUniqueValue("num-labels") + "");
	//		List<String> l = new ArrayList<String>();
	//		for (int i = 0; i < numLabels; i++)
	//			l.add("label#" + i);
	//		return l;
	//	}

	public ReportMLC(String outfile, String... datasetNames)
	{
		try
		{
			report = new Report(outfile, "Dataset Report");

			addDatasetTable(datasetNames);
			report.newPage();
			for (String datasetName : datasetNames)
				addDatasetInfo(datasetName);

			report.close();
			System.out.println("\nreport created:\n" + outfile);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void addDatasetTable(String... datasetNames) throws InvalidDataFormatException, IOException,
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
			res.setResultValue(r, "class-values", di.getNonMissingClassValuesString());
			res.setResultValue(r, "missing-values", di.getMissingClassValue());
		}
		report.addSection("Datasets", "", new ResultSet[] { res }, null, null);
	}

	public static enum PerformanceMeasures
	{
		accuracy, fmeasure, auc, all
	}

	public static String[] getSingleMacroPropNames(PerformanceMeasures measures)
	{
		switch (measures)
		{
			case accuracy:
				return new String[] { "accuracy", "auc" };
			case auc:
				return new String[] { "auc" };
			case all:
				return new String[] { "accuracy", "auc", "mcc", "sensitivity", "specificity" };
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
			case auc:
				return new String[] { "macro-auc", "weighted-macro-auc", "subset-accuracy" };
			case fmeasure:
				return new String[] { "micro-f-measure", "macro-f-measure", "weighted-macro-f-measure",
						"subset-accuracy" };
			case all:
				return new String[] { "micro-accuracy", "macro-accuracy", "weighted-macro-accuracy", "1-hamming-loss",
						"micro-auc", "macro-auc", "weighted-macro-auc", "micro-mcc", "macro-mcc", "weighted-macro-mcc",
						"micro-sensitivity", "macro-sensitivity", "micro-specificity", "macro-specificity",
						"subset-accuracy" };
		}
		return null;
	}

	public static String getInfo(PerformanceMeasures measures, String classZeroValue, String classOneValue)
	{
		StringLineAdder s = new StringLineAdder();
		switch (measures)
		{
			case accuracy:
				s.add("micro-accuracy: overall accuracy (each single prediction)");
				s.add("macro-accuracy: accuracy averaged by endpoint");
				s.add("1-hamming-loss: accuracy averaged by compound");
				s.add("subset-accuracy: average number of compounds where all enpoints are predicted correctly");
				s.add();
				s.add("micro-accuracy > macro-accuracy: endpoints with few missing values are predicted better than endpoints with many missing values");
				s.add("micro-accuracy > 1-hamming-loss: compounds with few missing values are predicted better than compounds with many missing values");
				break;
			case auc:
				s.add("auc:");
				s.add("* area-under-(the-roc)-curve, performance measure suitable for unbalanced classes, range: 0 - 1, random guessing: 0.5, perfect prediction: 1.0");
				s.add("* auc is the probability that the classifier ranks a compound with class '" + classOneValue
						+ "' higher than with class '" + classZeroValue + "'");
				s.add("* in more detail: predictions are ranked according to probabilities(=confidences) "
						+ "given by the classifier for each prediction, i.e. first the compounds with high probability for class '"
						+ classOneValue
						+ "', than the compounds the classifier is unsure about, than the compounds with high probability for class '"
						+ classZeroValue + "'");
				s.add();
				s.add("micro-auc: auc computed with each single prediction");
				s.add("macro-auc: auc averaged by endpoint");
				s.add("subset-accuracy: average number of compounds where all enpoints are predicted correctly");
				s.add();
				s.add("micro-auc > macro-auc: compounds with few missing values are predicted better than compounds with many missing values");
				break;
			case fmeasure:
				s.add("f-measure: harmonic mean of 'precision' and 'recall', performance measure for in-balanced data with less active than in-active compounds, ignores correct in-active predictions (true negatives tn)");
				s.add("precision: same as 'positive predictive value', ratio of active compounds within comopunds that are classified as actives ( tp/(tp+fp) )");
				s.add("recall: same as 'sensitiviy', ratio of active-classified compounds within all active comopunds ( tp/(tp+fn) )");
				s.add();
				s.add("micro-f-measure: f-measure computed with each single prediction");
				s.add("macro-f-measure: f-measure averaged by endpoint");
				s.add("subset-accuracy: average number of compounds where all enpoints are predicted correctly");
				s.add();
				s.add("micro-f-measure > macro-f-measure: compounds with few missing values are predicted better than compounds with many missing values");
				break;
			case all:
				break;
		}
		return s.toString();
	}

	public ReportMLC(String outfile, ResultSet results, PerformanceMeasures measures)
	{
		try
		{
			report = new Report(outfile, "Multi-Label-Classification (MLC) Results");
			String[] datasetNames = ArrayUtil.toArray(ListUtil.cast(String.class,
					results.getResultValues("dataset-name").values()));
			addDatasetTable(datasetNames);

			CountedSet<String> classZero = new CountedSet<String>();
			CountedSet<String> classOne = new CountedSet<String>();
			for (Object datasetName : datasetNames)
			{
				MLCDataInfo di = MLCDataInfo.get(getData(datasetName.toString()));
				classZero.add(di.getClassValuesZero());
				classOne.add(di.getClassValuesOne());
			}
			if (classZero.size() != 1 || classOne.size() != 1)
				throw new IllegalStateException("take care of different class values");

			report.addSection("Performance measures",
					getInfo(measures, classZero.values().get(0), classOne.values().get(0)), null, null, null);
			report.newPage();

			this.results = results;
			//			System.out.println(results.toNiceString());

			for (int i = 0; i < results.getNumResults(); i++)
				if (results.getResultValue(i, "mlc-algorithm").toString().equals("BR"))
					results.setResultValue(i, "mlc-algorithm", "Single endpoint prediction");
				else if (results.getResultValue(i, "mlc-algorithm").toString().equals("ECC"))
					results.setResultValue(i, "mlc-algorithm", "Ensemble of classfier chains");

			String compareProps[] = new String[] { "dataset-name", "mlc-algorithm", "mlc-algorithm-params",
					"classifier", "imputation" };

			results.sortProperties(compareProps);
			for (String p : compareProps)
				results.sortResults(p);
			for (String p : getProps(measures))
				results.movePropertyBack(p);
			results.movePropertyBack("runtime");

			String cmp1 = null;
			CountedSet<Object> cmpSet1 = null;
			String cmp2 = null;
			CountedSet<Object> cmpSet2 = null;
			for (String p : compareProps)
			{
				CountedSet<Object> set = results.getResultValues(p);
				if (set.size() > 1)
				{
					if (cmp1 == null)
					{
						cmp1 = p;
						cmpSet1 = set;
					}
					else if (cmp2 == null)
					{
						if (cmp1.equals("mlc-algorithm")) // >1 mlc-alg, ignore different mlc-params
							continue;
						cmp2 = p;
						cmpSet2 = set;
					}
					else
						throw new IllegalStateException("compare only two of those plz: "
								+ ArrayUtil.toString(compareProps));
				}
			}
			if (cmp1 == null)
			{
				addBoxPlots(results, "mlc-algorithm", "", measures);
			}
			else if (cmp2 == null)
			{
				addBoxPlots(results, cmp1, "", measures);
			}
			else
			{
				for (Object val : cmpSet1.values())
				{
					ResultSet res = results.copy();
					res.exclude(cmp1, val);
					addBoxPlots(res, cmp2, " (" + cmp1 + ": " + val + ")", measures);
				}
				for (Object val : cmpSet2.values())
				{
					ResultSet res = results.copy();
					res.exclude(cmp2, val);
					addBoxPlots(res, cmp1, " (" + cmp2 + ": " + val + ")", measures);
				}
			}
			report.close();

			System.out.println("report stored in " + outfile);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void addBoxPlots(ResultSet results, String compareProp, String titleSuffix, PerformanceMeasures measures)
			throws IOException, DocumentException
	{
		String numCompounds = CollectionUtil.toString(results.getResultValues("num-compounds").values());
		String numLabels = CollectionUtil.toString(results.getResultValues("num-labels").values());
		int numCVSeeds = results.getResultValues("cv-seed").size();
		Double numCVFolds = Double.parseDouble(results.getUniqueValue("num-folds") + "");

		List<String> catProps = ArrayUtil.toList(getProps(measures));
		ChartPanel boxPlot1 = results.boxPlot("Performance for different " + compareProp + titleSuffix, "Performance",
				new String[] { "compounds: " + numCompounds + ", labels: " + numLabels + ", " + numCVSeeds + " x "
						+ numCVFolds + "-fold CV" }, compareProp, catProps, null, 0.05);
		if (getProps(measures).length > 6)
		{
			CategoryPlot plot = (CategoryPlot) boxPlot1.getChart().getPlot();
			CategoryAxis xAxis = (CategoryAxis) plot.getDomainAxis();
			xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
		}
		File images[] = new File[] { FreeChartUtil.toTmpFile(boxPlot1, new Dimension(1200, 600)) };
		//		if (getProps(measures).length > 3)
		//		{
		//			CategoryPlot plot = (CategoryPlot) boxPlot1.getChart().getPlot();
		//			CategoryAxis xAxis = (CategoryAxis) plot.getDomainAxis();
		//			xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
		//		}
		//		FreeChartUtil.toFile(new File("/home/martin/tmp/boxplot_" + compareProp + "_" + measures + ".png"), boxPlot1,
		//				new Dimension(600, getProps(measures).length > 3 ? 400 : 300));
		//		System.out.println("stored: " + "/home/martin/tmp/boxplot_" + compareProp + "_" + measures + ".png");

		ResultSet rs = results.join(ArrayUtil.toList(new String[] { compareProp }), null, catProps);
		rs.excludeProperties(ArrayUtil.toList(ArrayUtil.concat(new String[] { compareProp }, getProps(measures),
				new String[] { "runtime" })));
		ResultSet tables[] = new ResultSet[] { rs };

		//		if (results.getResultValues("dataset-name").size() == 1)
		//		{

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
			ChartPanel boxPlot2 = results
					.boxPlot("Endpoint " + prop + " for different " + compareProp + titleSuffix, "Performance",
							new String[] { "compounds: " + numCompounds + ", labels: " + numLabels + ", " + numCVSeeds
									+ " x " + numCVFolds + "-fold CV" }, compareProp, catProps, catPropsDisp, 0.05);
			CategoryPlot plot = (CategoryPlot) boxPlot2.getChart().getPlot();
			CategoryAxis xAxis = (CategoryAxis) plot.getDomainAxis();
			xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
			images = ArrayUtil.concat(images,
					new File[] { FreeChartUtil.toTmpFile(boxPlot2, new Dimension(1200, 800)) });
		}

		//			ResultSet rs2 = results.join(ArrayUtil.toList(new String[] { compareProp }), null, catProps);
		//			rs2.excludeProperties(ArrayUtil.toList(ArrayUtil.concat(new String[] { compareProp },
		//					ArrayUtil.toArray(catProps))));
		//			tables = ArrayUtil.concat(tables, new ResultSet[] { rs2 });
		//		}

		report.addSection("Compare " + compareProp + titleSuffix, "", tables, images, null);
		report.newPage();
	}

	private static HashMap<String, MultiLabelInstances> dataMap = new HashMap<String, MultiLabelInstances>();

	public static MultiLabelInstances getData(String datasetName) throws InvalidDataFormatException, IOException,
			DocumentException
	{
		if (!dataMap.containsKey(datasetName))
			dataMap.put(datasetName, new MultiLabelInstances("arff/" + datasetName + ".arff", "arff/" + datasetName
					+ ".xml"));
		return dataMap.get(datasetName);
	}

	//	private void addDatasetInfo(String datasetName) throws InvalidDataFormatException, IOException, DocumentException
	//	{
	//		addDatasetInfo(getDatasetInfo(datasetName));
	//	}

	private void addDatasetInfo(String datasetName) throws InvalidDataFormatException, IOException, DocumentException
	{
		MLCDataInfo di = MLCDataInfo.get(getData(datasetName));

		File images[] = new File[] { FreeChartUtil.toTmpFile(di.plotMissingPerLabel(), new Dimension(1200, 800)),
				FreeChartUtil.toTmpFile(di.plotMissingPerCompound(), new Dimension(1200, 600)),
				FreeChartUtil.toTmpFile(di.plotCorrelationHistogramm(), new Dimension(1200, 600)),
				SwingUtil.toTmpFile(di.plotCorrelationMatrix(false), new Dimension(1200, 1150)),
				SwingUtil.toTmpFile(di.plotCorrelationMatrix(true), new Dimension(1200, 1150)) };

		//		di.plotCorrelationMatrix(true, csv, true);

		List<File> smallImages = new ArrayList<File>();
		if (!di.includeV)
		{
			CSVFile csv = MLCDataInfo.getCSV(di.dataset);

			for (int j = 0; j < di.dataset.getNumLabels(); j++)
			{
				Attribute labelAttr = di.dataset.getDataSet().attribute(di.dataset.getLabelIndices()[j]);

				System.out.println("create clazz histogram for " + labelAttr.name());
				Double d[] = csv.getDoubleColumn(labelAttr.name() + "_real");
				String s[] = csv.getColumn(labelAttr.name());
				List<String> clazz = ArrayUtil.toList(new String[] { di.getClassValuesZero(), di.getClassValuesOne() });
				//			Collections.sort(clazz, new DefaultComparator<String>(true));
				List<double[]> values = new ArrayList<double[]>();
				for (int i = 0; i < clazz.size(); i++)
					values.add(new double[0]);

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
						values.set(0, ArrayUtil.concat(values.get(0), new double[] { d[i] }));
						if (s[i].equals("1")) // only collect low values -> active -> class 1
							values.set(1, ArrayUtil.concat(values.get(1), new double[] { d[i] }));
					}
				}
				if (di.zeros_per_label[j] + di.ones_per_label[j] != values.get(0).length)
					throw new IllegalStateException("values 0 should contain both "
							+ (di.zeros_per_label[j] + di.ones_per_label[j]) + " != " + values.get(0).length);
				if (di.ones_per_label[j] != values.get(1).length)
					throw new IllegalStateException("values 1 should contain only ones " + di.ones_per_label[j]
							+ " != " + values.get(1).length);

				List<String> subtitles = ArrayUtil.toList(new String[] { di.zeros_per_label[j] + " / "
						+ di.ones_per_label[j] + " missing: " + di.missings_per_label[j] });
				HistogramPanel h = new HistogramPanel("Real values for " + labelAttr.name(), subtitles, "value",
						"num compounds", clazz, values, 50);
				h.setIntegerTickUnits();
				smallImages.add(FreeChartUtil.toTmpFile(h.getChartPanel(), new Dimension(600, 300)));

				double v1[] = values.get(0);
				double v2[] = values.get(1);
				Arrays.sort(v1);
				Arrays.sort(v2);
				int cut = (int) Math.floor(v1.length * 9.0 / 10.0);
				if (v2[v2.length - 1] >= v1[cut])
					throw new Error("not yet implemented, remove values from both arrays!");
				values.set(0, Arrays.copyOfRange(v1, 0, cut));

				subtitles.add("Zoomed in: without " + (v1.length - cut) + " top compounds");
				h = new HistogramPanel("Real values for " + labelAttr.name() + " (zoom)", subtitles, "value",
						"num compounds", clazz, values, 50);
				h.setIntegerTickUnits();
				smallImages.add(FreeChartUtil.toTmpFile(h.getChartPanel(), new Dimension(600, 300)));
			}
		}

		report.addSection("Dataset " + di.datasetName, di.toString(false), new ResultSet[] { di.getMissingPerLabel() },
				images, ListUtil.toArray(File.class, smallImages));
		report.newPage();
	}

	public static void chooseWithDialog()
	{
		File results[] = new File("results").listFiles(new FilenameFilter()
		{

			@Override
			public boolean accept(File dir, String name)
			{
				return name.endsWith(".results");
			}
		});
		File datasets[] = new File("arff").listFiles(new FilenameFilter()
		{

			@Override
			public boolean accept(File dir, String name)
			{
				return name.endsWith(".arff");
			}
		});
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
			new ReportMLC("dataset_report_" + ArrayUtil.toString(names, "-", "", "", "") + ".pdf", names);
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
				new ReportMLC(name + "_" + measures + "_report.pdf", rs, measures);
			}
		}
	}

	public static void main(String args[]) throws Exception
	{
		if (args != null && args.length == 1 && args[0].equals("debug"))
		{
			String names[] = { "dataB-PC", "dataB-PC-clust60to80" };
			new ReportMLC("dataset_report_" + ArrayUtil.toString(names, "-", "", "", "") + ".pdf", names);
		}
		else if (args == null || args.length == 0)
			chooseWithDialog();
		else
		{
			String name = args[0];
			if (name.endsWith(".results"))
			{
				name = name.replace(".results", "").replace("results/", "");
				System.out.println("create result report for " + name);
				System.out.println("reading results:");
				ResultSet rs = ResultSetIO.parseFromFile(new File("results/" + name + ".results"));
				PerformanceMeasures measures = PerformanceMeasures.accuracy;
				if (args.length > 1)
					measures = PerformanceMeasures.valueOf(args[1]);
				String mod = "";
				if (args.length > 2)
				{
					mod = "_removed" + (args.length - 2);
					for (int i = 2; i < args.length; i++)
					{
						String excl[] = args[i].split(",");
						rs.remove(excl[0], excl[1]);
					}
				}
				new ReportMLC("reports/report_" + name + mod + "_" + measures + ".pdf", rs, measures);
			}
			else
			{
				new ReportMLC("reports/report_datasets_" + ArrayUtil.toString(args, "_", "", "", "") + ".pdf", args);
			}
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
}
