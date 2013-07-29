package mlc.reporting;

import java.awt.Dimension;
import java.io.File;
import java.io.FileReader;
import java.util.List;

import mlc.MLCDataInfo;
import mlc.ModelInfo;
import mlc.report.HTMLReport;
import mulan.classifier.MultiLabelLearner;
import mulan.classifier.MultiLabelOutput;
import mulan.data.MultiLabelInstances;
import mulan.evaluation.Settings;
import mulan.evaluation.measure.ConfidenceLevelProvider;
import util.ArrayUtil;
import util.FileUtil;
import util.StringUtil;
import weka.WekaUtil;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ArffLoader.ArffReader;
import appDomain.ADVisualization;
import appDomain.DistanceBasedMLCApplicabilityDomain;
import appDomain.MLCApplicabilityDomain;
import datamining.ResultSet;
import freechart.FreeChartUtil;

public class PredictCompounds
{
	String modelName;
	String compoundsName;

	ModelInfo modelInfo;
	MultiLabelLearner mlcAlgorithm;
	MLCApplicabilityDomain appDomain;
	Instances testData;
	String testDataSmiles[];
	String testDataInchi[];
	MultiLabelInstances trainingDataset;
	MLCDataInfo trainingDataInfo;

	private PredictCompounds()
	{
	}

	public static void predictCompounds(String modelName, String compoundsName)
	{
		PredictCompounds pc = new PredictCompounds();
		pc.modelName = modelName;
		pc.compoundsName = compoundsName;
		try
		{
			pc.init();
			pc.buildReport();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void predictAppDomain(String modelName, String compoundsName, int compoundIndex, String endpoint)
	{
		PredictCompounds pc = new PredictCompounds();
		pc.modelName = modelName;
		pc.compoundsName = compoundsName;
		try
		{
			pc.init();
			int labelIndex = -1;
			if (endpoint != null)
				for (int l = 0; l < pc.trainingDataset.getNumLabels(); l++)
				{
					String labelName = pc.trainingDataset.getDataSet()
							.attribute(pc.trainingDataset.getLabelIndices()[l]).name();
					if (endpoint.equals(labelName))
					{
						labelIndex = l;
						break;
					}
				}
			FreeChartUtil.toFile(
					Settings.appDomainImageFile(modelName, compoundsName, compoundIndex, endpoint),
					ADVisualization.getDistHistogramm(pc.modelInfo.getDataset(),
							(DistanceBasedMLCApplicabilityDomain) pc.appDomain, labelIndex,
							pc.testData.get(compoundIndex)).getChartPanel(), new Dimension(800, 500));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void init() throws Exception
	{
		if (modelName == null)
			throw new Error("specify model name");
		modelInfo = ModelInfo.get(modelName);
		mlcAlgorithm = (MultiLabelLearner) SerializationHelper.read(Settings.modelFile(modelName));

		appDomain = null;
		if (new File(Settings.modelADFile(modelName)).exists())
			appDomain = (MLCApplicabilityDomain) SerializationHelper.read(Settings.modelADFile(modelName));

		if (compoundsName == null)
			throw new Error("specify compoundsName");
		testData = new ArffReader(new FileReader(new File(Settings.compoundsArffFile(compoundsName,
				modelInfo.getFeatures())))).getData();
		if (testData.numInstances() == 0)
			throw new Error("no compounds found");

		String smilesContent = FileUtil.readStringFromFile(Settings.compoundsSmilesFile(compoundsName));
		testDataSmiles = smilesContent.split("\n");
		for (int i = 0; i < testDataSmiles.length; i++)
			testDataSmiles[i] = testDataSmiles[i].split("\t")[0];
		System.out.println(ArrayUtil.toString(testDataSmiles));

		String inchiContent = FileUtil.readStringFromFile(Settings.compoundsInchiFile(compoundsName));
		testDataInchi = inchiContent.split("\n");
		for (int i = 0; i < testDataInchi.length; i++)
			testDataInchi[i] = testDataInchi[i].split("\t")[0];
		System.out.println(ArrayUtil.toString(testDataInchi));

		trainingDataset = new MultiLabelInstances(Settings.arffFile(modelInfo.getDataset()), Settings.xmlFile(modelInfo
				.getDataset()));
		trainingDataInfo = MLCDataInfo.get(trainingDataset);

		if (testDataSmiles.length != testData.numInstances() || testDataSmiles.length != testDataInchi.length)
			throw new Error("num compounds does not match");
	}

	private List<Integer> getDatasetMatches(Instance inst, String inchi)
	{
		List<Integer> equalIndices = WekaUtil.indices(trainingDataset, inst);
		for (int i = 0; i < trainingDataset.getNumInstances(); i++)
			if (!equalIndices.contains(new Integer(i)) && inchi.equals(trainingDataInfo.getInchi(i)))
				equalIndices.add(i);
		return equalIndices;
	}

	private String linkToModel()
	{
		return "../../" + modelName;
	}

	private void buildReport() throws Exception
	{
		String out = Settings.predictionOutfile(modelName, compoundsName);
		HTMLReport report = new HTMLReport(out + ".locked", "Prediction Report");
		report.addParagraph("Prediction with model " + report.encodeLink(linkToModel(), modelName));

		// for each test instance
		for (int i = 0; i < testData.numInstances(); i++)
		{
			Instance inst = testData.get(i);
			System.out.println(inst);

			// add smiles and 2d picture
			report.newSection("Prediction " + (i + 1) + "/" + testData.numInstances() + " : " + testDataSmiles[i]);

			report.addParagraph("Inchi: " + testDataInchi[i]);

			ResultSet rs = new ResultSet();
			int r = rs.addResult();
			rs.setResultValue(r, "2D-depiction", report.getImage(Settings.compoundPicture(testDataSmiles[i])));
			if (i == 0)
				rs.setResultValue(
						r,
						"3D-depiction",
						report.getJSmolPlugin(compoundsName + ".sdf"
								+ (testData.numInstances() > 1 ? ("?idx=" + i) : "")));
			report.addTable(rs);

			// add number of training dataset occurences

			List<Integer> equalIndices = getDatasetMatches(inst, testDataInchi[i]);
			if (equalIndices.size() == 0)
				report.addParagraph("Compound is not included in the training dataset");
			else
			{
				String s = "Compound is included " + equalIndices.size() + " times in the training dataset (";
				for (int j = 0; j < equalIndices.size(); j++)
				{
					String idx = (equalIndices.get(j) + 1) + "";
					s += report.encodeLink(linkToModel() + "/compounds#" + idx, "#" + idx);
					if (j + 1 < equalIndices.size())
						s += (", ");
				}
				s += (")");
				report.addParagraph(s);
			}

			// add global app domain
			if (appDomain != null)
			{
				double p = appDomain.getApplicabilityDomainPropabilityCompleteDataset(inst);
				String pStr = p > 0 ? "inside" : "outside";
				if (appDomain.isContinous())
					pStr += " (" + StringUtil.formatDouble(p * 100, 1) + "%)";

				report.addParagraph("Checking if compound is within "
						+ report.encodeLink(linkToModel() + "/appdomain", "applicability domain")
						+ " of the complete dataset: "
						+ report.encodeLink(linkToModel() + "/predict/" + compoundsName + "/appdomain/" + i, pStr));

				//				report.addToggleImage("Inside total dataset app-domain: " + pStr, FreeChartUtil.toFile(
				//						Settings.imageFile(UUID.randomUUID().toString()),
				//						ADVisualization.getDistHistogramm(modelInfo.getDataset(),
				//								(DistanceBasedMLCApplicabilityDomain) appDomain, -1, inst).getChartPanel(),
				//						new Dimension(800, 500)));
			}
			report.addParagraph(" ");
			report.addGap();

			// add result for each endpoint
			MultiLabelOutput prediction = mlcAlgorithm.makePrediction(inst);
			ResultSet res = new ResultSet();
			for (int l = 0; l < trainingDataset.getNumLabels(); l++)
			{
				String labelName = trainingDataset.getDataSet().attribute(trainingDataset.getLabelIndices()[l]).name();
				boolean predicted = prediction.getBipartition()[l];
				int resCount = res.addResult();
				res.setResultValue(resCount, report.encodeLink(linkToModel() + "/endpoints", "endpoint"),
						report.encodeLink(linkToModel() + "/endpoints#" + labelName, labelName));

				// add activity for each training dataset occurence
				for (Integer equalIndex : equalIndices)
				{
					Instance equalInst = trainingDataset.getDataSet().get(equalIndex);
					String val;
					if (equalInst.isMissing(trainingDataset.getLabelIndices()[l]))
						val = trainingDataInfo.getMissingClassValueNice();
					else if (equalInst.value(trainingDataset.getLabelIndices()[l]) == 0)
						val = trainingDataInfo.getClassValuesZeroNice();
					else if (equalInst.value(trainingDataset.getLabelIndices()[l]) == 1)
						val = trainingDataInfo.getClassValuesOneNice();
					else
						throw new Error("WTF");

					if (!equalInst.isMissing(trainingDataset.getLabelIndices()[l]))
					{
						if (trainingDataInfo.hasRealData())
							val += " (" + StringUtil.formatDouble(trainingDataInfo.getRealValue(equalIndex, labelName))
									+ "" + trainingDataInfo.getRealValueUnit() + ")";
						res.setResultValue(
								resCount,
								report.encodeLink(linkToModel() + "/compounds#" + (equalIndex + 1),
										"measured activity (#" + (equalIndex + 1)) + ")", val);
					}
				}

				boolean outsideAD = false;
				// add endpoint depending app-domain
				if (appDomain != null)
				{
					double p = appDomain.getApplicabilityDomainPropability(inst, l);
					outsideAD = p == 0;
					String pStr = p > 0 ? "inside" : "outside";
					if (appDomain.isContinous())
						pStr += " (" + StringUtil.formatDouble(p * 100, 1) + "%)";

					res.setResultValue(
							resCount,
							report.encodeLink(linkToModel() + "/appdomain", "applicability domain"),
							report.encodeLink(linkToModel() + "/predict/" + compoundsName + "/appdomain/" + i + "/"
									+ labelName, pStr));

					//					res.setResultValue(
					//							resCount,
					//							"inside app-domain",
					//							new HTMLReport.Toggler(pStr, new HTMLReport.Image(FreeChartUtil.toFile(
					//									Settings.imageFile(UUID.randomUUID().toString()),
					//									ADVisualization.getDistHistogramm(modelInfo.getDataset(),
					//											(DistanceBasedMLCApplicabilityDomain) appDomain, l, inst).getChartPanel(),
					//									new Dimension(800, 500)))));
				}

				// add prediciton result
				System.out.println(trainingDataset.getDataSet().attribute(trainingDataset.getLabelIndices()[l]).name()
						+ " " + prediction.getConfidences()[l]);
				res.setResultValue(
						resCount,
						report.encodeLink(linkToModel() + "/validation#model-confidence", "predicted activity"),
						(predicted ? trainingDataInfo.getClassValuesOneNice() : trainingDataInfo
								.getClassValuesZeroNice())
								+ " ("
								+ trainingDataInfo.getConfidenceLevelNice(prediction.getConfidences()[l]) + ")");

				// add validation result
				if (!modelInfo.isValidated())
					throw new Error("please validate model first");
				if (!outsideAD)
				{
					double propCorrect;
					if (predicted)
					{
						propCorrect = modelInfo.getValidationResult("macro-ppv", l,
								ConfidenceLevelProvider.getConfidence(prediction.getConfidences()[l]));
						//						res.setResultValue(resCount, "prop inactive",
						//								StringUtil.formatDouble(100 * (1 - propActive), 1));
					}
					else
					{
						propCorrect = modelInfo.getValidationResult("macro-npv", l,
								ConfidenceLevelProvider.getConfidence(prediction.getConfidences()[l]));
						//						res.setResultValue(resCount, "prop active",
						//								StringUtil.formatDouble(100 * (1 - propInactive), 1));
					}

					String pct = StringUtil.formatDouble(100 * propCorrect, 1) + "%";
					res.setResultValue(resCount, report.encodeLink(
							linkToModel() + "/validation#"
									+ Settings.text("probability-correct").toLowerCase().replaceAll(" ", "-"),
							"prediction correct"), report.encodeLink(linkToModel() + "/validation#" + labelName, pct));
				}
			}
			report.addTable(res);
		}
		report.close();
		if (!new File(out + ".locked").exists())
			throw new Error("could not find report");
		if (!FileUtil.robustRenameTo(out + ".locked", out))
			throw new Error("could not rename file");
		System.out.println("report created: " + out);
	}

}
