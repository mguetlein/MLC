package weka;

import java.util.ArrayList;
import java.util.List;

import util.FileUtil;
import util.FileUtil.CSVFile;
import util.FileUtil.ColumnExclude;

public class FilledArffWritable implements ArffWritable
{

	CSVFile csv;
	boolean isEndpoint[];
	boolean addID;

	public FilledArffWritable(CSVFile csv, boolean addID, String xmlFile)
	{
		csv = csv.exclude(new ColumnExclude()
		{
			@Override
			public boolean exclude(int columnIndex, String columnName)
			{
				return columnName.endsWith("_real");
			}
		});
		this.csv = csv;
		this.addID = addID;

		isEndpoint = new boolean[csv.getHeader().length];
		for (int i = 0; i < isEndpoint.length; i++)
		{
			isEndpoint[i] = !(csv.getHeader()[i].equals("id") || csv.getHeader()[i].startsWith("CDK:")
					|| csv.getHeader()[i].startsWith("OB:") || csv.getHeader()[i].startsWith("OB-FP") || csv
					.getHeader()[i].startsWith("OB-MACCS"));
			if (isEndpoint[i])
				System.err.println("is endpoint " + csv.getHeader()[i]);
		}

		if (xmlFile != null)
		{
			String s = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<labels xmlns=\"http://mulan.sourceforge.net/labels\">\n";
			for (int i = 0; i < isEndpoint.length; i++)
				if (isEndpoint[i])
					s += "<label name=\"" + csv.getHeader()[i] + "\"></label>\n";
			s += "</labels>\n";
			FileUtil.writeStringToFile(xmlFile, s);
		}
	}

	@Override
	public List<String> getAdditionalInfo()
	{
		ArrayList<String> l = new ArrayList<String>();
		l.add("missing endpoint values have been predicted");
		return l;
	}

	@Override
	public int getNumAttributes()
	{
		return csv.getHeader().length - (addID ? 0 : 1);
	}

	@Override
	public String getAttributeName(int attribute)
	{
		return csv.getHeader()[attribute + (addID ? 0 : 1)];
	}

	@Override
	public String getAttributeValueSpace(int attribute)
	{
		if (addID && attribute == 0)
			return "STRING";
		else if (isEndpoint[attribute + (addID ? 0 : 1)])
			return "{0,1}";
		else
			return "NUMERIC";
	}

	@Override
	public int getNumInstances()
	{
		return csv.content.size() - 1;
	}

	@Override
	public String getAttributeValue(int instance, int attribute)
	{
		return csv.content.get(instance + 1)[attribute + (addID ? 0 : 1)];
	}

	@Override
	public boolean isSparse()
	{
		return false;
	}

	@Override
	public String getMissingValue(int attribute)
	{
		return "?";
	}

	@Override
	public boolean isInstanceWithoutAttributeValues(int instance)
	{
		return false;
	}

}
