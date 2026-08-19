package dotty.tools.io

import dotty.tools.io.VirtualDirectory

object VirtualDirectoryCompat {
  def virtualDirectory(name: String): VirtualDirectory = new VirtualDirectory(name)
}
