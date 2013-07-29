import io.ExternalTool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import mlc.MLCDataInfo;
import mlc.reporting.ReportMLC;
import mulan.data.InvalidDataFormatException;
import util.ArrayUtil;
import util.CorrelationMatrix;
import util.CountedSet;
import util.FileUtil;
import util.FileUtil.CSVFile;
import util.StringUtil;
import weka.clusterers.ClusterEvaluation;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import com.itextpdf.text.DocumentException;

public class ClusterEndpoint
{
	public ClusterEndpoint(String inputFile, String outfile, double minTh, double highTh, String... clusterColumns)
	{
		this(FileUtil.readCSV(inputFile), outfile, minTh, highTh, clusterColumns);
	}

	public ClusterEndpoint(CSVFile file, String outfile, double minTh, double highTh, String... clusterColumns)
	{
		if (minTh >= highTh || minTh < 0 || highTh > 1)
			throw new IllegalStateException("illegal threshold params " + minTh + " - " + highTh);
		double centerTh = minTh + (highTh - minTh) / 2;

		try
		{
			for (String col : clusterColumns)
			{
				System.out.println(col);

				String valuesWithNullAndV[] = file.getColumn(col);
				System.out.println(ArrayUtil.toString(valuesWithNullAndV));

				List<Double> values = new ArrayList<Double>();
				for (int i = 0; i < valuesWithNullAndV.length; i++)
					if (valuesWithNullAndV[i] != null && !valuesWithNullAndV[i].equals("V"))
						values.add(Double.parseDouble(valuesWithNullAndV[i]));
				int num = values.size();
				Collections.sort(values);

				if (values.size() < 2)
					throw new IllegalArgumentException();

				Attribute attributes[] = new Attribute[] { new Attribute("val") };
				Instances dataset = new Instances("vals", (ArrayList<Attribute>) ArrayUtil.toList(attributes),
						values.size());
				for (Double v : values)
				{
					Instance i = new DenseInstance(1);
					i.setValue(0, v);
					dataset.add(i);
				}

				int numClusters = 2;
				Double thresholdValue = null;
				double ratio = 0;

				while (true)
				{
					//repeat until a split is found
					CascadeSimpleKMeans wekaClusterer = new CascadeSimpleKMeans();
					wekaClusterer.setPrintDebug(false);
					wekaClusterer.setMinNumClusters(numClusters);
					wekaClusterer.setMaxNumClusters(numClusters++);
					wekaClusterer.setSeed(0);
					wekaClusterer.setRestarts(10);

					ClusterEvaluation eval = new ClusterEvaluation();
					wekaClusterer.buildClusterer(dataset);
					eval.setClusterer(wekaClusterer);
					eval.evaluateClusterer(dataset);
					System.out.println("# of clusters: " + eval.getNumClusters());

					int lastCluster = -1;
					for (int i = 0; i < num; i++)
					{
						int c = (int) eval.getClusterAssignments()[i];
						if (lastCluster != -1 && c != lastCluster)
						{
							double tmpRatio = i / (double) num;
							if (tmpRatio >= minTh && tmpRatio <= highTh
									&& Math.abs(tmpRatio - centerTh) < Math.abs(ratio - centerTh))
							{
								ratio = tmpRatio;
								thresholdValue = values.get(i);
							}
						}
						lastCluster = c;
					}
					if (thresholdValue != null)
						break;
				}
				System.out.println("seperate into " + (ratio * num) + "/" + (num - (ratio * num)) + " "
						+ StringUtil.formatDouble(ratio));

				Integer cluster[] = new Integer[valuesWithNullAndV.length];
				for (int i = 0; i < valuesWithNullAndV.length; i++)
				{
					if (valuesWithNullAndV[i] == null)
						cluster[i] = null;
					//					else if (minNumLow < minNumHigh)
					//					{
					//						if (valuesWithNullAndV[i].equals("V"))
					//							throw new Exception(
					//									"adding V makes only sense if there are less compounds with high values");
					//						// if there are less compounds with low values, turn classes around
					//						if (Double.parseDouble(valuesWithNullAndV[i]) < thresholdValue)
					//							cluster[i] = 1;
					//						else
					//							cluster[i] = 0;
					//					}
					//					else
					//					{
					else if (valuesWithNullAndV[i].equals("V"))
						cluster[i] = 0;
					else
					{
						if (Double.parseDouble(valuesWithNullAndV[i]) < thresholdValue)
							cluster[i] = 1;
						else
							cluster[i] = 0;
					}
					//					}
				}

				System.out.println(CountedSet.fromArray(cluster));
				//				System.out.println(ArrayUtil.toString(cluster));
				System.out.println();

				file = file.exclude(col);
				file = file.addColumn(col, ArrayUtil.toStringArray(cluster));
			}

			FileUtil.writeCSV(outfile, file, false);
			System.out.println("\ncsv result file:\n" + outfile);

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void discretisize(String infile, String outfile, int numNoEndpointColumns, double lowThreshold,
			double highThreshold)
	{
		CSVFile file = FileUtil.readCSV(infile);
		String endpoints[] = file.getHeader();
		for (int i = 0; i < numNoEndpointColumns; i++)
			endpoints = ArrayUtil.removeAt(String.class, endpoints, 0);
		new ClusterEndpoint(file, outfile, lowThreshold, highThreshold, endpoints);
	}

	public static void main(String[] args) throws DocumentException, IOException, InvalidDataFormatException
	{
		//			MLCDataInfo di = MLCDataInfo.get(ReportMLC.getData("dataB-PC"));
		//			di.plotCorrelationMatrix(false, null, false);
		String endpoints[] = { "liver-weight-increased", "body-weight-decreased", "kidney-weight-increased", "cns",
				"rbc-haemoglobin", "spleen", "clinchem-nephrotox", "kidney", "liver", "liver-hypertrophy",
				"haematology-anaemia", "body-weight-gain-decreased", "rbc-erythrocytes", "liver-degeneration",
				"wbc-leucocytes", "clinchem-hepatotox", "female-reproductive-organ",
				"kidney-inflammation-regeneration", "heart", "wbc-lymphocytes", "kidney-degeneration",
				"adrenal-gland-weight-increased", "liver-inflammation-regeneration", "thymus-weight-decreased",
				"brain", "haematology-cellular-hemostasis", "rbc-haematocrit", "male-reproductive-organ-degeneration",
				"wbc", "intestine", "thyroid-gland", "adrenal-gland", "haematology-plasmatic-hemostasis",
				"thymus-degeneration", "male-reproductive-organ-weight-increased", "male-reproductive-organ-sperm",
				"haematopoiesis", "male-reproductive-organ-weight-decreased", "bone-marrow", "male-accessory-gland" };

		boolean create = true;
		boolean addVToHighValues = true;

		MLCDataInfo di = MLCDataInfo.get(ReportMLC.getData("dataB-PC"));
		CorrelationMatrix<Double> realMatrix = di.getRealValueCorrelationMatrix();

		//			for (double minLow : new double[] { 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7 })
		for (double minLow : new double[] { 0.6 })
		{
			double minHigh = Math.rint((minLow + 0.2) * 100) / 100; // to make sure that the double is exact at .X
			String name = "clust" + (int) (minLow * 100) + "to" + (int) (minHigh * 100);
			if (addVToHighValues)
				name += "V";
			System.out.println("\nXXXXXXXXXXX " + name + " XXXXXXXXXXX\n");

			if (create)
			{
				if (!new File("tmp/dataB-PC-" + name + ".arff").exists())
				{
					new ClusterEndpoint("data/tab_BMBF_RepDoseNeustoff_Kreuztab04062013_mitSmiles"
							+ (addVToHighValues ? "_V" : "") + ".csv",
							"data/tab_BMBF_RepDoseNeustoff_Kreuztab04062013_mitSmiles_" + name + ".csv", minLow,
							minHigh, endpoints);
					if (new File("tmp/dataB-PC-" + name + ".arff").exists())
					{
						new File("tmp/dataB-PC-" + name + ".arff").delete();
						new File("tmp/dataB-PC-" + name + ".xml").delete();
						new File("tmp/dataB-PC-" + name + ".csv").delete();
					}
					ExternalTool t = new ExternalTool();
					t.run("create arff/csv/xml",
							"ruby1.9.1 prepare_mlc.rb -e data/tab_BMBF_RepDoseNeustoff_Kreuztab04062013_mitSmiles_"
									+ name
									+ ".csv -f data/RepDoseNeustoff-2013-03-28.pc_descriptors.IDs.csv -r data/tab_BMBF_RepDoseNeustoff_Kreuztab04062013_mitSmiles"
									+ (addVToHighValues ? "_V" : "") + ".csv -n all -m all -d dataB-PC-" + name);
				}
			}
			else
			{
				di = MLCDataInfo.get(ReportMLC.getData("dataB-PC-" + name));
				CorrelationMatrix<Boolean> classMatrix = di.getClassCorrelationMatrix();
				System.out.println(realMatrix.rmse(classMatrix));
				//			SwingUtil.showInFrame(new JScrollPane(di.plotCorrelationMatrix(false, null)), "real");
				//			SwingUtil.showInFrame(new JScrollPane(di.plotCorrelationMatrix(true, null)), "class", true);

			}
		}
		System.exit(0);
	}
}
