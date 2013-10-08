package mulan.evaluation;

import java.io.File;
import java.io.FilenameFilter;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import util.ArrayUtil;
import util.FileUtil;
import util.StringUtil;

public class Settings
{
	//	static
	//	{
	//		Locale.setDefault(Locale.US);
	//	}

	private static String PWD = "";

	private static ResourceBundle bundle;

	private static ResourceBundle bundle()
	{
		if (bundle == null)
			bundle = ResourceBundle.getBundle("bmbf-mlc");
		return bundle;
	}

	public static String text(String key)
	{
		return bundle().getString(key);
	}

	public static String text(String key, String param1)
	{
		return MessageFormat.format(bundle().getString(key), param1);
	}

	public static String text(String key, String param1, String param2)
	{
		return MessageFormat.format(bundle().getString(key), param1, param2);
	}

	public static String arffFile(String datasetName)
	{
		return PWD + "arff/" + datasetName + ".arff";
	}

	public static String xmlFile(String datasetName)
	{
		return PWD + "arff/" + datasetName + ".xml";
	}

	public static String csvFile(String datasetName)
	{
		return PWD + "arff/" + datasetName + ".csv";
	}

	public static String csvCompoundInfo(String datasetName, boolean base)
	{
		if (base)
			return PWD + "data/" + datasetName.split("_")[0] + "_compoundInfo.csv";
		else
			return PWD + "arff/" + datasetName + "_compoundInfo.csv";
	}

	public static String getFeaturesFromDatabaseName(String datasetName)
	{
		String features = ArrayUtil.last(datasetName.split("_"));
		if (ArrayUtil.indexOf(new String[] { "PC", "PCFP", "FP", "OB", "PCFP1", "FP1", "PC1FP1" }, features) == -1)
			throw new IllegalStateException("unknown feature : " + features);
		return features;
	}

	public static String inchiFile(String datasetName)
	{
		return PWD + "data/" + datasetName.split("_")[0] + ".inchi";
	}

	public static String smilesFile(String datasetName)
	{
		return PWD + "data/" + datasetName.split("_")[0] + ".smi";
	}

	public static String cssFile()
	{
		return "css/styles.css";
	}

	public static String missclassifiedFile(String datasetName, String experimentName)
	{
		return PWD + "results/" + datasetName + "_" + experimentName + "_missclassified.csv";
	}

	public static String missclassifiedFile(String datasetName, String experimentName, String endpoint)
	{
		return PWD + "results/" + datasetName + "_" + experimentName + "_missclassified_" + endpoint + ".csv";
	}

	public static String filledCsvFile(String datasetName, String experimentName, boolean confidence)
	{
		return PWD + "filled/" + datasetName + "_" + experimentName + "_filled" + (confidence ? "Confidence" : "Class")
				+ ".csv";
	}

	public static String filledArffFile(String datasetName, String experimentName, boolean confidence)
	{
		return PWD + "filled/" + datasetName + "_" + experimentName + "_filled" + (confidence ? "Confidence" : "Class")
				+ ".arff";
	}

	public static String resultFile(String experimentName, String... datasetNames)
	{
		return PWD + "results/" + experimentName + "_" + ArrayUtil.toString(datasetNames, "_", "", "", "") + ".results";
	}

	public static String reportFile(String[] datasetNames)
	{
		return PWD + "reports/report_datasets_" + ArrayUtil.toString(datasetNames, "_", "", "", "");
	}

	public static String reportFileEndpoints(String datasetName)
	{
		return PWD + "reports/report_dataset_endpoints_" + datasetName;
	}

	public static String reportFile(String experimentName, String[] datasetNames, String measures)
	{
		return PWD + "reports/report_" + experimentName + "_" + ArrayUtil.toString(datasetNames, "_", "", "", "") + "_"
				+ measures;
	}

	public static String compoundTableFile(String model)
	{
		return PWD + "models/" + model + "/compounds";
	}

	public static String endpointTableFile(String model)
	{
		return PWD + "models/" + model + "/endpoints";
	}

	public static String validationReportFile(String model)
	{
		return PWD + "models/" + model + "/validation";
	}

	//	public static String validationReportImageFile(String model, String image)
	//	{
	//		return PWD + "models/" + model + "/images/" + image + ".png";
	//	}

	public static void createModelDirectory(String modelName)
	{
		FileUtil.createParentFolders(PWD + "models/" + modelName);
		FileUtil.createParentFolders(PWD + "models/" + modelName + "/images/.");
		FileUtil.createParentFolders(PWD + "models/" + modelName + "/predictions/.");
	}

	public static String modelFile(String modelName)
	{
		return PWD + "models/" + modelName + "/" + modelName + ".model";
	}

	public static String modelPropsFile(String modelName)
	{
		return PWD + "models/" + modelName + "/" + modelName + ".settings";
	}

	public static String modelADFile(String modelName)
	{
		return PWD + "models/" + modelName + "/" + modelName + ".ad";
	}

	public static String modelDescriptionReport(String modelName)
	{
		return PWD + "models/" + modelName + "/description";
	}

	public static String compoundsArffFile(String compoundsName, String trainingDataset)
	{
		return PWD + "predictions/" + compoundsName + "_" + trainingDataset + ".arff";
	}

	public static String compoundsSmilesFile(String compoundsName)
	{
		return PWD + "predictions/" + compoundsName + ".smi";
	}

	public static String compoundsSDFile(String compoundsName)
	{
		return PWD + "predictions/" + compoundsName + ".sdf";
	}

	public static String compoundsInchiFile(String compoundsName)
	{
		return PWD + "predictions/" + compoundsName + ".inchi";
	}

	public static String compoundPicture(String smiles)
	{
		return PWD + "predictions/" + StringUtil.getMD5(smiles) + ".png";
	}

	//	public static String imageFile(String identifier)
	//	{
	//		return PWD + "images/" + identifier + ".png";
	//	}

	//	public static String imageFile(String image)
	//	{
	//		return PWD + "images/" + image + ".png";
	//	}
	//
	public static String predictionReport(String modelName, String compoundsName)
	{
		return PWD + "models/" + modelName + "/predictions/" + compoundsName;
	}

	public static String appDomainImageFile(String modelName, String compoundsName, int compoundIndex, String endpoint)
	{
		return PWD + "models/" + modelName + "/predictions/" + compoundsName + "_AD_" + compoundIndex
				+ (endpoint != null ? ("_" + endpoint) : "") + ".png";
	}

	public static File[] listResultFiles()
	{
		return new File(PWD + "results").listFiles(new FilenameFilter()
		{
			@Override
			public boolean accept(File dir, String name)
			{
				return name.endsWith(".results");
			}
		});
	}

	public static File[] listArffFiles()
	{
		return new File(PWD + "arff").listFiles(new FilenameFilter()
		{
			@Override
			public boolean accept(File dir, String name)
			{
				return name.endsWith(".arff");
			}
		});
	}

}
