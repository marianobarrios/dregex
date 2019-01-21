package dregex.impl

object JavaCharacterProperties {

  val properties: Map[String, Int => Boolean] = {
    val builder = collection.mutable.Map[String, Int => Boolean]()
    for (method <- classOf[Character].getMethods) {
      val name = method.getName
      if (name.startsWith("is")) {
        val paramType = method.getParameters.head.getType
        if (paramType == classOf[Int]) {
          val property = name.substring("is".length)
          def evaluationFn(codePoint: Int): Boolean = {
            method.invoke(null, codePoint.asInstanceOf[Object]).asInstanceOf[Boolean]
          }
          builder.put(property, evaluationFn)
        }
      }
    }
    builder.toMap
  }

}
