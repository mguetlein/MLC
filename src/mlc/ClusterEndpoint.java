package mlc;

import io.ExternalTool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import mlc.MLCDataInfo.StudyDuration;
import mlc.report.DiscMethod;
import mlc.reporting.ReportMLC;
import mulan.data.InvalidDataFormatException;
import util.ArrayUtil;
import util.CorrelationMatrix;
import util.CountedSet;
import util.DoubleArraySummary;
import util.FileUtil;
import util.FileUtil.CSVFile;
import util.ListUtil;
import util.StringUtil;
import weka.clusterers.ClusterEvaluation;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import com.itextpdf.text.DocumentException;

public class ClusterEndpoint extends DiscMethod
{
	public static enum Method
	{
		ratio, absolute
	}

	Method method;
	double minTh;
	double highTh;
	double centerTh;
	Double adjustChronic;
	String dataset;

	@Override
	public String getEndpointDescription(boolean active, int label, MLCDataInfo di)
	{
		if (method == Method.absolute)
		{
			if (adjustChronic != null)
			{
				if (active)
				{
					DoubleArraySummary accute = DoubleArraySummary.create(di.getStudyDurationValues(label,
							StudyDuration.accute, "1"));
					DoubleArraySummary chronic = DoubleArraySummary.create(di.getStudyDurationValues(label,
							StudyDuration.chronic, "1"));
					if (di.getLabelName(label).equals("thyroid-gland"))
					{
						System.out.println("accute inactive "
								+ ListUtil.toString(di.getStudyDurationValues(label, StudyDuration.accute, "0")));
						System.out.println("accute   active "
								+ ListUtil.toString(di.getStudyDurationValues(label, StudyDuration.accute, "1")));
						System.out.println("chronic inactive "
								+ ListUtil.toString(di.getStudyDurationValues(label, StudyDuration.chronic, "0")));
						System.out.println("chronic   active "
								+ ListUtil.toString(di.getStudyDurationValues(label, StudyDuration.chronic, "1")));
					}

					return "sub-accute <= " + StringUtil.formatDouble(accute.getMax()) + ", sub-chronic <= "
							+ StringUtil.formatDouble(chronic.getMax());
				}
				else
				{
					DoubleArraySummary accute = DoubleArraySummary.create(di.getStudyDurationValues(label,
							StudyDuration.accute, "0"));
					DoubleArraySummary chronic = DoubleArraySummary.create(di.getStudyDurationValues(label,
							StudyDuration.chronic, "0"));
					return "sub-accute >= " + StringUtil.formatDouble(accute.getMin()) + ", sub-chronic >= "
							+ StringUtil.formatDouble(chronic.getMin());
				}
			}
		}
		throw new Error("not yet implemted");
	}

	@Override
	public String getDescription()
	{
		return "none, yet";
	}

	@Override
	public String getName()
	{
		return "Cluster Disc";
	}

	@Override
	public String getShortName()
	{
		if (method == Method.ratio)
		{
			return "Cl" + (int) (minTh * 10) + (int) (highTh * 10);
		}
		else if (method == Method.absolute)
		{
			String c = "Ca";
			c += StringUtil.concatChar("" + (int) (minTh * 10), 2, '0', false);
			c += "-";
			c += StringUtil.concatChar("" + (int) (highTh * 10), 2, '0', false);
			if (adjustChronic != null)
			{
				c += "c";
				c += StringUtil.concatChar("" + (int) (adjustChronic * 10), 2, '0', false);
			}
			return c;
		}
		else
		{
			throw new Error();
		}
	}

	public static DiscMethod fromString(String s)
	{
		if (s.startsWith("C") && (s.length() == 7 || s.length() == 10))
		{
			Method m = (s.charAt(1) == 'a') ? Method.absolute : Method.ratio;
			double minTh = Integer.parseInt(s.substring(2, 4)) / 0.1;
			double highTh = Integer.parseInt(s.substring(5, 7)) / 0.1;
			Double adjustChronic = null;
			if (s.length() == 10)
				adjustChronic = Integer.parseInt(s.substring(8, 10)) / 0.1;
			return new ClusterEndpoint(m, minTh, highTh, adjustChronic);
		}
		return null;
	}

	public static void apply(String filename, Method method, double minTh, double highTh, Double adjustChronic)
	{
		ClusterEndpoint c = new ClusterEndpoint(method, minTh, highTh, adjustChronic);
		c.apply(filename);//, outfile);
	}

	public ClusterEndpoint(Method method, double minTh, double highTh, Double adjustChronic)
	{
		if (minTh >= highTh || minTh < 0 || (method == Method.ratio && highTh > 1))
			throw new IllegalStateException("illegal threshold params " + minTh + " - " + highTh);
		centerTh = minTh + (highTh - minTh) / 2;

		this.method = method;
		this.minTh = minTh;
		this.highTh = highTh;
		this.adjustChronic = adjustChronic;
	}

	private double adjustChronic(int compoundIndex)
	{
		if (adjustChronic == null)
			return 1.0;
		else
		{
			if (MLCDataInfo.getStudyDuration(dataset, compoundIndex) == StudyDuration.chronic)
				return adjustChronic;
			else
				return 1.0;
		}
	}

	public static int numCompounds;

	private void apply(String filename) //, String outfile)
	{
		this.dataset = FileUtil.getFilename(filename, false);
		CSVFile file = FileUtil.readCSV(filename);
		numCompounds = file.content.size() - 1;

		System.out.println("clustering [" + minTh + " - " + centerTh + " - " + highTh + "]");

		try
		{
			for (String col : ArrayUtil.removeAt(String.class, file.getHeader(), 0))
			{
				System.out.println(col);

				String valuesWithNullAndV[] = file.getColumn(col);
				System.out.println(ArrayUtil.toString(valuesWithNullAndV));

				List<Double> values = new ArrayList<Double>();
				for (int i = 0; i < valuesWithNullAndV.length; i++)
					if (valuesWithNullAndV[i] != null && !valuesWithNullAndV[i].equals("V"))
					{
						double d = Double.parseDouble(valuesWithNullAndV[i]);
						d *= adjustChronic(i);
						values.add(d);
					}
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

					double ratio = 0;
					double real = 0;
					Double tmpThresholdValue = null;

					int lastCluster = -1;
					for (int i = 0; i < num; i++)
					{
						int c = (int) eval.getClusterAssignments()[i];
						if (lastCluster != -1 && c != lastCluster)
						{
							double tmpRatio = i / (double) num;
							double tmpReal = (values.get(i - 1) + values.get(i)) / 2.0;
							//							System.out.print(StringUtil.formatDouble(tmpReal) + " ");

							boolean accept;
							if (method == Method.ratio)
								accept = Math.abs(tmpRatio - centerTh) < Math.abs(ratio - centerTh);
							else if (method == Method.absolute)
								accept = Math.abs(tmpReal - centerTh) < Math.abs(real - centerTh);
							else
								throw new Error("WTF");
							if (accept)
							{
								ratio = tmpRatio;
								real = tmpReal;
								tmpThresholdValue = values.get(i);
							}
						}
						lastCluster = c;
					}
					//					System.out.println();
					double min = values.get(0);
					double max = values.get(values.size() - 1);
					System.out.println("best split: " + (ratio * num) + "/" + (num - (ratio * num)) + " ; "
							+ StringUtil.formatDouble(ratio) + " ; [" + StringUtil.formatDouble(min) + " - "
							+ StringUtil.formatDouble(real) + " - " + StringUtil.formatDouble(max) + "] mmol");
					boolean accept;
					if (method == Method.ratio)
						accept = ratio >= minTh && ratio <= highTh;
					else if (method == Method.absolute)
						accept = real >= minTh && real <= highTh;
					else
						throw new Error("WTF");
					if (numClusters > num)
					{
						System.err.println("cut not possible in the defined threshold, taking the closest one");
						accept = true;
					}
					if (accept)
					{
						thresholdValue = tmpThresholdValue;
						break;
					}
				}
				boolean includesV = false;

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
					{
						cluster[i] = 0;
						includesV = true;
					}
					else
					{
						double d = Double.parseDouble(valuesWithNullAndV[i]);
						d *= adjustChronic(i);
						if (d < thresholdValue)
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

			String outfile = FileUtil.getParent(filename) + File.separator + FileUtil.getFilename(filename, false)
					+ "_" + getShortName() + "." + FileUtil.getFilenamExtension(filename);
			FileUtil.writeCSV(outfile, file, false);
			System.out.println("\ncsv result file:\n" + outfile);
		}
		catch (Exception e)
		{
			throw new Error(e);
		}
	}

	public static void main(String[] args) throws DocumentException, IOException, InvalidDataFormatException
	{
		//			MLCDataInfo di = MLCDataInfo.get(ReportMLC.getData("dataB-PC"));
		//			di.plotCorrelationMatrix(false, null, false);

		Boolean create = true;
		boolean addVToHighValues = false;

		//String dat = "dataA";
		String dat = "dataC";
		//int numEndpoints = 22;
		int numEndpoints = 40;
		//String feat = "PCFP";
		String feat = "dummy";
		String data;
		if (addVToHighValues)
			data = dat + "_withV";
		else
			data = dat + "_noV";
		//String feat_file = "dataF2";
		String feat_file = "dataFC";

		final MLCDataInfo diBase = MLCDataInfo.get(ReportMLC.getData(data + (addVToHighValues ? "_RvsV_" : "_EqF_")
				+ feat));
		CorrelationMatrix<Double> realMatrix = diBase.getRealValueCorrelationMatrix();
		CorrelationMatrix<Boolean> classMatrix = diBase.getClassCorrelationMatrix();
		System.out.println(diBase.getClassRatio());
		System.out.println(realMatrix.rmse(classMatrix));
		//		SwingUtil.showInFrame(new JScrollPane(di.plotCorrelationMatrix(false)), "real");
		//		SwingUtil.showInFrame(new JScrollPane(di.plotCorrelationMatrix(true)), "class-eqf");

		//			for (double minLow : new double[] { 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7 })

		HashMap<double[], Method> runs = new LinkedHashMap<double[], Method>();
		//		runs.put(new double[] { 0.5, 1.5 }, Method.absolute);
		//		runs.put(new double[] { 0.75, 1.75 }, Method.absolute);
		//		runs.put(new double[] { 1.0, 2.0 }, Method.absolute);
		//		runs.put(new double[] { 1.25, 2.25 }, Method.absolute);
		//		runs.put(new double[] { 1.5, 2.5 }, Method.absolute);
		//		runs.put(new double[] { 1.75, 2.75 }, Method.absolute);
		//		runs.put(new double[] { 2.0, 3.0 }, Method.absolute);
		//
		//		runs.put(new double[] { 0.5, 1.0 }, Method.absolute);
		//		runs.put(new double[] { 0.75, 1.25 }, Method.absolute);
		//		runs.put(new double[] { 1.0, 1.5 }, Method.absolute);
		//		runs.put(new double[] { 1.25, 1.75 }, Method.absolute);
		runs.put(new double[] { 1.5, 2.0 }, Method.absolute);
		//		runs.put(new double[] { 1.75, 2.25 }, Method.absolute);
		//		runs.put(new double[] { 2.0, 2.5 }, Method.absolute);
		//		runs.put(new double[] { 2.25, 2.75 }, Method.absolute);
		//		runs.put(new double[] { 2.5, 3.0 }, Method.absolute);

		//runs.put(new double[] { 0.6, 0.8 }, Method.ratio);

		for (double[] ths : runs.keySet())
		{
			for (final Double chronicAdjust : new Double[] { 2.0 })//{ null, 1.3, 1.6, 2.0, 2.3 })
			{
				double minLow = ths[0];
				double minHigh = ths[1];
				Method method = runs.get(ths);

				//			if (addVToHighValues)
				//				name += "V";
				ClusterEndpoint c = new ClusterEndpoint(method, minLow, minHigh, chronicAdjust);
				String name = c.getShortName();
				System.out.println("\nXXXXXXXXXXX " + name + " XXXXXXXXXXX\n");

				if ((create == null && !new File("arff/" + data + "_" + name + "_" + feat + ".arff").exists())
						|| (create != null && create))
				{
					c.apply("data/" + data + ".csv");
					ExternalTool t = new ExternalTool(null);
					t.run("create arff/csv/xml", new String[] { "ruby1.9.1", "prepare_mlc.rb", "-e",
							"data/" + data + "_" + name + ".csv", "-c", "data/" + dat + "_compoundInfo.csv", "-f",
							"features/" + feat_file + "_" + feat + ".csv", "-r", "data/" + data + ".csv", "-n",
							numEndpoints + "", "-m", (numEndpoints - 1) + "", "-d", data + "_" + name + "_" + feat },
							null, true, null, new File("/home/martin/workspace/BMBF-MLC"));
				}

				MLCDataInfo di = MLCDataInfo.get(ReportMLC.getData("" + data + "_" + name + "_" + feat));
				classMatrix = di.getClassCorrelationMatrix();
				System.out.println(di.getClassRatio());
				System.out.println(realMatrix.rmse(classMatrix));
				//				SwingUtil.showInFrame(new JScrollPane(di.plotCorrelationMatrix(true)), "class-" + name, true);

			}
		}
		System.exit(0);
	}
}
