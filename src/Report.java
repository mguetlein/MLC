import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import util.StringUtil;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.codec.PngImage;

import datamining.ResultSet;

public class Report
{
	private static Font FONT_TITLE = new Font(Font.FontFamily.TIMES_ROMAN, 18, Font.BOLD);

	private static Font FONT_SUB_TITLE = new Font(Font.FontFamily.TIMES_ROMAN, 14, Font.BOLD);

	private static Font FONT_TEXT = new Font(Font.FontFamily.TIMES_ROMAN, 8);
	private static Font FONT_TEXT_BOLD = new Font(Font.FontFamily.TIMES_ROMAN, 8, Font.BOLD);

	Document document;

	public Report(String outfile, String title)
	{
		try
		{
			document = new Document();
			PdfWriter.getInstance(document, new FileOutputStream(outfile));
			document.open();

			document.addTitle(title);
			//		document.addSubject("Using iText");
			//		document.addKeywords("Java, PDF, iText");
			//		document.addAuthor("Lars Vogel");
			//		document.addCreator("Lars Vogel");

			addTitle(title);

			//			addContent(document);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void close()
	{
		document.close();
	}

	private void addTitle(String title) throws DocumentException
	{
		Paragraph preface = new Paragraph();
		preface.add(new Paragraph(title, FONT_TITLE));
		preface.add(new Paragraph(new Date().toString(), FONT_TEXT));
		addEmptyLine(preface, 1);
		document.add(preface);
		//		document.newPage();
	}

	//	public void addChapter(String title, HashMap<String, String[]> sections) throws DocumentException
	//	{
	//		Anchor anchor = new Anchor(title, FONT_TITLE);
	//		anchor.setName(title);
	//		Chapter catPart = new Chapter(new Paragraph(anchor), 1);
	//		for (String subtitle : sections.keySet())
	//		{
	//			Paragraph subPara = new Paragraph(subtitle, FONT_SUB_TITLE);
	//			Section subCatPart = catPart.addSection(subPara);
	//			subCatPart.add(new Paragraph(ArrayUtil.toString(sections.get(subtitle))));
	//		}
	//		document.add(catPart);
	//	}

	//	public void addSection(String title, String content) throws DocumentException, IOException
	//	{
	//		addSection(title, content, null);
	//	}

	public void addSection(String title, String content, ResultSet[] tables, File[] images) throws DocumentException,
			IOException
	{
		Paragraph subPara = new Paragraph(title, FONT_SUB_TITLE);
		subPara.add(new Paragraph(content, FONT_TEXT));
		if (tables != null && tables.length > 0)
		{
			addEmptyLine(subPara, 1);
			for (ResultSet resultSet : tables)
			{
				subPara.add(table(resultSet));
			}
		}
		if (images != null && images.length > 0)
			for (File file : images)
			{
				Image image = PngImage.getImage(file.getAbsolutePath());
				float scaler = ((document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin() - 10) / image
						.getWidth()) * 100;
				image.scalePercent(scaler);
				subPara.add(image);
			}
		addEmptyLine(subPara, 1);
		document.add(subPara);
	}

	//	private static void addContent(Document document) throws DocumentException, IOException
	//	{
	//		Anchor anchor = new Anchor("First Chapter", FONT_TITLE);
	//		anchor.setName("First Chapter");
	//
	//		// Second parameter is the number of the chapter
	//		Chapter catPart = new Chapter(new Paragraph(anchor), 1);
	//
	//		Paragraph subPara = new Paragraph("Subcategory 1", FONT_SUB_TITLE);
	//		Section subCatPart = catPart.addSection(subPara);
	//		subCatPart.add(new Paragraph("Hello"));
	//
	//		subPara = new Paragraph("Subcategory 2", FONT_SUB_TITLE);
	//		subCatPart = catPart.addSection(subPara);
	//		subCatPart.add(new Paragraph("Paragraph 1"));
	//		subCatPart.add(new Paragraph("Paragraph 2"));
	//		subCatPart.add(new Paragraph("Paragraph 3"));
	//
	//		// Add a list
	//		createList(subCatPart);
	//		Paragraph paragraph = new Paragraph();
	//
	//		Image image1 = PngImage.getImage("/home/martin/workspace/JavaLib/data/ches-mapper.png");
	//		paragraph.add(image1);
	//
	//		addEmptyLine(paragraph, 5);
	//		subCatPart.add(paragraph);
	//
	//		// Add a table
	//		createTable(subCatPart);
	//
	//		// Now add all this to the document
	//		document.add(catPart);
	//
	//		// Next section
	//		anchor = new Anchor("Second Chapter", FONT_TITLE);
	//		anchor.setName("Second Chapter");
	//
	//		// Second parameter is the number of the chapter
	//		catPart = new Chapter(new Paragraph(anchor), 1);
	//
	//		subPara = new Paragraph("Subcategory", FONT_SUB_TITLE);
	//		subCatPart = catPart.addSection(subPara);
	//		subCatPart.add(new Paragraph("This is a very important message"));
	//
	//		// Now add all this to the document
	//		document.add(catPart);
	//
	//	}

	private static PdfPTable table(ResultSet rs)
	{
		PdfPTable table = new PdfPTable(rs.getProperties().size());

		for (String p : rs.getProperties())
		{
			PdfPCell c1 = new PdfPCell(new Phrase(p, FONT_TEXT_BOLD));
			c1.setHorizontalAlignment(Element.ALIGN_CENTER);
			table.addCell(c1);
		}
		table.setHeaderRows(1);

		for (int i = 0; i < rs.getNumResults(); i++)
			for (String p : rs.getProperties())
			{
				Object val = rs.getResultValue(i, p);
				if (val instanceof Double)
					table.addCell(new Phrase(StringUtil.formatDouble((Double) val), FONT_TEXT));
				else
					table.addCell(new Phrase(val + "", FONT_TEXT));

			}
		return table;
	}

	//	private static void createTable(Section subCatPart) throws BadElementException
	//	{
	//		PdfPTable table = new PdfPTable(3);
	//
	//		// t.setBorderColor(BaseColor.GRAY);
	//		// t.setPadding(4);
	//		// t.setSpacing(4);
	//		// t.setBorderWidth(1);
	//
	//		PdfPCell c1 = new PdfPCell(new Phrase("Table Header 1"));
	//		c1.setHorizontalAlignment(Element.ALIGN_CENTER);
	//		table.addCell(c1);
	//
	//		c1 = new PdfPCell(new Phrase("Table Header 2"));
	//		c1.setHorizontalAlignment(Element.ALIGN_CENTER);
	//		table.addCell(c1);
	//
	//		c1 = new PdfPCell(new Phrase("Table Header 3"));
	//		c1.setHorizontalAlignment(Element.ALIGN_CENTER);
	//		table.addCell(c1);
	//		table.setHeaderRows(1);
	//
	//		table.addCell("1.0");
	//		table.addCell("1.1");
	//		table.addCell("1.2");
	//		table.addCell("2.1");
	//		table.addCell("2.2");
	//		table.addCell("2.3");
	//
	//		subCatPart.add(table);
	//
	//	}
	//
	//	private static void createList(Section subCatPart)
	//	{
	//		com.itextpdf.text.List list = new com.itextpdf.text.List(true, false, 10);
	//		list.add(new ListItem("First point"));
	//		list.add(new ListItem("Second point"));
	//		list.add(new ListItem("Third point"));
	//		subCatPart.add(list);
	//	}

	private static void addEmptyLine(Paragraph paragraph, int number)
	{
		for (int i = 0; i < number; i++)
		{
			paragraph.add(new Paragraph(" "));
		}
	}

	public void newPage()
	{
		document.newPage();

	}

}
