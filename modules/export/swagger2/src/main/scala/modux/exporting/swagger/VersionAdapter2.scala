package modux.exporting.swagger

import io.swagger.models.parameters.{Parameter, PathParameter, QueryParameter}
import io.swagger.models.properties._
import modux.model.schema._

import java.util

object VersionAdapter2 {

  def apply(xs: Seq[MParameter]): Seq[Parameter] = xs.map { x =>
    x.in match {
      case "path" =>
        new PathParameter()
          .name(x.name)
          .required(x.required)
          .property(x.schema.map(VersionAdapter2(_)).orNull)
      case "query" =>
        new QueryParameter()
          .name(x.name)
          .required(x.required)
          .property(x.schema.map(VersionAdapter2(_)).orNull)
      case _ => throw new RuntimeException(s"Unsupported parameter type ${x.in}")
    }
  }

  def xs(xs: Seq[MSchema]): Seq[Property] = xs.map(x => VersionAdapter2(x))

  def apply(mschema: MSchema): Property = {

    mschema match {
      case x: MRefSchema => new RefProperty(x.ref).allowEmptyValue(x.isNullable)
      case x: MPrimitiveSchema =>

        val s: Property = {
          x.kind match {
            case "string" =>
              val p: StringProperty = new StringProperty()
              p.setFormat(x.format.orNull)
              p.setExample(x.example.orNull)
              p
            case "integer" =>
              val p: IntegerProperty = new IntegerProperty()
              p.setFormat(x.format.orNull)
              p.setExample(x.example.orNull: AnyRef)

              p
            case "byte" =>
              val p: BinaryProperty = new BinaryProperty()
              p.setFormat(x.format.orNull)
              p.setExample(x.example.orNull)

              p
            case "number" =>
              val p: DecimalProperty = new DecimalProperty()
              p.setFormat(x.format.orNull)
              p.setExample(x.example.orNull: AnyRef)

              p
            case "boolean" =>
              val p: BooleanProperty = new BooleanProperty()
              p.setFormat(x.format.orNull)
              p.setExample(x.example.orNull: AnyRef)
              p
            case "object" =>
              val p: ObjectProperty = x.properties.foldLeft(new ObjectProperty()) { case (acc, (k, v)) => acc.property(k, VersionAdapter2(v)) }
              p.setFormat(x.format.orNull)
              p.setExample(x.example.orNull: AnyRef)
              p
            case _ => throw new RuntimeException("Unsupported schema")
          }
        }

        s.setRequired(!x.isNullable )

        s

      case x: MArraySchema =>
        new ArrayProperty().items(VersionAdapter2(x.item))
      case x: MComposed =>
        import scala.jdk.CollectionConverters._
        val java: util.List[Property] = x.items.map(y => VersionAdapter2(y)).asJava
        new ComposedProperty().allOf(java)
    }
  }
}
