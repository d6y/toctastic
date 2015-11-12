import com.itextpdf.text.Document
import com.itextpdf.text.pdf.{ PdfCopy, PdfReader}
import com.itextpdf.text.pdf.parser.{ PdfReaderContentParser, SimpleTextExtractionStrategy}
import java.io.FileOutputStream

// The idea is to locate the TOC page numbers
case class InclusivePageRange(first: Int, last: Int)

// Once we the page numbers we will splice the TOC from the source (full PDF),
// replacing the TOC in the destination (preview PDF)
case class Splice(
  source: PdfReader, select:  InclusivePageRange,
  dest:   PdfReader, replace: InclusivePageRange
)

// Splicing will create a new document, not modify an existing one
object Editor {
  def apply(command: Splice, outputFilename: String): Unit  = {
    val doc = new Document()
    val copier = new PdfCopy(doc, new FileOutputStream(outputFilename))
    doc.open
    def from(pdf: PdfReader)(page: Int): Unit = copier.addPage(copier.getImportedPage(pdf, page))
    import command._
    (1 until replace.first).foreach(from(dest))
    (select.first to select.last).foreach(from(source))
    (replace.last to dest.getNumberOfPages).foreach(from(dest))
    doc.close
  }
}

// In our PDFs the TOC starts on page 3.
// The problem, then, is locating the end of the TOC.

// We do that by searching for a page that starts with the word "Preface".
// The preface is the first page after the TOC, so give us the end of the TOC.
object TOCFinder {

  // Does the given page in the given document start with the given text?
  def hasTitle(text: String, doc: PdfReader)(pageIndex: Int): Boolean = {
    val parser = new PdfReaderContentParser(doc)
    val content = parser.processContent(pageIndex, new SimpleTextExtractionStrategy()).getResultantText()
    content.trim.startsWith(text)
  }

  def apply(doc: PdfReader): Option[InclusivePageRange] = {
    val tocStartPage = 3
    val preface = hasTitle("Preface", doc) _
    Stream.from(tocStartPage+1).find(preface).map { endPage =>
      InclusivePageRange(tocStartPage, endPage-1)
    }
  }
}

object Toctastic extends App {

  args match {
    case Array(fullFilename, previewFilename, outputFilename) =>

      val full = new PdfReader(fullFilename)
      val preview = new PdfReader(previewFilename)

      val command: Option[Splice] = for {
        fullToc    <- TOCFinder(full)
        previewToc <- TOCFinder(preview)
      } yield Splice(full, fullToc, preview, previewToc)

      // println(command)

      command.fold(println("Unable to find TOCs"))(Editor(_, outputFilename))

    case _ => println("Usage: full.psd preview.pdf out.pdf")
  }
}