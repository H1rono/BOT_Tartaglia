package example

object Hello extends App {
  import org.json4s.native.JsonMethods.parse

  val flakeLock = io.Source.fromFile("flake.lock").mkString
  val parsed = parse(flakeLock)
  println(parsed)
}
