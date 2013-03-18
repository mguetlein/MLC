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
import util.StringLineAdder;

import com.itextpdf.text.DocumentException;

import datamining.ResultSet;
import freechart.FreeChartUtil;

public class ReportMLC
{
	ResultSet results;
	Report report;

	private List<String> getLabelProperties()
	{
		Double numLabels = Double.parseDouble(results.getUniqueValue("num-labels") + "");
		List<String> l = new ArrayList<String>();
		for (int i = 0; i < numLabels; i++)
			l.add("label#" + i);
		return l;
	}

	public static final String[] RESULT_PROPERTIES = { "micro-accuracy", "macro-accuracy", "1-hamming-loss",
			"subset-accuracy" };

	private static final String[] DATASET_NAMES = { "(A)", "(B)", "(C)" };

	public ReportMLC(String... arffFiles)
	{
		try
		{
			report = new Report("report.pdf", "MLC Results");
			int i = 0;
			for (String arffFile : arffFiles)
				addDatasetInfo(arffFile, DATASET_NAMES[i++]);
			report.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public ReportMLC(ResultSet results)
	{
		try
		{
			String outfile = "report.pdf";
			report = new Report(outfile, "Multi-Label-Classification (MLC) Results");

			StringLineAdder s = new StringLineAdder();
			s.add("micro-accuracy: overall accuracy (each single prediction)");
			s.add("macro-accuracy: accuracy averaged by endpoint");
			s.add("1-hamming-loss: accuracy averaged by compound");
			s.add("subset-accuracy: average number of compounds where all enpoints are predicted correctly");
			s.add();
			s.add("micro-accuracy > macro-accuracy: endpoints with few missing values are predicted better than endpoints with many missing values");
			s.add("micro-accuracy > 1-hamming-loss: compounds with few missing values are predicted better than compounds with many missing values");
			report.addSection("Performance measures", s.toString(), null, null);
			report.newPage();

			this.results = results;
			//			System.out.println(results.toNiceString());

			for (int i = 0; i < results.getNumResults(); i++)
				if (results.getResultValue(i, "mlc-algorithm").toString().equals("BR"))
					results.setResultValue(i, "mlc-algorithm", "Single endpoint prediction");
				else if (results.getResultValue(i, "mlc-algorithm").toString().equals("ECC"))
					results.setResultValue(i, "mlc-algorithm", "Ensemble of classfier chains");

			CountedSet<Object> arffFiles = results.getResultValues("arff-file");
			for (int i = 0; i < results.getNumResults(); i++)
				results.setResultValue(i, "Dataset",
						DATASET_NAMES[arffFiles.values().indexOf(results.getResultValue(i, "arff-file"))]);
			for (String p : RESULT_PROPERTIES)
				results.movePropertyBack(p);

			for (Object arffFile : arffFiles.values())
			{
				ResultSet res = results.copy();
				res.exclude("arff-file", arffFile);
				addBoxPlots(res, "mlc-algorithm", " for Dataset " + res.getUniqueValue("Dataset"));
			}

			CountedSet<Object> mlcAlgorithms = results.getResultValues("mlc-algorithm");
			for (Object mlcAlgorithm : mlcAlgorithms.values())
			{
				ResultSet res = results.copy();
				res.exclude("mlc-algorithm", mlcAlgorithm);
				addBoxPlots(res, "Dataset", " for mlc-algorithm = " + mlcAlgorithm);
			}

			int i = 0;
			for (Object arffFile : arffFiles.values())
				addDatasetInfo(arffFile.toString(), DATASET_NAMES[i++]);
			report.close();

			System.out.println("report stored in " + outfile);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void addBoxPlots(ResultSet results, String compareProp, String titleSuffix) throws IOException,
			DocumentException
	{
		String numCompounds = CollectionUtil.toString(results.getResultValues("num-compounds").values());
		String numLabels = CollectionUtil.toString(results.getResultValues("num-labels").values());
		int numCVSeeds = results.getResultValues("cv-seed").size();
		Double numCVFolds = Double.parseDouble(results.getUniqueValue("num-folds") + "");

		List<String> catProps = ArrayUtil.toList(RESULT_PROPERTIES);
		ChartPanel boxPlot1 = results.boxPlot("Performance for different " + compareProp + titleSuffix, "Performance",
				new String[] { "compounds: " + numCompounds + ", labels: " + numLabels + ", " + numCVSeeds + " x "
						+ numCVFolds + "-fold CV" }, compareProp, catProps);
		File images[] = new File[] { FreeChartUtil.toTmpFile(boxPlot1, new Dimension(1200, 600)) };

		ResultSet rs = results.join(ArrayUtil.toList(new String[] { compareProp }), null, catProps);
		rs.excludeProperties(ArrayUtil.toList(ArrayUtil.concat(new String[] { compareProp }, RESULT_PROPERTIES)));
		ResultSet tables[] = new ResultSet[] { rs };

		if (results.getResultValues("arff-file").size() == 1)
		{
			Double numLabelsInt = Double.parseDouble(results.getUniqueValue("num-labels") + "");
			catProps.clear();
			for (int i = 0; i < numLabelsInt; i++)
				catProps.add("macro-accuracy#" + i);
			List<String> catPropsDisp = new ArrayList<String>();
			for (int i = 0; i < numLabelsInt; i++)
				catPropsDisp.add(results.getUniqueValue("label#" + i).toString());
			ChartPanel boxPlot2 = results.boxPlot("Endpoint accuracy for different " + compareProp + titleSuffix,
					"Performance", new String[] { "compounds: " + numCompounds + ", labels: " + numLabels + ", "
							+ numCVSeeds + " x " + numCVFolds + "-fold CV" }, compareProp, catProps, catPropsDisp);
			CategoryPlot plot = (CategoryPlot) boxPlot2.getChart().getPlot();
			CategoryAxis xAxis = (CategoryAxis) plot.getDomainAxis();
			xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
			images = ArrayUtil.concat(images,
					new File[] { FreeChartUtil.toTmpFile(boxPlot2, new Dimension(1200, 800)) });

			//			ResultSet rs2 = results.join(ArrayUtil.toList(new String[] { compareProp }), null, catProps);
			//			rs2.excludeProperties(ArrayUtil.toList(ArrayUtil.concat(new String[] { compareProp },
			//					ArrayUtil.toArray(catProps))));
			//			tables = ArrayUtil.concat(tables, new ResultSet[] { rs2 });
		}

		report.addSection("Compare " + compareProp + titleSuffix, "", tables, images);
		//		report.newPage();
	}

	private void addDatasetInfo(String arffFile, String datasetName) throws InvalidDataFormatException, IOException,
			DocumentException
	{
		String xmlFile = arffFile.replace(".arff", ".xml");
		MultiLabelInstances dataset = new MultiLabelInstances(arffFile, xmlFile);
		MLCData.DatasetInfo di = new MLCData.DatasetInfo(dataset);
		File images[] = new File[] { FreeChartUtil.toTmpFile(di.plotMissingPerLabel(), new Dimension(1200, 800)),
				FreeChartUtil.toTmpFile(di.plotMissingPerCompound(), new Dimension(1200, 600)),
				FreeChartUtil.toTmpFile(di.plotCorrelation(), new Dimension(1200, 600)) };
		report.addSection("Dataset " + datasetName, "arff-file: " + arffFile + "\n" + di.toString(false),
				new ResultSet[] { di.getMissingPerLabel() }, images);
		report.newPage();
	}

	public static void main(String args[]) throws Exception
	{
		//		System.out.println("reading results:");
		//		ResultSet rs = ResultSetIO.parseFromFile(new File("tmp/results"));
		//		System.out.println(rs.getNumResults() + " single results, creating report");
		//		new ReportMLC(rs);

		new ReportMLC("tmp/input2013-03-18_16-27-00.arff", "tmp/input2013-03-18_16-32-42.arff");

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
