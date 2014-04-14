package mlc.reporting;

import java.io.File;

import mlc.MLCDataInfo;
import mulan.data.MultiLabelInstances;
import mulan.evaluation.Settings;
import util.ArrayUtil;
import util.CountedSet;
import util.FileUtil;
import datamining.ResultSet;
import datamining.ResultSetIO;

public class MultiValidationReport
{

	public static void multiValidationReport(String experimentName, String[] datasetNames, String performanceMeasure)
			throws Exception
	{
		if (experimentName == null || datasetNames.length == 0 || performanceMeasure == null)
			throw new Error("experimentName and/or datasetNames and/or performanceMeasure missing");
		System.out.println("create result report for " + Settings.resultFile(experimentName, datasetNames));
		System.out.println("reading results:");
		ResultSet results = ResultSetIO.parseFromFile(new File(Settings.resultFile(experimentName, datasetNames)));

		//		{
		//			results = results.filter(new ResultSetFilter()
		//			{
		//				@Override
		//				public boolean accept(Result result)
		//				{
		//					return result.getValue("mlc-algorithm-params").toString().contains("confidences=true")
		//							&& result.getValue("mlc-algorithm-params").toString().contains("replacement=true")
		//							&& !result.getValue("mlc-algorithm-params").toString().contains("num-chains=15");
		//				}
		//			});
		//		}

		ReportMLC.PerformanceMeasures measures = ReportMLC.PerformanceMeasures.accuracy;
		measures = ReportMLC.PerformanceMeasures.valueOf(performanceMeasure);
		String outfile = Settings.reportFile(experimentName, datasetNames, measures.toString());
		MultiValidationReport.multiValidationReport(outfile, experimentName, datasetNames, results, measures);
	}

	public static void multiValidationReport(String outfile, String experimentName, ResultSet results,
			ReportMLC.PerformanceMeasures measures) throws Exception
	{
		MultiValidationReport.multiValidationReport(outfile, experimentName,
				ArrayUtil.cast(String.class, ArrayUtil.toArray(results.getResultValues("dataset-name").values())),
				results, measures);
	}

	public static void multiValidationReport(String outfile, String experimentName, String[] datasetNames,
			ResultSet results, ReportMLC.PerformanceMeasures measures) throws Exception
	{
		{
			//			if (datasetNames.length > 1)
			//				throw new IllegalStateException();

			MultiLabelInstances data = ReportMLC.getData(datasetNames[0]);
			MLCDataInfo di = MLCDataInfo.get(data);
			ResultSet join = results.join("mlc-algorithm");
			for (int i = 0; i < data.getNumLabels(); i++)
			{
				System.out.println(results.getResultValue(0, "label#" + i) + ","
						+ join.getResultValue(0, "macro-auc#" + i) + ","
						+ (di.ones_per_label[i] + di.zeros_per_label[i]) + "," + di.ones_per_label[i] + ","
						+ di.zeros_per_label[i]);
			}

		}

		//		String mod = "";
		//			if (args.length > 2)
		//			{
		//				mod = "_removed" + (args.length - 2);
		//				for (int i = 2; i < args.length; i++)
		//				{
		//					String excl[] = args[i].split(",");
		//					rs.remove(excl[0], excl[1]);
		//				}
		//			}

		ReportMLC rep = new ReportMLC(outfile, "Multi-Label-Classification (MLC) Results", "../");

		rep.addDatasetOverviewTable(datasetNames);

		//		CountedSet<String> classZero = new CountedSet<String>();
		//		CountedSet<String> classOne = new CountedSet<String>();
		//		for (Object datasetName : datasetNames)
		//		{
		//			MLCDataInfo di = MLCDataInfo.get(ReportMLC.getData(datasetName.toString()));
		//			classZero.add(di.getClassValuesZero());
		//			classOne.add(di.getClassValuesOne());
		//		}
		//		//			if (classZero.size() != 1 || classOne.size() != 1)
		//		//				throw new IllegalStateException("take care of different class values");
		//		String classZeroStr = ArrayUtil.toString(ArrayUtil.toArray(classZero.values()), "/", "", "", "");
		//		String classOneStr = ArrayUtil.toString(ArrayUtil.toArray(classOne.values()), "/", "", "", "");

		rep.report.newSection("Performance measures");
		rep.report.addTable(ReportMLC.getInfo(measures, MLCDataInfo.INACTIVE, MLCDataInfo.ACTIVE));

		//			System.out.println(results.toNiceString());

		final String singleStr = "Single endpoint prediction";
		for (int i = 0; i < results.getNumResults(); i++)
			if (results.getResultValue(i, "mlc-algorithm").toString().equals("BR"))
				results.setResultValue(i, "mlc-algorithm", singleStr);
			else if (results.getResultValue(i, "mlc-algorithm").toString().equals("ECC"))
				results.setResultValue(i, "mlc-algorithm", "Ensemble of classfier chains");

		String compareProps[] = new String[] { "dataset-name", "mlc-algorithm", "mlc-algorithm-params", "classifier",
				"imputation", "app-domain", "app-domain-params" };

		results.sortProperties(compareProps);
		for (String p : new String[] { "dataset-name", "mlc-algorithm-params", "classifier", "mlc-algorithm",
				"imputation", "app-domain", "app-domain-params" })
			results.sortResults(p);
		for (String p : ReportMLC.getProps(measures))
			results.movePropertyBack(p);
		results.movePropertyBack("runtime");

		String cmp1 = null;
		CountedSet<Object> cmpSet1 = null;
		String cmp2 = null;
		CountedSet<Object> cmpSet2 = null;
		for (String p : compareProps)
		{
			CountedSet<Object> set = results.getResultValues(p);
			if (set.getNumValues() > 1)
			{
				if (cmp1 == null)
				{
					cmp1 = p;
					cmpSet1 = set;
				}
				else if (cmp2 == null)
				{
					if (cmp1.equals("mlc-algorithm") && p.equals("mlc-algorithm-params")) // >1 mlc-alg, ignore different mlc-params
						continue;
					if (cmp1.equals("app-domain") && p.equals("app-domain-params"))
						continue;
					cmp2 = p;
					cmpSet2 = set;
				}
				else
				{
					if (cmp2.equals("mlc-algorithm") && p.equals("mlc-algorithm-params")) // >1 mlc-alg, ignore different mlc-params
						continue;
					if (cmp2.equals("app-domain") && p.equals("app-domain-params"))
						continue;
					throw new IllegalStateException("compare only two of those plz: "
							+ ArrayUtil.toString(compareProps) + ", different: " + cmp1 + " : " + cmpSet1 + ", " + cmp2
							+ " : " + cmpSet2 + ", " + p + " : " + set);
				}
			}
		}

		String boxSuffix = FileUtil.getFilename(Settings.resultFile(experimentName, datasetNames), false);
		if (cmp1 == null)
		{
			rep.addBoxPlots(results, "mlc-algorithm", "", boxSuffix, measures);
		}
		else if (cmp2 == null)
		{
			rep.addBoxPlots(results, cmp1, "", boxSuffix, measures);
		}
		else
		{
			for (Object val : cmpSet1.values())
			{
				ResultSet res = results.copy();
				res.exclude(cmp1, val);
				rep.addBoxPlots(res, cmp2, " (" + cmp1 + ": " + val + ")", boxSuffix + "_" + cmp1 + "-" + val, measures);
			}
			for (Object val : cmpSet2.values())
			{
				ResultSet res = results.copy();
				res.exclude(cmp2, val);
				rep.addBoxPlots(res, cmp1, " (" + cmp2 + ": " + val + ")", boxSuffix + "_" + cmp2 + "-" + val, measures);
			}
		}

		rep.close();
	}
}
