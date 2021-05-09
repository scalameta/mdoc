package mdoc

import mdoc.internal.cli.Settings

final class OnLoadContext private[mdoc] (
    val reporter: Reporter,
    private[mdoc] val settings: Settings
) {
  def site: Map[String, String] = settings.site
}
