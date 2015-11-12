import org.apache.pdfbox.pdmodel.{PDPage, PDDocument}
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File

// The idea is to locate the TOC page numbers
case class InclusivePageIndexRange(first: Int, last: Int)

// Once we have locate the page numbers we will splice the TOC
// from the source (full PDF), replacing the TOC in the
// destination (preview PDF)
case class Splice(
  source: PDDocument, select:  InclusivePageIndexRange,
  dest:   PDDocument, replace: InclusivePageIndexRange
)

// Splicing will create a new document, not modify an existing one
object Editor {
  def apply(command: Splice): PDDocument = {
    import command._
    val doc = new PDDocument()
    val pages =
      (0 until replace.first).map(dest.getPage) ++
      (select.first until select.last).map(source.getPage) ++
      (replace.last until dest.getNumberOfPages).map(dest.getPage)
    pages.foreach(doc.importPage)
    doc
  }
}

// In our PDFs the TOC starts on page 3.
// The problem, then, is locating the end of the TOC.

// We do that by searching for a page that starts with the word "Preface".
// The preface is the first page after the TOC, so give us the end of the TOC.
object TOCFinder {

  // Does the given page in the given document start with the given text?
  def hasTitle(text: String, doc: PDDocument)(pageIndex: Int): Boolean = {
    val reader = new PDFTextStripper()
    reader.setStartPage(pageIndex)
    reader.setEndPage(pageIndex)
    val text = reader.getText(doc)
    text.trim.startsWith(text)
  }

  def apply(doc: PDDocument): Option[InclusivePageIndexRange] = {
    val tocStartIndex = 2 // page 3
    val preface = hasTitle("Preface", doc) _
    // When searching we start from the page after the first TOC page:
    Stream.from(tocStartIndex+1).find(preface).map { endIndex =>
      InclusivePageIndexRange(tocStartIndex, endIndex-1)
    }
  }
}

object Toctastic extends App {

  // Ensure we close the files
  def using[T](a: PDDocument, b: PDDocument)(f: (PDDocument, PDDocument) => T): T =
    try f(a,b) finally {
      a.close
      b.close
    }

  def open(filename: String) = PDDocument.load(new File(filename))

  args match {
    case Array(fullFilename, previewFilename, outputFilename) =>
      using( open(fullFilename), open(previewFilename) ) { (full, preview) =>

        val command: Option[Splice] = for {
          fullToc    <- TOCFinder(full)
          previewToc <- TOCFinder(preview)
        } yield Splice(full, fullToc, preview, previewToc)

        command match {
          case None => println("Unable to find TOCs")
          case _    => command.
            map(Editor.apply).
            foreach(doc => {
              doc.save(outputFilename)
              doc.close
              }
            )
        }
      }
    case _ => println("Usage: full.psd preview.pdf out.pdf")
  }
}