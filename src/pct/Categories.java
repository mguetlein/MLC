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

	public void add(int[] n)
	{
		categories.add(new Category(n));
	}

	public int numCategories()
	{
		return categories.size();
	}
}
