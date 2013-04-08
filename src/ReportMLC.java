import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mulan.data.InvalidDataFormatException;
import mulan.data.MultiLabelInstances;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;

import util.ArrayUtil;
import util.CollectionUtil;
import util.CountedSet;
import util.FileUtil;
import util.FileUtil.CSVFile;
import util.ListUtil;
import util.StringLineAdder;
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

	public static final String[] RESULT_PROPERTIES_ACCURACY = { "micro-accuracy", "macro-accuracy", "1-hamming-loss",
			"subset-accuracy" };

	public static final String[] RESULT_PROPERTIES_F_MEASURE = { "micro-f-measure", "macro-f-measure",
			"subset-accuracy" };

	public ReportMLC(String outfile, String... datasetNames)
	{
		try
		{
			report = new Report(outfile, "Dataset Report");
			MLCData.DatasetInfo di[] = new MLCData.DatasetInfo[datasetNames.length];
			ResultSet res = new ResultSet();
			for (int i = 0; i < di.length; i++)
			{
				di[i] = getDatasetInfo(datasetNames[i]);
				int r = res.addResult();
				res.setResultValue(r, "dataset-name", di[i].datasetName);
				res.setResultValue(r, "endpoint-file", di[i].endpointFile);
				res.setResultValue(r, "feature-file", di[i].featureFile);
				res.setResultValue(r, "num-endpoints", di[i].numEndpoints);
				res.setResultValue(r, "num-missing-allowed", di[i].numMissingAllowed);
				res.setResultValue(r, "num-instances", di[i].dataset.getNumInstances());
				res.setResultValue(r, "discretization-level", di[i].discretizationLevel);
				res.setResultValue(r, "include-V", di[i].includeV);
			}
			report.addSection("Datasets", "", new ResultSet[] { res }, null, null);
			report.newPage();
			for (int i = 0; i < di.length; i++)
				addDatasetInfo(di[i], datasetNames[i]);

			report.close();
			System.out.println("\nreport created:\n" + outfile);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static enum PerformanceMeasures
	{
		accuracy, fmeasure
	}

	public ReportMLC(String outfile, ResultSet results, PerformanceMeasures measures)
	{
		try
		{
			report = new Report(outfile, "Multi-Label-Classification (MLC) Results");

			StringLineAdder s = new StringLineAdder();
			if (measures == PerformanceMeasures.accuracy)
			{
				s.add("micro-accuracy: overall accuracy (each single prediction)");
				s.add("macro-accuracy: accuracy averaged by endpoint");
				s.add("1-hamming-loss: accuracy averaged by compound");
				s.add();
				s.add("micro-accuracy > macro-accuracy: endpoints with few missing values are predicted better than endpoints with many missing values");
				s.add("micro-accuracy > 1-hamming-loss: compounds with few missing values are predicted better than compounds with many missing values");
			}
			else if (measures == PerformanceMeasures.fmeasure)
			{
				s.add("f-measure: harmonic mean of 'precision' and 'recall', performance measure for in-balanced data with less active than in-active compounds, ignores correct in-active predictions (true negatives tn)");
				s.add("precision: same as 'positive predictive value', ratio of active compounds within comopunds that are classified as actives ( tp/(tp+fp) )");
				s.add("recall: same as 'sensitiviy', ratio of active-classified compounds within all active comopunds ( tp/(tp+fn) )");
				s.add();
				s.add("micro-f-measure: f-measure computed with each single predictions");
				s.add("macro-f-measure: f-measure averaged by endpoint");
				s.add();
				s.add("micro-f-measure > macro-f-measure: compounds with few missing values are predicted better than compounds with many missing values");
			}
			s.add();
			s.add("subset-accuracy: average number of compounds where all enpoints are predicted correctly");
			report.addSection("Performance measures", s.toString(), null, null, null);
			report.newPage();

			this.results = results;
			//			System.out.println(results.toNiceString());

			for (int i = 0; i < results.getNumResults(); i++)
				if (results.getResultValue(i, "mlc-algorithm").toString().equals("BR"))
					results.setResultValue(i, "mlc-algorithm", "Single endpoint prediction");
				else if (results.getResultValue(i, "mlc-algorithm").toString().equals("ECC"))
					results.setResultValue(i, "mlc-algorithm", "Ensemble of classfier chains");

			CountedSet<Object> datasetNames = results.getResultValues("dataset-name");

			results.sortProperties(new String[] { "dataset-name", "mlc-algorithm", "mlc-algorithm-params" });
			for (String p : (measures == PerformanceMeasures.accuracy ? RESULT_PROPERTIES_ACCURACY
					: RESULT_PROPERTIES_F_MEASURE))
				results.movePropertyBack(p);

			CountedSet<Object> mlcAlgorithms = results.getResultValues("mlc-algorithm");
			CountedSet<Object> mlcAlgorithmParams = results.getResultValues("mlc-algorithm-params");
			CountedSet<Object> wekaClassifiers = results.getResultValues("classifier");
			if (mlcAlgorithms.size() > 1 && mlcAlgorithmParams.size() > 1)
				throw new IllegalStateException("compare either algs or alg-params, plz!");
			if (mlcAlgorithms.size() > 1 && wekaClassifiers.size() > 1)
				throw new IllegalStateException("compare either mlc-algs or weka-algs, plz!");
			String algCmp = null;
			CountedSet<Object> algSet = null;
			if (mlcAlgorithmParams.size() > 1)
			{
				algCmp = "mlc-algorithm-params";
				algSet = mlcAlgorithmParams;
			}
			else if (wekaClassifiers.size() > 1)
			{
				algCmp = "classifier";
				algSet = wekaClassifiers;
			}
			else
			{
				algCmp = "mlc-algorithm";
				algSet = mlcAlgorithms;
			}

			for (Object datasetName : datasetNames.values())
			{
				ResultSet res = results.copy();
				res.exclude("dataset-name", datasetName);
				addBoxPlots(res, algCmp, " for dataset " + res.getUniqueValue("dataset-name"), measures);
			}

			for (Object mlcAlg : algSet.values())
			{
				ResultSet res = results.copy();
				res.exclude(algCmp, mlcAlg);
				addBoxPlots(res, "dataset-name", " for " + algCmp + " = " + mlcAlg, measures);
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

		List<String> catProps = ArrayUtil.toList((measures == PerformanceMeasures.accuracy ? RESULT_PROPERTIES_ACCURACY
				: RESULT_PROPERTIES_F_MEASURE));
		ChartPanel boxPlot1 = results.boxPlot("Performance for different " + compareProp + titleSuffix, "Performance",
				new String[] { "compounds: " + numCompounds + ", labels: " + numLabels + ", " + numCVSeeds + " x "
						+ numCVFolds + "-fold CV" }, compareProp, catProps, null, 0.05);
		File images[] = new File[] { FreeChartUtil.toTmpFile(boxPlot1, new Dimension(1200, 600)) };

		ResultSet rs = results.join(ArrayUtil.toList(new String[] { compareProp }), null, catProps);
		rs.excludeProperties(ArrayUtil.toList(ArrayUtil.concat(new String[] { compareProp },
				(measures == PerformanceMeasures.accuracy ? RESULT_PROPERTIES_ACCURACY : RESULT_PROPERTIES_F_MEASURE))));
		ResultSet tables[] = new ResultSet[] { rs };

		//		if (results.getResultValues("dataset-name").size() == 1)
		//		{
		Double numLabelsInt = Double.parseDouble(results.getUniqueValue("num-labels") + "");
		catProps.clear();
		for (int i = 0; i < numLabelsInt; i++)
			catProps.add((measures == PerformanceMeasures.accuracy ? "macro-accuracy#" : "macro-f-measure#") + i);
		List<String> catPropsDisp = new ArrayList<String>();
		for (int i = 0; i < numLabelsInt; i++)
			catPropsDisp.add(results.getUniqueValue("label#" + i).toString());
		ChartPanel boxPlot2 = results.boxPlot("Endpoint "
				+ (measures == PerformanceMeasures.accuracy ? "accuracy" : "f-measure") + " for different "
				+ compareProp + titleSuffix, "Performance", new String[] { "compounds: " + numCompounds + ", labels: "
				+ numLabels + ", " + numCVSeeds + " x " + numCVFolds + "-fold CV" }, compareProp, catProps,
				catPropsDisp, 0.05);
		CategoryPlot plot = (CategoryPlot) boxPlot2.getChart().getPlot();
		CategoryAxis xAxis = (CategoryAxis) plot.getDomainAxis();
		xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
		images = ArrayUtil.concat(images, new File[] { FreeChartUtil.toTmpFile(boxPlot2, new Dimension(1200, 800)) });

		//			ResultSet rs2 = results.join(ArrayUtil.toList(new String[] { compareProp }), null, catProps);
		//			rs2.excludeProperties(ArrayUtil.toList(ArrayUtil.concat(new String[] { compareProp },
		//					ArrayUtil.toArray(catProps))));
		//			tables = ArrayUtil.concat(tables, new ResultSet[] { rs2 });
		//		}

		report.addSection("Compare " + compareProp + titleSuffix, "", tables, images, null);
		report.newPage();
	}

	private MLCData.DatasetInfo getDatasetInfo(String datasetName) throws InvalidDataFormatException, IOException,
			DocumentException
	{
		MultiLabelInstances dataset = new MultiLabelInstances("tmp/" + datasetName + ".arff", "tmp/" + datasetName
				+ ".xml");
		return new MLCData.DatasetInfo(dataset);
	}

	//	private void addDatasetInfo(String datasetName) throws InvalidDataFormatException, IOException, DocumentException
	//	{
	//		addDatasetInfo(getDatasetInfo(datasetName));
	//	}

	private void addDatasetInfo(MLCData.DatasetInfo di, String datasetName) throws InvalidDataFormatException,
			IOException, DocumentException
	{
		File images[] = new File[] { FreeChartUtil.toTmpFile(di.plotMissingPerLabel(), new Dimension(1200, 800)),
				FreeChartUtil.toTmpFile(di.plotMissingPerCompound(), new Dimension(1200, 600)),
				FreeChartUtil.toTmpFile(di.plotCorrelation(), new Dimension(1200, 600)) };

		CSVFile csv = FileUtil.readCSV("tmp/" + datasetName + ".csv");

		List<File> smallImages = new ArrayList<File>();
		for (int j = 0; j < di.dataset.getNumLabels(); j++)
		{
			Attribute labelAttr = di.dataset.getDataSet().attribute(di.dataset.getLabelIndices()[j]);

			System.out.println("create clazz histogram for " + labelAttr.name());
			Double d[] = csv.getDoubleColumn(labelAttr.name() + "_real");
			String s[] = csv.getColumn(labelAttr.name());
			List<String> clazz = ArrayUtil.toList(new String[] { "all", "0" });
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
					if (s[i].equals("0"))
						values.set(1, ArrayUtil.concat(values.get(1), new double[] { d[i] }));
				}
			}
			for (int i = 0; i < clazz.size(); i++)
			{
				if (clazz.get(i).equals("all")
						&& (di.zeros_per_label[j] + di.ones_per_label[j]) != values.get(i).length)
					throw new IllegalStateException((di.zeros_per_label[j] + di.ones_per_label[j]) + " != "
							+ values.get(i).length);
				if (clazz.get(i).equals("1") && di.ones_per_label[j] != values.get(i).length)
					throw new IllegalStateException(di.ones_per_label[j] + " != " + values.get(i).length);
				if (clazz.get(i).equals("0") && di.zeros_per_label[j] != values.get(i).length)
					throw new IllegalStateException(di.zeros_per_label[j] + " != " + values.get(i).length);
			}
			List<String> subtitles = ArrayUtil.toList(new String[] { di.zeros_per_label[j] + " / "
					+ di.ones_per_label[j] + " missing: " + di.missings_per_label[j] });
			HistogramPanel h = new HistogramPanel("Real values for " + labelAttr.name(), subtitles, "value",
					"num compounds", clazz, values, 50);
			h.setIntegerTickUnits();

			smallImages.add(FreeChartUtil.toTmpFile(h.getChartPanel(), new Dimension(600, 300)));
		}

		report.addSection("Dataset " + di.datasetName, di.toString(false), new ResultSet[] { di.getMissingPerLabel() },
				images, ListUtil.toArray(smallImages));
		report.newPage();
	}

	public static void main(String args[]) throws Exception
	{
		System.out.println("reading results:");
		//String infile = "ECC_BR_dataA-dataB-dataC";
		//String infile = "BR_alg_dataC";
		//String infile = "ECC_BR_cpdb";
		String infile = "BR_IBk_dataAsmall";
		//		String infile = "BR_ECC_dataApc-dataAfp1-dataAfp2-dataAfp3-dataAfp4";
		ResultSet rs = ResultSetIO.parseFromFile(new File("tmp/" + infile + ".results"));
		System.out.println(rs.getNumResults() + " single results, creating report");
		new ReportMLC(infile + "_report.pdf", rs, PerformanceMeasures.fmeasure);

		//		new ReportMLC("dataset_report.pdf", "dataA", "dataB", "dataC", "dataD");
		//new ReportMLC("dataset_report_dataDall.pdf", "dataAsmallC", "dataAsmall");
		//new ReportMLC("dataset_report_dataA_pc_fp.pdf", "dataApc", "dataAfp1", "dataAfp2", "dataAfp3", "dataAfp4");

		//				new ReportMLC("dataset_report.pdf", "cpdb");

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
}
