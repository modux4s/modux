package modux.exporting.swagger

import io.swagger.v3.oas.models.media._
import io.swagger.v3.oas.models.parameters.{Parameter, PathParameter, QueryParameter}
import modux.model.schema._

object VersionAdapter3 {

  def apply(xs: Seq[MParameter]): Seq[Parameter] = xs.map { x =>
    x.in match {
      case "path" =>
        new PathParameter()
          .name(x.name)
          .required(x.required)
          .schema(x.schema.map(VersionAdapter3(_)).orNull)
      case "query" =>
        new QueryParameter()
          .name(x.name)
          .required(x.required)
          .schema(x.schema.map(VersionAdapter3(_)).orNull)
      case _ => throw new RuntimeException(s"Unsupported parameter type ${x.in}")
    }
  }

  def xs(xs: Seq[MSchema]): Seq[Schema[_]] = xs.map(x => VersionAdapter3(x))

  def apply(mschema: MSchema): Schema[_] = {

    mschema match {
      case x: MRefSchema => new ObjectSchema().$ref(x.ref).nullable(x.isNullable)
      case x: MPrimitiveSchema =>

        val s: Schema[_] = {
          x.kind match {
            case "string" => new StringSchema()
            case "integer" => new IntegerSchema()
            case "byte" => new BinarySchema()
            case "number" => new NumberSchema()
            case "boolean" => new BooleanSchema()
            case "object" => new ObjectSchema()
            case _ => throw new RuntimeException("Unsupported schema")
          }
        }

        x
          .properties
          .foldLeft(s) { case (acc, (k, v)) => acc.addProperties(k, VersionAdapter3(v)); acc }
          .nullable(x.isNullable)
          .format(x.format.orNull)
          .example(x.example.orNull)

      case x: MArraySchema => new ArraySchema().items(VersionAdapter3(x.item))
      case x: MComposed =>
        x.items.foldLeft(new ComposedSchema) { case (comp, x) => comp.addOneOfItem(VersionAdapter3(x)) }
    }
  }
}
