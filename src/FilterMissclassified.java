import mulan.data.MultiLabelInstances;
import mulan.evaluation.Settings;
import mulan.evaluation.SinglePredictionTrackerUtil;

public class FilterMissclassified
{
	public static void doFilter(String datasetNames[], String experimentName, String endpoint) throws Exception
	{
		if (datasetNames.length == 0)
			throw new IllegalArgumentException("give dataset names to be filtered");
		for (String datasetName : datasetNames)
		{
			MultiLabelInstances dataset = new MultiLabelInstances(Settings.arffFile(datasetName),
					Settings.xmlFile(datasetName));

			String availableEndpoints[] = new String[dataset.getNumLabels()];
			for (int i = 0; i < dataset.getNumLabels(); i++)
				availableEndpoints[i] = dataset.getDataSet().attribute(dataset.getLabelIndices()[i]).name();

			String exportEndpoints[];
			if (endpoint.equals("all"))
				exportEndpoints = availableEndpoints;
			else
				exportEndpoints = new String[] { endpoint };

			for (String endp : exportEndpoints)
			{
				String filterEndpoints[] = new String[availableEndpoints.length - 1];
				int count = 0;
				for (int i = 0; i < availableEndpoints.length; i++)
					if (!availableEndpoints[i].equals(endp))
						filterEndpoints[count++] = availableEndpoints[i];
				SinglePredictionTrackerUtil.filterCsv(datasetName, experimentName, filterEndpoints, endp);
			}
			return;
		}
	}
}
