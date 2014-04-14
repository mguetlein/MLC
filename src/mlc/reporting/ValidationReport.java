package mlc.reporting;

import java.io.File;
import java.util.HashMap;

import mlc.ConfusionMatrix;
import mlc.MLCDataInfo;
import mlc.ModelInfo;
import mulan.data.MultiLabelInstances;
import mulan.evaluation.Settings;
import mulan.evaluation.measure.ConfidenceLevel;
import mulan.evaluation.measure.ConfidenceLevelProvider;
import util.StringUtil;
import datamining.Result;
import datamining.ResultSet;
import datamining.ResultSetIO;

public class ValidationReport
{

	public static void validationReport(String performanceMeasure, String modelName) throws Exception
	{
		ModelInfo mi = ModelInfo.get(modelName);
		System.out.println("create result report for " + Settings.resultFile(mi.getExperiment(), mi.getDataset()));
		System.out.println("reading results:");
		ResultSet results = ResultSetIO
				.parseFromFile(new File(Settings.resultFile(mi.getExperiment(), mi.getDataset())));

		//		{
		//			List<String> remProp = new ArrayList<String>();
		//			for (String prop : results.getProperties())
		//				if (prop.contains("#") && !prop.contains("#0"))
		//					remProp.add(prop);
		//			for (String p : remProp)
		//				results.removePropery(p);
		//			System.out.println(results.toNiceString());
		//			System.exit(0);
		//		}

		ReportMLC.PerformanceMeasures measures = ReportMLC.PerformanceMeasures.accuracy;
		measures = ReportMLC.PerformanceMeasures.valueOf(performanceMeasure);
		String outfile = Settings.validationReportFile(modelName);
		ValidationReport.validationReport(outfile, mi.getDataset(), results, measures, modelName);
	}

	//	private static String linkToModel(String model)
	//	{
	//		return "../" + model;
	//	}

	public static void validationReport(String outfile, String datasetName, ResultSet results,
			ReportMLC.PerformanceMeasures measures, String model) throws Exception
	{
		MultiLabelInstances data = ReportMLC.getData(datasetName);
		MLCDataInfo di = MLCDataInfo.get(data);

		ReportMLC rep = new ReportMLC(outfile, "Validation results", false, "../../");
		rep.report.addParagraph("This is a validation report for model "
				+ rep.report.encodeLink(".", ModelInfo.get(model).getAlias()) + ".");
		rep.report.addGap();

		rep.report.newSection("General information");

		rep.report.addParagraph("The model was validated with a " + results.getResultValues("cv-seed").getNumValues()
				+ "-times repeated " + ((Double) results.getUniqueValue("num-folds")).intValue()
				+ "-fold cross-validation.");

		rep.report.newSubsection("Performance measures");

		rep.report.addTable(ReportMLC.getInfo(measures, MLCDataInfo.INACTIVE, MLCDataInfo.ACTIVE), null, null, false);

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

		rep.report.newSection("Average performance over all endpoints");

		rep.addBoxPlots(results, "mlc-algorithm", "", "no_file", measures, false);

		rep.report.newSection("Single endpoint validation");
		ResultSet summed = results.join("cv-seed", Result.JOIN_MODE_SUM);
		summed.clearMergeCountAndVariance();
		ResultSet joined = summed.join("mlc-algorithm");
		if (joined.getNumResults() != 1)
			throw new Error("should be one result");

		for (int l = 0; l < data.getNumLabels(); l++)
		{
			String labelName = data.getDataSet().attribute(data.getLabelIndices()[l]).name();
			System.out.println(l + " " + labelName);
			rep.report.newSubsection(labelName);
			String s = "The endpoint " + rep.report.encodeLink("endpoints#" + labelName, labelName) + " is "
					+ di.zeros_per_label[l] + " x active, " + di.ones_per_label[l] + " x inactive and "
					+ di.missings_per_label[l] + " x missing in the training dataset. ";

			HashMap<ConfidenceLevel, Double> numResults = new HashMap<ConfidenceLevel, Double>();
			for (ConfidenceLevel level : ConfidenceLevelProvider.LEVELS)
			{
				Double sum = 0.0;
				for (ConfusionMatrix.Values v : ConfusionMatrix.Values.values())
					sum += (Double) joined.getResultValue(0, v.toString() + "#" + l + level.getShortName());
				numResults.put(level, sum);
			}
			s += "In each cross-validation ";
			s += StringUtil.formatDouble(numResults.get(ConfidenceLevelProvider.CONFIDENCE_LEVEL_HIGH)) + " (of all "
					+ (di.ones_per_label[l] + di.zeros_per_label[l]) + " non-missing compounds) were predicted with "
					+ ConfidenceLevelProvider.CONFIDENCE_LEVEL_HIGH.getNiceName() + ", ";
			s += StringUtil.formatDouble(numResults.get(ConfidenceLevelProvider.CONFIDENCE_LEVEL_MEDIUM)) + " with "
					+ ConfidenceLevelProvider.CONFIDENCE_LEVEL_MEDIUM.getNiceName() + " and ";
			s += StringUtil.formatDouble(numResults.get(ConfidenceLevelProvider.CONFIDENCE_LEVEL_LOW)) + " with "
					+ ConfidenceLevelProvider.CONFIDENCE_LEVEL_LOW.getNiceName() + ".";
			rep.report.addParagraph(s);
			rep.report.addGap();
			rep.addSingleEndpointConfidencePlot(results, l, labelName, measures);
		}

		rep.close();
	}
}
