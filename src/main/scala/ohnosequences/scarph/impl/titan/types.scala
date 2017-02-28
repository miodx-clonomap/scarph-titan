package ohnosequences.scarph.impl.titan

case object types {

  import com.tinkerpop.blueprints
  import com.thinkaurelius.titan.{ core => titan }
  import scala.collection.JavaConverters.{ asJavaIterableConverter, iterableAsScalaIterableConverter }
  import ohnosequences.scarph._, impl._

  sealed trait AnyTitanType

  case object TitanZero extends AnyTitanType
  type TitanZero = TitanZero.type

  case class TitanUnit(val graph: titan.TitanGraph) extends AnyTitanType


  trait AnyDuplet extends AnyTitanType {

    type Left <: AnyTitanType
    val  left: Left

    type Right <: AnyTitanType
    val  right: Right
  }

  case class Duplet[L <: AnyTitanType, R <: AnyTitanType](val left: L, val right: R) extends AnyDuplet {

    type Left = L
    type Right = R
  }

  trait AnyContainer extends AnyTitanType {

    type Inside
    type Values = Iterable[Inside]
    val  values: Values

    def map[T](f: Inside => T): Container[T] = Container(values.map(f): Iterable[T])
    def flatMap[T](f: Inside => Iterable[T]): Container[T] = Container(values.flatMap(f): Iterable[T])
    def filter(f: Inside => Boolean): Container[Inside] = Container(values.filter(f))
  }

  case class Container[T](val values: Iterable[T]) extends AnyContainer { type Inside = T }


  final type TitanVertices = Container[titan.TitanVertex]
  final type TitanEdges    = Container[titan.TitanEdge]
  final type TitanQueries  = Container[blueprints.Query]

  final type JIterable[T]  = java.lang.Iterable[T]



  /* Conversions */

  implicit final def blueprintsVerticesOps(es: JIterable[blueprints.Vertex]):
    BlueprintsVerticesOps =
    BlueprintsVerticesOps(es)
  case class BlueprintsVerticesOps(val es: JIterable[blueprints.Vertex]) extends AnyVal {

    final def asTitanVertices: Iterable[titan.TitanVertex] =
      es.asScala map { x => x.asInstanceOf[titan.TitanVertex] }
  }

  implicit final def blueprintsEdgesOps(es: JIterable[blueprints.Edge]):
    BlueprintsEdgesOps =
    BlueprintsEdgesOps(es)
  case class BlueprintsEdgesOps(val es: JIterable[blueprints.Edge]) extends AnyVal {

    final def asTitanEdges: Iterable[titan.TitanEdge] =
      es.asScala map { x => x.asInstanceOf[titan.TitanEdge] }
  }

}
