import java.util.ArrayList;
import java.util.List;

import util.ArrayUtil;
import util.FileUtil;
import util.FileUtil.CSVFile;

public class MissclassifiedCSVSplitter
{
	public MissclassifiedCSVSplitter(String file, boolean createChesMapperSettingsFile)
	{
		System.out.println("reading " + file);
		CSVFile csv = FileUtil.readCSV(file);

		//System.out.println(ArrayUtil.toString(csv.getHeader()));

		final List<String> endpoints = new ArrayList<String>();
		for (String s : csv.getHeader())
		{
			if (s.matches(".*_missclassified$"))
			{
				String endpoint = s.substring(0, s.lastIndexOf("_"));
				if (!endpoint.equals("all_endpoints"))
				{
					if (ArrayUtil.indexOf(csv.getHeader(), endpoint) == -1)
						throw new Error("not found: " + endpoint);
					endpoints.add(endpoint);
				}
			}
		}
		//		System.out.println(ListUtil.toString(endpoints));

		//for (final String endpoint : endpoints)
		for (int i = 0; i < endpoints.size(); i++)
		{
			final String endpoint = endpoints.get(endpoints.size() - (i + 1));
			System.out.println(endpoint);

			CSVFile newCsv = csv.exclude(new FileUtil.ColumnExclude()
			{
				@Override
				public boolean exclude(int columnIndex, String columnName)
				{
					String ep = columnName;
					if (ep.endsWith("_missclassified") || ep.endsWith("_classified") || ep.endsWith("_real"))
						ep = ep.substring(0, ep.lastIndexOf("_"));
					return (!ep.equals(endpoint) && endpoints.contains(ep)) || ep.contains("all_endpoints");
				}
			});

			final int endpointColumn = ArrayUtil.indexOf(newCsv.getHeader(), endpoint);
			//			System.out.println(ArrayUtil.toString(newCsv.getHeader()));
			//			System.out.println(newCsv.content.size());

			newCsv = newCsv.removeRow(new FileUtil.RowRemove()
			{
				@Override
				public boolean remove(int rowIndex, String[] row)
				{
					return row[endpointColumn] == null || row[endpointColumn].length() == 0;
				}
			});

			//			System.out.println(newCsv.content.size());

			if (!file.endsWith("_missclassified.csv"))
				throw new IllegalArgumentException(file);
			int index = file.lastIndexOf("_");
			String newFile = file.substring(0, index) + "_" + endpoint + file.substring(index);
			System.out.println("writing to " + newFile + " (instances: " + newCsv.content.size() + ")");
			FileUtil.writeCSV(newFile, newCsv, false);

			if (createChesMapperSettingsFile)
			{

				//				List<String> features = new ArrayList<String>();
				//				for (String s : newCsv.getHeader())
				//				{
				//					if (s.startsWith(endpoint)
				//							|| ArrayUtil.indexOf(new String[] { "SMILES", "Name", "CAS", "DSSTox_FileID",
				//									"TestSubstance_ChemicalName", "TestSubstance_CASRN" }, s) != -1)
				//					{
				//						//					System.err.println("skip " + s);
				//					}
				//					else
				//						features.add(s);
				//				}
				//				Properties p = MappingWorkflow.createMappingWorkflow(newFile, ListUtil.toArray(features),
				//						NoClusterer.INSTANCE);
				//				String chesFile = newFile.substring(0, newFile.lastIndexOf(".")) + ".ches";
				//				System.out.println("writing to " + chesFile);
				//				MappingWorkflow.exportMappingWorkflowToFile(p, chesFile);
			}
		}

	}

	public static void main(String args[])
	{
		new MissclassifiedCSVSplitter("tmp/cpdb_ECC_BR_missclassified.csv", false);
		//new MissclassifiedCSVSplitter("tmp/dataC_ECC_BR_missclassified.csv");
	}
}
