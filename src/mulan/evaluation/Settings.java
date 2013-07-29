package mulan.evaluation;

import java.io.File;
import java.io.FilenameFilter;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import util.ArrayUtil;
import util.StringUtil;

public class Settings
{
	//	static
	//	{
	//		Locale.setDefault(Locale.US);
	//	}

	public static String PWD = "";

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

	public static String resultFile(String experimentName, String... datasetNames)
	{
		return PWD + "results/" + experimentName + "_" + ArrayUtil.toString(datasetNames, "_", "", "", "") + ".results";
	}

	public static String reportFile(String[] datasetNames)
	{
		return PWD + "reports/report_datasets_" + ArrayUtil.toString(datasetNames, "_", "", "", "");
	}

	public static String compoundTableFile(String... datasetNames)
	{
		return PWD + "reports/report_compounds_" + ArrayUtil.toString(datasetNames, "_", "", "", "");
	}

	public static String endpointTableFile(String... datasetNames)
	{
		return PWD + "reports/report_endpoints_" + ArrayUtil.toString(datasetNames, "_", "", "", "");
	}

	public static String validationReportFile(String experimentName, String[] datasetNames, String measures)
	{
		return PWD + "reports/report_validation_" + experimentName + "_"
				+ ArrayUtil.toString(datasetNames, "_", "", "", "") + "_" + measures;
	}

	public static String reportFile(String experimentName, String[] datasetNames, String measures)
	{
		return PWD + "reports/report_" + experimentName + "_" + ArrayUtil.toString(datasetNames, "_", "", "", "") + "_"
				+ measures;
	}

	public static String modelFile(String modelName)
	{
		return PWD + "models/" + modelName + ".model";
	}

	public static String modelPropsFile(String modelName)
	{
		return PWD + "models/" + modelName + ".settings";
	}

	public static String modelADFile(String modelName)
	{
		return PWD + "models/" + modelName + ".ad";
	}

	public static String modelDescriptionReport(String modelName)
	{
		return PWD + "models/" + modelName;
	}

	public static String compoundsArffFile(String compoundsName, String features)
	{
		return PWD + "predictions/" + compoundsName + "_" + features + ".arff";
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
		return PWD + "images/" + StringUtil.getMD5(smiles) + ".png";
		//return PWD + "predictions/" + smiles + ".png";
	}

	public static String imageFile(String identifier)
	{
		return PWD + "images/" + identifier + ".png";
	}

	public static String predictionOutfile(String modelName, String compoundsName)
	{
		return PWD + "predictions/" + modelName + "_" + compoundsName + ".html";
	}

	public static String appDomainImageFile(String modelName, String compoundsName, int compoundIndex, String endpoint)
	{
		return PWD + "predictions/" + modelName + "_" + compoundsName + "_" + compoundIndex
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
