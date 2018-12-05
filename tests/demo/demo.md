# Title

Paragraph. Paragraph.

```scala mdoc
case class User(name: String, age: Int)
val users = List(
    User("John", 18),
    User("Susan", 42)
)
users.foreach { user =>
  println("${user.name}: ${user.age}")
}
```
