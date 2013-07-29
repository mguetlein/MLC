package appDomain;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Arrays;

import util.DoubleArraySummary;
import util.StringLineAdder;
import weka.core.DenseInstance;
import weka.core.DistanceFunction;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader.ArffReader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

public class CentroidBasedApplicabilityDomain extends AbstractDistanceBasedApplicabilityDomain
{
	Instances data;
	protected ReplaceMissingValues missingValuesConverter;
	protected Instance meanInstance;
	protected DistanceFunction distanceFunction;
	protected double[] trainingDistances;
	protected double averageDistanceToCentroid;
	protected double maxDistanceToCentroid;

	@Override
	public String getDistanceDescription()
	{
		return "Distance to training dataset centroid";
	}

	@Override
	public void init(Instances origData) throws Exception
	{
		missingValuesConverter = new ReplaceMissingValues();
		data = new Instances(origData);
		missingValuesConverter.setInputFormat(data);
		data = Filter.useFilter(data, missingValuesConverter);

		meanInstance = new DenseInstance(data.numAttributes());
		for (int i = 0; i < data.numAttributes(); i++)
			meanInstance.setValue(i, data.meanOrMode(i));

		distanceFunction = new EuclideanDistance(data);

		trainingDistances = new double[data.numInstances()];
		for (int i = 0; i < data.numInstances(); i++)
			trainingDistances[i] = distanceFunction.distance(meanInstance, data.get(i));

		DoubleArraySummary sum = DoubleArraySummary.create(trainingDistances);
		if (method == Method.median)
			averageDistanceToCentroid = sum.getMedian();
		else
			averageDistanceToCentroid = sum.getMean();
		maxDistanceToCentroid = sum.getMax();
		Arrays.sort(trainingDistances);
		//		if (DEBUG)
		//			System.out.println("AD is dist-to-centroid <= " + factor + " * " + StringUtil.formatDouble(medianDistance)
		//					+ " ( " + StringUtil.formatDouble(factor * medianDistance) + " )");
	}

	@Override
	public double getDistance(Instance inst)
	{
		missingValuesConverter.input(inst);
		missingValuesConverter.batchFinished();
		inst = missingValuesConverter.output();
		double dist = distanceFunction.distance(meanInstance, inst);
		//		if (dist > 25 * medianDistance)
		//		{
		//			//			for (int i = 0; i < inst.numAttributes(); i++)
		//			//			{
		//			//				System.out.println(StringUtil.concatWhitespace(inst.attribute(i).name(), 25) + " "
		//			//						+ StringUtil.concatWhitespace(StringUtil.formatDouble(inst.value(i)), 10) + " "
		//			//						+ StringUtil.concatWhitespace(StringUtil.formatDouble(meanInstance.value(i)), 10));
		//			//			}
		//			System.err.println("large outlier, distance is " + dist + ", medianDistance was " + medianDistance);
		//			return null;
		//		}
		return dist;
	}

	@Override
	public Instances getData()
	{
		return data;
	}

	@Override
	public double getAverageTrainingDistance()
	{
		return averageDistanceToCentroid;
	}

	@Override
	public double getMaxTrainingDistance()
	{
		return maxDistanceToCentroid;
	}

	@Override
	public double[] getTrainingDistances()
	{
		return trainingDistances;
	}

	public static void main(String args[]) throws Exception
	{
		StringLineAdder s = new StringLineAdder();
		s.add("@RELATION test");
		s.add("");
		s.add("@ATTRIBUTE attr1  REAL");
		s.add("@ATTRIBUTE attr2  REAL");
		s.add("@ATTRIBUTE class {T,F}");
		s.add("");
		s.add("@DATA");
		s.add("5.1,10000.0,T");
		s.add("4.9,9000.0,F");
		s.add("5.1,11000.0,T");
		s.add("4.5,12000.0,T");
		s.add("4.9,?,F");
		s.add("?,?,F");

		ArffReader r = new ArffReader(new BufferedReader(new StringReader(s.toString())));
		Instances data = r.getData();
		data.setClassIndex(data.numAttributes() - 1);
		CentroidBasedApplicabilityDomain ad = new CentroidBasedApplicabilityDomain();
		ad.debug = true;
		ad.init(data);

		for (int i = 0; i < data.numInstances(); i++)
			ad.isInside(data.get(i));
	}

	@Override
	public DistanceBasedApplicabilityDomain copy()
	{
		CentroidBasedApplicabilityDomain ad = new CentroidBasedApplicabilityDomain();
		ad.setMethod(method);
		ad.setDistanceMultiplier(distanceMultiplier);
		ad.setContinous(continous);
		ad.setContinousFullDistanceMultiplier(continousFullDistanceMultiplier);
		return ad;
	}

}
