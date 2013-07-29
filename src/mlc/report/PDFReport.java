package mlc.report;


public class PDFReport extends AbstractReport
{
	//	Document pdfDocument;
	//
	//	public PDFReport(String outfile, String title)
	//	{
	//		super(outfile, title);
	//		try
	//		{
	//			outfile += ".pdf";
	//			pdfDocument = new Document(PageSize.A4.rotate());
	//			PdfWriter.getInstance(pdfDocument, new FileOutputStream(outfile));
	//			pdfDocument.open();
	//			pdfDocument.addTitle(title);
	//			addTitle(title);
	//		}
	//		catch (Exception e)
	//		{
	//			e.printStackTrace();
	//		}
	//	}
	//
	//	public void close()
	//	{
	//		pdfDocument.close();
	//	}
	//
	//	private void addTitle(String title) throws DocumentException
	//	{
	//		Paragraph preface = new Paragraph();
	//		preface.add(new Paragraph(title, FONT_TITLE));
	//		preface.add(new Paragraph(new Date().toString(), FONT_TEXT));
	//		addEmptyLine(preface, 1);
	//		pdfDocument.add(preface);
	//		//		document.newPage();
	//	}
	//
	//	//	public void addChapter(String title, HashMap<String, String[]> sections) throws DocumentException
	//	//	{
	//	//		Anchor anchor = new Anchor(title, FONT_TITLE);
	//	//		anchor.setName(title);
	//	//		Chapter catPart = new Chapter(new Paragraph(anchor), 1);
	//	//		for (String subtitle : sections.keySet())
	//	//		{
	//	//			Paragraph subPara = new Paragraph(subtitle, FONT_SUB_TITLE);
	//	//			Section subCatPart = catPart.addSection(subPara);
	//	//			subCatPart.add(new Paragraph(ArrayUtil.toString(sections.get(subtitle))));
	//	//		}
	//	//		document.add(catPart);
	//	//	}
	//
	//	//	public void addSection(String title, String content) throws DocumentException, IOException
	//	//	{
	//	//		addSection(title, content, null);
	//	//	}
	//
	//	public void addSection(String title, String content, ResultSet[] tables, File[] images, File[] smallImages)
	//	{
	//		try
	//		{
	//			Paragraph subPara = new Paragraph(title, FONT_SUB_TITLE);
	//			if (content != null && content.length() > 0)
	//				subPara.add(new Paragraph(content, FONT_TEXT));
	//			if (tables != null && tables.length > 0)
	//			{
	//				addEmptyLine(subPara, 1);
	//				for (ResultSet resultSet : tables)
	//				{
	//					subPara.add(table(resultSet));
	//				}
	//			}
	//			if (images != null && images.length > 0)
	//				for (File file : images)
	//				{
	//					Image image = PngImage.getImage(file.getAbsolutePath());
	//					float docH = (pdfDocument.getPageSize().getHeight() - pdfDocument.topMargin()
	//							- pdfDocument.bottomMargin() - 10);
	//					float docW = (pdfDocument.getPageSize().getWidth() - pdfDocument.leftMargin()
	//							- pdfDocument.rightMargin() - 10);
	//					if (image.getHeight() > docH || image.getWidth() > docW)
	//					{
	//						System.out.println("to big " + image.getWidth() + "x" + image.getHeight());
	//						float scaleH = (docH / image.getHeight()) * 100;
	//						float scaleW = (docW / image.getWidth()) * 100;
	//						float scale = Math.min(scaleH, scaleW);
	//						image.scalePercent(scale);
	//						System.out.println("-> " + image.getWidth() + "x" + image.getHeight());
	//					}
	//					//image.setRotationDegrees(45f);
	//					subPara.add(image);
	//				}
	//			if (smallImages != null && smallImages.length > 0)
	//			{
	//				addEmptyLine(subPara, 15);
	//				PdfPTable table = new PdfPTable(2);
	//				for (int i = 0; i < smallImages.length; i++)
	//				{
	//					Image image = PngImage.getImage(smallImages[i].getAbsolutePath());
	//					//				float scaler = ((document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin() - 10) / image
	//					//						.getWidth()) * 50;
	//					//				image.scalePercent(scaler);
	//					table.addCell(image);
	//				}
	//				subPara.add(table);
	//			}
	//			addEmptyLine(subPara, 1);
	//			pdfDocument.add(subPara);
	//		}
	//		catch (Exception e)
	//		{
	//			e.printStackTrace();
	//		}
	//	}
	//
	//	//	private static void addContent(Document document) throws DocumentException, IOException
	//	//	{
	//	//		Anchor anchor = new Anchor("First Chapter", FONT_TITLE);
	//	//		anchor.setName("First Chapter");
	//	//
	//	//		// Second parameter is the number of the chapter
	//	//		Chapter catPart = new Chapter(new Paragraph(anchor), 1);
	//	//
	//	//		Paragraph subPara = new Paragraph("Subcategory 1", FONT_SUB_TITLE);
	//	//		Section subCatPart = catPart.addSection(subPara);
	//	//		subCatPart.add(new Paragraph("Hello"));
	//	//
	//	//		subPara = new Paragraph("Subcategory 2", FONT_SUB_TITLE);
	//	//		subCatPart = catPart.addSection(subPara);
	//	//		subCatPart.add(new Paragraph("Paragraph 1"));
	//	//		subCatPart.add(new Paragraph("Paragraph 2"));
	//	//		subCatPart.add(new Paragraph("Paragraph 3"));
	//	//
	//	//		// Add a list
	//	//		createList(subCatPart);
	//	//		Paragraph paragraph = new Paragraph();
	//	//
	//	//		Image image1 = PngImage.getImage("/home/martin/workspace/JavaLib/data/ches-mapper.png");
	//	//		paragraph.add(image1);
	//	//
	//	//		addEmptyLine(paragraph, 5);
	//	//		subCatPart.add(paragraph);
	//	//
	//	//		// Add a table
	//	//		createTable(subCatPart);
	//	//
	//	//		// Now add all this to the document
	//	//		document.add(catPart);
	//	//
	//	//		// Next section
	//	//		anchor = new Anchor("Second Chapter", FONT_TITLE);
	//	//		anchor.setName("Second Chapter");
	//	//
	//	//		// Second parameter is the number of the chapter
	//	//		catPart = new Chapter(new Paragraph(anchor), 1);
	//	//
	//	//		subPara = new Paragraph("Subcategory", FONT_SUB_TITLE);
	//	//		subCatPart = catPart.addSection(subPara);
	//	//		subCatPart.add(new Paragraph("This is a very important message"));
	//	//
	//	//		// Now add all this to the document
	//	//		document.add(catPart);
	//	//
	//	//	}
	//
	//	private static PdfPTable table(ResultSet rs)
	//	{
	//		PdfPTable table = new PdfPTable(rs.getProperties().size());
	//
	//		for (String p : rs.getProperties())
	//		{
	//			PdfPCell c1 = new PdfPCell(new Phrase(p, FONT_TEXT_BOLD));
	//			c1.setHorizontalAlignment(Element.ALIGN_CENTER);
	//			table.addCell(c1);
	//		}
	//		table.setHeaderRows(1);
	//
	//		for (int i = 0; i < rs.getNumResults(); i++)
	//			for (String p : rs.getProperties())
	//			{
	//				Object val = rs.getResultValue(i, p);
	//				if (p.equals("runtime"))
	//					table.addCell(new Phrase(TimeFormatUtil.format(((Double) val).longValue()), FONT_TEXT));
	//				else if (val instanceof Double)
	//					table.addCell(new Phrase(StringUtil.formatDouble((Double) val, 3), FONT_TEXT));
	//				else
	//					table.addCell(new Phrase(val + "", FONT_TEXT));
	//
	//			}
	//		return table;
	//	}
	//
	//	//	private static void createTable(Section subCatPart) throws BadElementException
	//	//	{
	//	//		PdfPTable table = new PdfPTable(3);
	//	//
	//	//		// t.setBorderColor(BaseColor.GRAY);
	//	//		// t.setPadding(4);
	//	//		// t.setSpacing(4);
	//	//		// t.setBorderWidth(1);
	//	//
	//	//		PdfPCell c1 = new PdfPCell(new Phrase("Table Header 1"));
	//	//		c1.setHorizontalAlignment(Element.ALIGN_CENTER);
	//	//		table.addCell(c1);
	//	//
	//	//		c1 = new PdfPCell(new Phrase("Table Header 2"));
	//	//		c1.setHorizontalAlignment(Element.ALIGN_CENTER);
	//	//		table.addCell(c1);
	//	//
	//	//		c1 = new PdfPCell(new Phrase("Table Header 3"));
	//	//		c1.setHorizontalAlignment(Element.ALIGN_CENTER);
	//	//		table.addCell(c1);
	//	//		table.setHeaderRows(1);
	//	//
	//	//		table.addCell("1.0");
	//	//		table.addCell("1.1");
	//	//		table.addCell("1.2");
	//	//		table.addCell("2.1");
	//	//		table.addCell("2.2");
	//	//		table.addCell("2.3");
	//	//
	//	//		subCatPart.add(table);
	//	//
	//	//	}
	//	//
	//	//	private static void createList(Section subCatPart)
	//	//	{
	//	//		com.itextpdf.text.List list = new com.itextpdf.text.List(true, false, 10);
	//	//		list.add(new ListItem("First point"));
	//	//		list.add(new ListItem("Second point"));
	//	//		list.add(new ListItem("Third point"));
	//	//		subCatPart.add(list);
	//	//	}
	//
	//	private static void addEmptyLine(Paragraph paragraph, int number)
	//	{
	//		for (int i = 0; i < number; i++)
	//		{
	//			paragraph.add(new Paragraph(" "));
	//		}
	//	}
	//
	//	public void newPage()
	//	{
	//		pdfDocument.newPage();
	//
	//	}
	//
	//	@Override
	//	public void addImage(String image)
	//	{
	//		throw new IllegalStateException("Not yet implemented");
	//	}
	//
	//	@Override
	//	public void addParagraph(String text)
	//	{
	//		throw new IllegalStateException("Not yet implemented");
	//	}
	//
	//	@Override
	//	public void addSmallImages(String[] smallImages)
	//	{
	//		throw new IllegalStateException("Not yet implemented");
	//	}
	//
	//	@Override
	//	public void addTable(ResultSet table, String title)
	//	{
	//		throw new IllegalStateException("Not yet implemented");
	//	}
	//
	//	@Override
	//	public void addTable(ResultSet table, String title, boolean format)
	//	{
	//		throw new IllegalStateException("Not yet implemented");
	//	}
	//
	//	@Override
	//	public void addTable(ResultSet table)
	//	{
	//		throw new IllegalStateException("Not yet implemented");
	//	}
	//
	//	@Override
	//	public void newSection(String title)
	//	{
	//		throw new IllegalStateException("Not yet implemented");
	//	}
	//
	//	@Override
	//	public void newSubsection(String title)
	//	{
	//		throw new IllegalStateException("Not yet implemented");
	//	}
}
