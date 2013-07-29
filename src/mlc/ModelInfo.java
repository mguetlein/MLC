package mlc;
import java.io.File;
import java.util.HashMap;
import java.util.List;


import mlc.reporting.ReportMLC;
import mulan.evaluation.Settings;
import mulan.evaluation.measure.ConfidenceLevel;
import util.ArrayUtil;
import util.FileUtil;
import util.ListUtil;
import datamining.ResultSet;
import datamining.ResultSetIO;

public class ModelInfo
{
	private static HashMap<String, ModelInfo> instances = new HashMap<String, ModelInfo>();

	private HashMap<String, String> props = new HashMap<String, String>();
	private ResultSet validationResult;
	private ResultSet validationResultJoin;

	private ModelInfo(String modelName)
	{
		if (!new File(Settings.modelFile(modelName)).exists())
			throw new Error("model not found");

		String s = FileUtil.readStringFromFile(Settings.modelPropsFile(modelName));
		for (String ss : s.split(","))
		{
			String sss[] = ss.split("=");
			if (sss.length != 2)
				throw new Error("cannot read props");
			props.put(sss[0], sss[1]);
		}
	}

	public static void writeModelProps(String modelName, String datasetName, String features, String experimentName)
	{
		if (!datasetName.matches(".*" + features + ".*"))
			throw new IllegalStateException("dataset name '" + datasetName + "' does not contain features '" + features
					+ "'");
		String settings = "dataset=" + datasetName + ",features=" + features + ",validation-experiment="
				+ experimentName;
		System.out.println("writing mlc model settings to file: " + Settings.modelPropsFile(modelName));
		FileUtil.writeStringToFile(Settings.modelPropsFile(modelName), settings);
	}

	public String getFeatures()
	{
		return props.get("features");
	}

	public String getDataset()
	{
		return props.get("dataset");
	}

	public String getExperiment()
	{
		return props.get("validation-experiment");
	}

	public boolean isValidated()
	{
		return new File(Settings.resultFile(getExperiment(), getDataset())).exists();
	}

	public double getValidationResult(String measure)
	{
		return getValidationResult(measure, null);
	}

	public double getValidationResult(String measure, Integer label)
	{
		return getValidationResult(measure, label, null);
	}

	public double getValidationResult(String measure, Integer label, ConfidenceLevel confLevel)
	{
		if (validationResult == null)
		{
			validationResult = ResultSetIO.parseFromFile(new File(Settings.resultFile(getExperiment(), getDataset())));
			List<String> catProps = ArrayUtil.toList(ReportMLC.getProps(ReportMLC.PerformanceMeasures.all));
			validationResultJoin = validationResult.join(ArrayUtil.toList(new String[] { "mlc-algorithm" }), null,
					catProps);
			if (validationResultJoin.getNumResults() > 1)
				throw new Error();
		}
		String p = measure;
		if (label != null)
			p += "#" + label;
		if (confLevel != null)
			p += confLevel.getShortName();
		if (!validationResultJoin.hasProperty(p))
			throw new Error("not found '" + p + "', available: "
					+ ListUtil.toString(validationResultJoin.getProperties()));
		return (Double) validationResultJoin.getResultValue(0, p);
	}

	public static ModelInfo get(String modelName)
	{
		if (!instances.containsKey(modelName))
			instances.put(modelName, new ModelInfo(modelName));
		return instances.get(modelName);
	}

}
