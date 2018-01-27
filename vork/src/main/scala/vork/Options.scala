package vork

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import vork.utils.IO
import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.Configured
import metaconfig.annotation._
import metaconfig.generic
import metaconfig.generic.Surface
import metaconfig.typesafeconfig.typesafeConfigMetaconfigParser

case class Options(
    @Description("The input directory to generate the vork site.")
    @ExtraName("i")
    in: Path = Paths.get("docs"),
    @Description("The output directory to generate the vork site.")
    @ExtraName("o")
    out: Path = Paths.get("target").resolve("vork"),
    @Description("The current working directory")
    cwd: Path = Paths.get(sys.props("user.dir")),
    cleanTarget: Boolean = false,
    encoding: Charset = StandardCharsets.UTF_8,
    configPath: Path = Paths.get("vork.conf"),
    @Description("Optional classpath to compile Scala code examples")
    classpath: String = "",
    @ExtraName("w")
    watch: Boolean = false,
    config: Config = Config()
) {

  def isAbsolute: Boolean =
    cwd.isAbsolute &&
      configPath.isAbsolute &&
      in.isAbsolute &&
      out.isAbsolute

  def resolveIn(relpath: Path): Path = {
    require(!relpath.isAbsolute)
    in.resolve(relpath)
  }

  def resolveOut(relpath: Path): Path = {
    require(!relpath.isAbsolute)
    out.resolve(relpath)
  }
}

object Options {
  def fromCliArgs(args: List[String]): Configured[Options] = {
    Conf
      .parseCliArgs[Options](args)
      .andThen(_.as[Options])
      .andThen(fromDefault)
  }
  def fromDefault(default: Options): Configured[Options] = {
    val absoluteOptions: Options = {
      import default._
      copy(
        in = IO.absolutize(in, cwd),
        out = IO.absolutize(out, cwd),
        configPath = IO.absolutize(configPath, cwd)
      )
    }

    val parsedConfig: Configured[Config] =
      if (Files.exists(absoluteOptions.configPath)) {
        Conf.parseFile(absoluteOptions.configPath.toFile).andThen(_.as[Config])
      } else {
        Configured.ok(absoluteOptions.config)
      }
    parsedConfig.map(newConfig => absoluteOptions.copy(config = newConfig))
  }
  import vork.utils.Decoders._ // WARNING: IntelliJ will remove this required import
  implicit val surface: Surface[Options] =
    generic.deriveSurface[Options]
  implicit val decoder: ConfDecoder[Options] =
    generic.deriveDecoder[Options](Options())
}
