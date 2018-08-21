# Little scala swagger

Little swagger implementation in scala as I was not satisfied with `swagger-core` library.

Write your routes by hand and derive your case classes and enums with methods `caseClass` or `enum` respectively.
For enum you need to have `Enum` instance in scope:
```scala
  import enum.Enum

  sealed trait Salutation

  object Salutation {
    case object Mr extends Salutation
    case object Miss extends Salutation
    case object Ms extends Salutation
    case object Mrs extends Salutation

    implicit val enum: Enum[Salutation] = Enum.derived[Salutation]
  }
```

After constructing your API definition you can call `validate` which will in runtime check if all defined types are 
present and if there are no obsolete types (e.g. one not used in route directly or from any other type).

## Example

```scala

  def swagger =
    Root(
      swagger = "2.0",
      info = Info(description = "", version = "1.0", title = "", termsOfService = ""),
      basePath = "/api/v1",
      tags = Nil,
      schemes = "https" :: "http" :: Nil,
      paths = Map(
        "/customer" -> Path(post = customerCreate_doc.some).tag("customer"),
        "/customer/{customerId}" -> Path(
          get = customerGet_doc.some,
          delete = customerDelete_doc.some,
          put = customerUpdate_doc.some
        ).tag("customer"),
        "/customer/ext/{externalId}" -> Path(
          get = customerGetExt_doc.some,
          put = customerUpdateExt_doc.some,
          delete = customerDeleteExt_doc.some
        ).tag("customer/ext"),
        "/task/{taskId}" -> Path(get = getTask_doc.some).tag("task")
      )
    ).definition(
        caseClass[IndividualEntity]
          .desc("externalID", "id in external system")
          .desc("dateCreated", "In request ignored, used in response"),
        caseClass[LegalEntity]
          .desc("dBNumber", "Dun & Bradstreet DUNS number")
          .desc("externalID", "id in external system")
          .desc("dateCreated", "In request ignored, used in response"),
        caseClass[LegalEntityNameVariation],
        caseClass[CustomerCreatedResponse],
        enum[IDTypeEnum],
        enum[NameTypeEnum],
        enum[EntityTypeEnum],
        enum[Salutation],
        enum[GenderEnum],
      )
      .validate
      
      val swaggerJson = swagger.asJson.spaces2DropNulls
```


## License
Copyright (c) 2013 Stephen Mathieson
Licensed under the WTFPL license.