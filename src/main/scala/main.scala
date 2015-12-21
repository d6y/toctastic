import com.itextpdf.text.Document
import com.itextpdf.text.pdf.{PdfCopy, PdfReader}
import com.itextpdf.text.pdf.parser.{PdfReaderContentParser, SimpleTextExtractionStrategy}
import java.io.{File, FileOutputStream}
import java.util.regex.Pattern

// The idea is to locate the TOC page numbers
case class InclusivePageRange(first: Int, last: Int)

// Once we have the page numbers we will splice the TOC from the source (full PDF),
// replacing the TOC in the destination (preview PDF).
// That is we select certain page from the source, and
// in the destination we replace certain pages with the selecte source pages.
case class Splice(
  source: PdfReader, select:  InclusivePageRange,
  dest:   PdfReader, replace: InclusivePageRange
)

// I say "replace", but Splicing will create a new document, not modify an existing one
object Editor {
  def apply(command: Splice, outputFilename: String): Unit  = {
    val doc = new Document()
    val copier = new PdfCopy(doc, new FileOutputStream(outputFilename))
    doc.open
    def from(pdf: PdfReader)(page: Int): Unit = copier.addPage(copier.getImportedPage(pdf, page))
    import command._
    (1 until replace.first).foreach(from(dest))
    (select.first to select.last).foreach(from(source))
    (replace.last+1 to dest.getNumberOfPages).foreach(from(dest))
    doc.close
  }
}

// In our PDFs the TOC starts on page 3.
// The problem, then, is locating the end of the TOC.

// We do that by searching for a page that starts with the word "Preface" or "Foreword".
// The preface is the first page after the TOC, so give us the end of the TOC.
// (In Essential Play there's no preface or foreward, so we look for Introduction)
object TOCFinder {

  val regex = Pattern.compile("^(Foreword|Preface|Introdu).*", Pattern.DOTALL)

  // Does the given page in the given document start with the given regex?
  def hasTitle(pattern: Pattern, doc: PdfReader)(pageIndex: Int): Boolean = {
    val parser = new PdfReaderContentParser(doc)
    // Null pointer here? We've probably run out pages looking for the pattern
    val content = parser.processContent(pageIndex, new SimpleTextExtractionStrategy()).getResultantText()
    //println(s"\n\n$pageIndex: ${content.trim}")
    pattern.matcher(content.trim).matches()
  }

  def apply(doc: PdfReader): Option[InclusivePageRange] = {
    val tocStartPage = 3
    val justAfterToc = hasTitle(regex, doc) _
    Stream.from(tocStartPage+1).find(justAfterToc).map { endPage =>
      InclusivePageRange(tocStartPage, endPage-1)
    }
  }
}

object Toctastic extends App {

  args match {
    case Array(fullFilename, previewFilename, outputFilename) =>

      assert(new File(fullFilename).exists,    s"Cannot find $fullFilename")
      assert(new File(previewFilename).exists, s"Cannot find $previewFilename")

      val full = new PdfReader(fullFilename)
      val preview = new PdfReader(previewFilename)

      val command: Option[Splice] = for {
        fullToc    <- TOCFinder(full)
        previewToc <- TOCFinder(preview)
      } yield Splice(full, fullToc, preview, previewToc)

      println(command)

      command.fold(println("Unable to find TOCs"))(Editor(_, outputFilename))

    case _ => println("Usage: full.pdf preview.pdf out.pdf")
  }
}
