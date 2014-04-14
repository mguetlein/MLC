package pct;

import java.util.ArrayList;
import java.util.List;

import mulan.evaluation.Settings;
import util.ArrayUtil;
import util.FileUtil;

public class Categories
{
	List<Category> categories = new ArrayList<Category>();

	public Categories()
	{
	}

	public void writeToFile(String modelName)
	{
		String s = "";
		for (Category c : categories)
			s += c.toCSV() + "\n";
		String f = Settings.modelCategoriesFile(modelName);
		System.out.println("writing categories to file: " + f);
		FileUtil.writeStringToFile(f, s);
	}

	public boolean includes(int i)
	{
		for (Category cat : categories)
			if (ArrayUtil.indexOf(cat.instances, i) != -1)
				return true;
		return false;
	}

	public String toString()
	{
		StringBuffer b = new StringBuffer();
		for (Category c : categories)
		{
			b.append(c.toString());
			b.append("\n");
		}
		return b.toString();
	}

	public String getCompactString()
	{
		StringBuffer b = new StringBuffer();
		int idx = 0;
		for (Category c : categories)
		{
			b.append("\"");
			b.append(c.getName());
			b.append("\"");
			if (++idx < categories.size())
				b.append(",");
		}
		return b.toString();
	}

	public void add(int[] n)
	{
		for (int i : n)
			if (includes(i))
				throw new Error("index '" + i + "' already included, cannot add '" + ArrayUtil.toString(n) + "'\n"
						+ this);
		categories.add(new Category(n));
	}

	public int numCategories()
	{
		return categories.size();
	}
}
