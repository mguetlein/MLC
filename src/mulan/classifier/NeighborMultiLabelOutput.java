package mulan.classifier;

public class NeighborMultiLabelOutput extends MultiLabelOutput
{
	protected int neighborInstances[];

	public NeighborMultiLabelOutput(boolean[] bipartition, double[] someConfidences, int neighborInstances[])
	{
		super(bipartition, someConfidences);
		this.neighborInstances = neighborInstances;
	}

	public int[] getNeighborInstances()
	{
		return neighborInstances;
	}
}
