package littlescalaswagger

import scala.reflect.ClassTag
import cats.syntax.option._
import io.circe.{ Encoder }
import io.circe.generic.semiauto.deriveEncoder
import io.circe.generic.auto._
import enum.Enum

object Swagger {

  val DEFINITIONS = "#/definitions/"

  case class Root(
    swagger: String,
    info: Info,
    basePath: String,
    tags: List[String],
    schemes: List[String],
    paths: Paths,
    definitions: Map[String, Definition] = Map.empty
  ) {

    def definition(ds: DefinitionWithName*): Root =
      copy(definitions = ds.map(d => (d.name, d.definition)).toMap)

    def validate: Root = {

      val methods = paths.values
        .flatMap(p => (p.get ++ p.post ++ p.delete ++ p.put))

      val schemasFromParameters =
        methods
          .flatMap(_.parameters)
          .map(p => p.schema orElse p.items)
          .collect { case Some(s) => s }
          .flatMap(p => p :: p.oneOf.getOrElse(Nil) ++ p.items)

      val schemasFromResponses = methods
        .flatMap(_.responses.values)
        .map(_.schema)
        .collect { case Some(s) => s }
        .flatMap(p => p :: p.oneOf.getOrElse(Nil))

      val schemasFromDefinitions = definitions.values
        .flatMap(_.properties.values) ++
        definitions.values
          .flatMap(_.properties.values)
          .flatMap(p => p.items.toList ++ p.oneOf.getOrElse(Nil))

      val allSchemas = (schemasFromParameters ++ schemasFromResponses ++ schemasFromDefinitions)
        .map(p => p.`$ref` orElse p.items.flatMap(_.`$ref`))
        .collect { case Some(s) => s }
        .map(_.substring(DEFINITIONS.length))
        .toSet

      val allDefinitions = definitions.keys.toSet

      val missingDefinitions = allSchemas.diff(allDefinitions)
      if (!missingDefinitions.isEmpty)
        throw new Exception(s"Missing definitions: ${missingDefinitions.mkString(",")}")

      val unusedDefinition = allDefinitions.diff(allSchemas)
      if (!unusedDefinition.isEmpty)
        throw new Exception(s"Unused definitions: ${unusedDefinition.mkString(",")}")

      this
    }

  }

  object Root {
    // this is the root Encoder which is cached all others are derived when needed
    implicit val encoder: Encoder[Root] = deriveEncoder
  }

  case class Info(description: String, version: String, title: String, termsOfService: String)

  type Paths = Map[String, Path]

  case class Path(
    get: Option[Method] = None,
    post: Option[Method] = None,
    put: Option[Method] = None,
    delete: Option[Method] = None
  ) {
    def tag(tag: String): Path = Path(
      get = get.map(_.copy(tags = List(tag).some)),
      post = post.map(_.copy(tags = List(tag).some)),
      put = put.map(_.copy(tags = List(tag).some)),
      delete = delete.map(_.copy(tags = List(tag).some))
    )
  }

  case class Method(
    summary: String,
    tags: Option[List[String]] = None,
    description: String,
    produces: List[String] = Nil,
    parameters: List[Parameter] = Nil,
    responses: Map[Int, Response] = Map.empty
  ) {

    def parameter(p: Parameter): Method = copy(parameters = p :: parameters)

    def response(code: Int, r: Response): Method = copy(responses = responses.updated(code, r))
  }

  sealed trait Location

  object Location {
    object query extends Location
    object body extends Location
    object header extends Location
    object path extends Location
    object cookie extends Location
  }

  implicit val locationEnum: Enum[Location] = Enum.derived[Location]
  implicit val locationEncoder: Encoder[Location] = CirceEnums.enumEncoder

  case class Parameter(
    name: String,
    in: Location,
    description: String,
    required: Boolean,
    `type`: Option[String],
    schema: Option[Property],
    items: Option[Property] = None
  )

  case class DefinitionWithName(name: String, definition: Definition) {

    def desc(d: String): DefinitionWithName =
      copy(definition = definition.copy(description = d.some))

    def desc(property: String, d: String): DefinitionWithName = {
      val newProperty = definition.properties(property).copy(description = d.some)
      val newProperties = definition.properties.updated(property, newProperty)
      copy(definition = definition.copy(properties = newProperties))
    }
  }

  case class Definition(
    `type`: String,
    required: List[String],
    properties: Map[String, Property],
    enum: List[String],
    description: Option[String] = None
  )

  case class Property(
    `type`: Option[String] = None,
    format: Option[String] = None,
    example: Option[String] = None,
    `$ref`: Option[String] = None,
    oneOf: Option[List[Property]] = None,
    items: Option[Property] = None,
    description: Option[String] = None
  )

  case class Response(description: String, schema: Option[Property])

  // helpers

  def simpleName[A](implicit t: ClassTag[A]): String = t.runtimeClass.getSimpleName

  def ref[A: ClassTag] = Property(`$ref` = s"$DEFINITIONS${simpleName[A]}".some)

  def fieldToProperty(f: Field): Property =
    if (f.isList)
      Property(`type` = "array".some, items = fieldToProperty(f.copy(isList = false)).some)
    else {
      val typeName = f.clazz.getSimpleName
      typeName match {
        case "int" => Property(`type` = "integer".some, format = "int32".some)
        case "long" => Property(`type` = "integer".some, format = "int64".some)
        case "String" => Property(`type` = "string".some)
        case "Boolean" => Property(`type` = "boolean".some)
        case "boolean" => Property(`type` = "boolean".some)
        case "OffsetDateTime" => Property(`type` = "string".some, format = "date".some, example = "2018-01-01".some)
        case "CountryCode" =>
          Property(`type` = "string".some, format = "ISO 3166-1 alpha-2 country code".some, example = "US".some)
        case _ => Property(`$ref` = s"$DEFINITIONS$typeName".some)
      }
    }

  def caseClass[A: ToFields: ClassTag]: DefinitionWithName =
    DefinitionWithName(
      simpleName[A],
      Definition(
        `type` = "object",
        required = ToFields[A].fields.filterNot(_.isOption).map(_.name),
        properties = ToFields[A].fields.map(f => (f.name, fieldToProperty(f))).toMap,
        enum = Nil
      )
    )

  def enum[A: Enum: ClassTag]: DefinitionWithName =
    DefinitionWithName(
      simpleName[A],
      Definition(`type` = "string", required = Nil, properties = Map(), enum = Enum[A].labels.toList)
    )
}
