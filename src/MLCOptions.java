public class MLCOptions
{
	private int numCores = 1;
	private String classifier = "SMO";
	private String mlcAlgorithm = "ECC";
	private int numFolds = 10;
	private int minSeed = 0;
	private int maxSeedExclusive = 1;
	private String mlcAlgorithmParams;
	private String datasetName;
	private String experimentName;
	private String imputation = "false";
	private String fillMissing = null;
	private String appDomain = "None";
	private String appDomainParams;

	private static String[] split(String s)
	{
		if (s == null || s.length() == 0)
			return new String[0];
		else
			return s.split(",");
	}

	public int getNumCores()
	{
		return numCores;
	}

	public void setNumCores(int numCores)
	{
		this.numCores = numCores;
	}

	public String[] getClassifiers()
	{
		return split(classifier);
	}

	public void setClassifier(String classifier)
	{
		this.classifier = classifier;
	}

	public String[] getMlcAlgorithms()
	{
		return split(mlcAlgorithm);
	}

	public void setMlcAlgorithm(String mlcAlgorithm)
	{
		this.mlcAlgorithm = mlcAlgorithm;
	}

	public int getNumFolds()
	{
		return numFolds;
	}

	public void setNumFolds(int numFolds)
	{
		this.numFolds = numFolds;
	}

	public int getMinSeed()
	{
		return minSeed;
	}

	public void setMinSeed(int minSeed)
	{
		this.minSeed = minSeed;
	}

	public int getMaxSeedExclusive()
	{
		return maxSeedExclusive;
	}

	public void setMaxSeedExclusive(int maxSeedExclusive)
	{
		this.maxSeedExclusive = maxSeedExclusive;
	}

	public String[] getMlcAlgorithmParams()
	{
		return split(mlcAlgorithmParams);
	}

	public void setMlcAlgorithmParams(String mlcAlgorithmParams)
	{
		this.mlcAlgorithmParams = mlcAlgorithmParams;
	}

	public String[] getDatasetNames()
	{
		return split(datasetName);
	}

	public void setDatasetName(String datasetName)
	{
		this.datasetName = datasetName;
	}

	public String getExperimentName()
	{
		return experimentName;
	}

	public void setExperimentName(String experimentName)
	{
		this.experimentName = experimentName;
	}

	public String[] getImputations()
	{
		return split(imputation);
	}

	public void setImputation(String imputation)
	{
		this.imputation = imputation;
	}

	public void setFillMissing(String fillMissing)
	{
		this.fillMissing = fillMissing;
	}

	public String[] getFillMissings()
	{
		return split(fillMissing);
	}

	public void setAppDomain(String appDomain)
	{
		this.appDomain = appDomain;
	}

	public String[] getAppDomains()
	{
		return split(appDomain);
	}

	public void setAppDomainParams(String appDomainParams)
	{
		this.appDomainParams = appDomainParams;
	}

	public String[] getAppDomainParams()
	{
		return split(appDomainParams);
	}

}
