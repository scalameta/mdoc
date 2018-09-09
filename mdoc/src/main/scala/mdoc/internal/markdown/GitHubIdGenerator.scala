package mdoc.internal.markdown
import com.vladsch.flexmark.html.renderer.HeaderIdGenerator

object GitHubIdGenerator extends (String => String) {
  private def dashChars: String = " -_"
  def apply(header: String): String = {
    HeaderIdGenerator.generateId(header, dashChars, false)
  }
}
