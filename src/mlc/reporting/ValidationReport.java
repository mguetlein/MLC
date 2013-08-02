package mlc.reporting;

import java.io.File;

import mlc.MLCDataInfo;
import mlc.ModelInfo;
import mulan.data.MultiLabelInstances;
import mulan.evaluation.Settings;
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
		//		MLCDataInfo di = MLCDataInfo.get(data);

		ReportMLC rep = new ReportMLC(outfile, "Validation results", false);
		rep.report.addParagraph("This is a validation report for model " + rep.report.encodeLink(".", model) + ".");
		rep.report.addGap();

		rep.report.newSection("General information");

		rep.report.addParagraph("The model was validated with a " + results.getResultValues("cv-seed").size()
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

		for (int l = 0; l < data.getNumLabels(); l++)
		{
			String labelName = data.getDataSet().attribute(data.getLabelIndices()[l]).name();
			System.out.println(l + " " + labelName);
			rep.report.newSubsection(labelName);
			rep.addSingleEndpointConfidencePlot(results, l, labelName, measures);
		}

		rep.close();
	}
}
