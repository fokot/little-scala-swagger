package littlescalaswagger

import shapeless.{ ::, HList, HNil, LabelledGeneric, Lazy, Witness }
import shapeless.labelled.FieldType

import scala.reflect.ClassTag

case class Field(name: String, clazz: Class[_], isOption: Boolean = false, isList: Boolean = false)

trait ToField[A] {
  def field: Field
}

trait ToFields[A] {
  def fields: List[Field]
}

trait LowToField {

  implicit def fieldObject[K <: Symbol, A: ClassTag](implicit key: Witness.Aux[K]) =
    ToField.instance[FieldType[K, A]](Field(key.value.name, implicitly[ClassTag[A]].runtimeClass))
}

object ToField extends LowToField {

  def apply[A: ToField] = implicitly[ToField[A]]

  def instance[A](f: Field) = new ToField[A] {
    override def field = f
  }

  implicit def fieldsOption[K, A](implicit fields: Lazy[ToField[FieldType[K, A]]]) =
    instance[FieldType[K, Option[A]]](fields.value.field.copy(isOption = true))

  implicit def fieldsList[K, A](implicit fields: Lazy[ToField[FieldType[K, A]]]) =
    instance[FieldType[K, List[A]]](fields.value.field.copy(isList = true))

}

/**
  *
  * ToFields[TaskDTO].fields
  *
  * Will give you fields of the case class also with their names
  *
  */
object ToFields {

  def apply[A: ToFields] = implicitly[ToFields[A]]

  def instance[A](fs: List[Field]) = new ToFields[A] {
    override def fields = fs
  }

  implicit val fieldsHNil = instance[HNil](Nil)

  implicit def fieldsHCons[A: ToField, H <: HList: ToFields] = instance[A :: H](ToField[A].field :: ToFields[H].fields)

  implicit def fieldsCaseClass[A, H <: HList](implicit gen: LabelledGeneric.Aux[A, H], h: ToFields[H]) =
    instance[A](h.fields)
}
