package mlc.reporting;

import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mlc.MLCDataInfo;
import mulan.data.MultiLabelInstances;
import mulan.evaluation.Settings;
import mulan.evaluation.measure.ConfidenceLevel;
import mulan.evaluation.measure.ConfidenceLevelProvider;

import org.jfree.chart.ChartPanel;

import util.ArrayUtil;
import datamining.ResultSet;
import datamining.ResultSetIO;
import freechart.FreeChartUtil;

public class ValidationReport
{

	public static void validationReport(String experimentName, String[] datasetNames, String performanceMeasure,
			String modelName) throws Exception
	{
		if (experimentName == null || datasetNames.length != 1)
			throw new Error("experimentName and/or datasetNames missing, nof datasets should be 1");
		System.out.println("create result report for " + Settings.resultFile(experimentName, datasetNames));
		System.out.println("reading results:");
		ResultSet results = ResultSetIO.parseFromFile(new File(Settings.resultFile(experimentName, datasetNames)));
		ReportMLC.PerformanceMeasures measures = ReportMLC.PerformanceMeasures.accuracy;
		measures = ReportMLC.PerformanceMeasures.valueOf(performanceMeasure);
		String outfile = Settings.validationReportFile(experimentName, datasetNames, measures.toString());
		ValidationReport.validationReport(outfile, datasetNames[0], results, measures, modelName);
	}

	private static String linkToModel(String model)
	{
		return "../" + model;
	}

	public static void validationReport(String outfile, String datasetName, ResultSet results,
			ReportMLC.PerformanceMeasures measures, String model) throws Exception
	{
		MultiLabelInstances data = ReportMLC.getData(datasetName);
		MLCDataInfo di = MLCDataInfo.get(data);

		ReportMLC rep = new ReportMLC(outfile, "Validation of " + model);

		rep.report.newSection("General information");

		rep.report.addParagraph("The model was validated with a " + results.getResultValues("cv-seed").size()
				+ "-times repeated " + ((Double) results.getUniqueValue("num-folds")).intValue()
				+ "-fold cross-validation.");

		rep.report.newSubsection("Performance measures");

		rep.report.addTable(ReportMLC.getInfo(measures, di.getClassValuesZeroNice(), di.getClassValuesOneNice()), null,
				null, false);

		rep.report.newSubsection(Settings.text("probability-correct"));
		rep.report.addParagraph(Settings.text("probability-correct.description"));

		//		int i = 5;
		//		System.out.println(i + ": " + data.getDataSet().attribute(data.getLabelIndices()[i]).name());
		//		ConfidenceLevel c = ConfidenceLevelProvider.CONFIDENCE_LEVEL_HIGH;
		//		results.excludeProperties(ArrayUtil.toList(new String[] { "dataset-name", "cv-seed", "fold",
		//				"macro-accuracy#" + i + c.getShortName(), "macro-ppv#" + i + c.getShortName(),
		//				"macro-npv#" + i + c.getShortName(), "TP#" + i + c.getShortName(), "FP#" + i + c.getShortName(),
		//				"TN#" + i + c.getShortName(), "FN#" + i + c.getShortName() }));
		//		System.out.println(results.toNiceString());
		//		ResultSet joined = results.join(ArrayUtil.toList(new String[] { "dataset-name" }), null, null,
		//				Result.JOIN_MODE_MEAN).copy();
		//		//		joined.clearMergeCountAndVariance();
		//		System.out.println(joined.toNiceString());
		//		//		joined = joined.join(ArrayUtil.toList(new String[] { "dataset-name" }), null, null, Result.JOIN_MODE_MEAN);
		//		//		System.out.println(results.toNiceString());
		//		System.exit(1);

		rep.report.newSection("Single endpoint validation");

		for (int l = 0; l < data.getNumLabels(); l++)
		{
			String labelName = data.getDataSet().attribute(data.getLabelIndices()[l]).name();

			System.out.println(l + " " + labelName);

			rep.report.newSubsection(labelName);

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
						Double val = (Double) results.getResultValue(i, prop) * 100;
						rs.setResultValue(n, pNice, val);
					}
				}
			}

			//			System.out.println(rs.toNiceString());
			//			System.out.println(rs);

			rs.setNicePropery(confStr,
					rep.report.encodeLink(linkToModel(model) + "/validation#model-confidence", confStr));
			for (String p : dispProps)
				rs.setNicePropery(p, rep.report.encodeLink(linkToModel(model) + "/validation#" + p, p));

			rep.report.addTable(rs.join(confStr));

			rep.report.addGap();

			ChartPanel boxPlot = rs.boxPlot("Performance for endpoint " + labelName, "Performance", null, confStr,
					dispProps, null, 5.0);
			//			ChartPanel boxPlot = results.boxPlot("Performance for endpoint " + labelName, "Performance", null,
			//					"dataset-name", catProps, dispProps, 0.05);
			rep.report.addImage(FreeChartUtil.toFile(Settings.imageFile(UUID.randomUUID().toString()), boxPlot,
					new Dimension(800, 400)));

			//			for (ConfidenceLevel confLevel : ConfidenceLevelProvider.LEVELS)
			//			{
			//				rep.report.addTable(ResultSet.build(di.getConfusionMatrix(joined, l, confLevel)), "Confusion Matrix "
			//						+ confLevel.getName());
			//			}

			//			if (l > 2)
			//				break;
		}

		rep.close();
	}
}
