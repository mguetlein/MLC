package mlc.report;

import mlc.ClusterEndpoint;
import mlc.MLCDataInfo;

public abstract class DiscMethod
{
	public abstract String getShortName();

	public abstract String getName();

	public abstract String getDescription();

	public abstract String getEndpointDescription(boolean active, int label, MLCDataInfo di);

	public static DiscMethod fromString(String s)
	{
		DiscMethod m = ClusterEndpoint.fromString(s);
		if (m == null)
			throw new Error("unknown disc method");
		return m;
	}
}
