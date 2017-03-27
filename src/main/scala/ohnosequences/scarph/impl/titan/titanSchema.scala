package ohnosequences.scarph.impl.titan

case object titanSchema {

  import ohnosequences.scarph._
  import com.thinkaurelius.titan.{ core => titan }
  import titan.{ TitanTransaction, TitanGraphTransaction, TitanGraph }
  import titan.{ Multiplicity, Cardinality }
  import titan.schema.{ SchemaManager, TitanManagement, TitanGraphIndex }

  import scala.reflect._
  import scala.util._

  val primitivesToBoxed = Map[Class[_], Class[_ <: AnyRef]](
    classOf[Int]      -> classOf[java.lang.Integer],
    classOf[Long]     -> classOf[java.lang.Long],
    classOf[Boolean]  -> classOf[java.lang.Boolean],
    classOf[Float]    -> classOf[java.lang.Float],
    classOf[Double]   -> classOf[java.lang.Double],
    classOf[Char]     -> classOf[java.lang.Character],
    classOf[Byte]     -> classOf[java.lang.Byte],
    classOf[Short]    -> classOf[java.lang.Short]
  )

  final def edgeTitanMultiplicity(a: AnyEdge): Multiplicity = a.sourceArity match {

    case oneOrNone(_) | exactlyOne(_)   => a.targetArity match {

      case oneOrNone(_)  | exactlyOne(_)  => Multiplicity.ONE2ONE
      case atLeastOne(_) | manyOrNone(_)  => Multiplicity.ONE2MANY
    }

    case atLeastOne(_) | manyOrNone(_)  => a.targetArity match {

      case oneOrNone(_)  | exactlyOne(_)  => Multiplicity.MANY2ONE
      case atLeastOne(_) | manyOrNone(_)  => Multiplicity.MULTI
    }
  }

  type MGMT = titan.schema.TitanManagement

  // TODO this should be improved
  /* These methods work in a context of a previously created schema manager transaction (see below) */
  implicit final class SchemaManagerSchemaOps(val schemaManager: MGMT) extends AnyVal {

    final def addPropertyKey(p: AnyProperty): titan.PropertyKey = {

      val clzzFromTag =
        p.targetArity.graphObject.valueTag.runtimeClass

      val clzz: Class[_] =
        primitivesToBoxed.get(clzzFromTag) getOrElse clzzFromTag

      println(s"  Creating [${p.label}] property key (${clzz})")

      val propertyKey = Option(schemaManager.getPropertyKey(p.label)) getOrElse
        schemaManager.makePropertyKey(p.label)
        .cardinality( Cardinality.SINGLE )
        .dataType(clzz)
        .make()

      // why here? because https://github.com/thinkaurelius/titan/issues/793#issuecomment-60698050
      val index =
        schemaManager.createOrGetIndexFor(p)

      propertyKey
    }

    final def addEdgeLabel(e: AnyEdge): titan.EdgeLabel = {
      println(s"  Creating [${e.label}] edge label")

      Option(schemaManager.getEdgeLabel(e.label)) getOrElse
        schemaManager.makeEdgeLabel(e.label)
          .directed()
          .multiplicity(edgeTitanMultiplicity(e))
          .make()
    }

    final def addVertexLabel(v: AnyVertex): titan.VertexLabel = {
      println(s"  Creating [${v.label}] vertex label")

      Option(schemaManager.getVertexLabel(v.label)) getOrElse
        schemaManager.makeVertexLabel(v.label)
          .make()
    }

    // TODO: could return something more useful, for example pairs (scarph type, titan key)
    final def createSchema(schema: AnyGraphSchema): Unit = {
      println(s"  Creating schema types for ${schema.label}")
      println(s"  Creating vertices")
      val vertexLabels = schema.vertices.toSeq.map(schemaManager.addVertexLabel)
      println(s"  Creating edges")
      val edgeLabels   = schema.edges.toSeq.map(schemaManager.addEdgeLabel)
      println(s"  Creating properties")
      val props = schema.properties.toSeq; println(props)
      val propertyKeys = props.map(schemaManager.addPropertyKey)
      println(s"  Finished creating schema types for ${schema.label}")
    }
  }

  /* This is similar to SchemaManagerOps, but can create indexes */
  implicit final class TitanManagementOps(val manager: MGMT) extends AnyVal {

    def createOrGetIndexFor(property: AnyProperty): TitanGraphIndex = {

      val ownerClass: Class[_ <: org.apache.tinkerpop.gremlin.structure.Element] =
        property.source.elementType match {
          case VertexElement => classOf[titan.TitanVertex]
          case EdgeElement   => classOf[titan.TitanEdge]
        }

      val propertyKey: titan.PropertyKey =
        manager getPropertyKey property.label

      val indexName: String =
        s"${property.label}.index"

      val genericConf =
        manager
          .buildIndex(indexName, ownerClass)
          .addKey(propertyKey)
          .indexOnly(
            property.source.elementType match {
              case VertexElement => manager.getVertexLabel(property.source.label)
              case EdgeElement   => manager.getEdgeLabel(property.source.label)
            }
          )

      val indexBuilder =
        property.sourceArity match {
          case oneOrNone(_) | exactlyOne(_) => genericConf.unique()
          case _                            => genericConf
        }

      Option(manager.getGraphIndex(indexName)) getOrElse {

        println { s"  Creating index ${indexName} for ${propertyKey}" }
        indexBuilder.buildCompositeIndex()
      }
    }
  }

  /* This opens a new schema manager instance, create the schema and commits */
  implicit final class TitanGraphOps(val graph: TitanGraph) extends AnyVal {

    /* This is useful for wrapping writing operations */
    def withTransaction[X](fn: TitanGraphTransaction => X): Try[X] = {
      val tx = graph.newTransaction()

      val result = Try {
        val x = fn(tx)
        tx.commit()
        x
      }

      result match {
        case Failure(err) => println(s"${err}"); tx.rollback(); result
        case Success(_) => result
      }
    }

    /* Same as withTransaction, but uses TitanManagement (they don't have a common super-interface) */
    def withManager[T](fn: MGMT => T): Try[T] = {
      val tx = graph.openManagement()

      val result = Try {
        val x = fn(tx)
        tx.commit()
        x
      }

      result match {
        case Failure(_) => tx.rollback(); result
        case Success(_) => result
      }
    }

    def createSchema(schema: AnyGraphSchema): Try[Unit] =
      withManager { _ createSchema schema }

    def createIndex(property: AnyProperty): Try[TitanGraphIndex] =
      withManager { _ createOrGetIndexFor property }
  }
}
