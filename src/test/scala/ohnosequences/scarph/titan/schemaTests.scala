package ohnosequences.scarph.impl.titan.test

import com.thinkaurelius.titan.core


class TitanSuite extends org.scalatest.FunSuite with org.scalatest.BeforeAndAfterAll {

  import ohnosequences.{ scarph => s }
  import s.objects._, s.evals._, s.morphisms._
  import s.syntax.objects._, s.syntax.morphisms._
  import s.test.twitter._, s.test.queries, s.test.asserts._

  import ohnosequences.scarph.impl.{ titan => t }
  import t.evals._, t.types._, t.rewrites._, t.syntax._

  import java.io.File

  // val graphLocation = new File("/tmp/titanTest")
  // var twitterGraph: titan.TitanGraph = null
  val twitterGraph = core.TitanFactory.open("inmemory")

  override final def beforeAll() {

    /*
    def cleanDir(f: File) {
      if (f.isDirectory) f.listFiles.foreach(cleanDir(_))
      else { println(f.toString); f.delete }
    }
    cleanDir(graphLocation)
    */

    // twitterGraph = TitanFactory.open("berkeleyje:" + graphLocation.getAbsolutePath)

    println("Created Titan graph")

    import ohnosequences.scarph.impl.titan.titanSchema._
    import ohnosequences.scarph.test._

    // twitterGraph.createSchema(twitter)

    import com.tinkerpop.blueprints.util.io.graphson._
    GraphSONReader.inputGraph(twitterGraph, this.getClass.getResource("/twitter_graph.json").getPath)

    println("Loaded sample Twitter data")
  }

  override final def afterAll() {
    // NOTE: uncommend if you want to add data to the GraphSON:
    // import com.tinkerpop.blueprints.util.io.graphson._
    // GraphSONWriter.outputGraph(twitterGraph, "graph_compact.json", GraphSONMode.COMPACT)

    twitterGraph.shutdown
    println("Shutdown Titan graph")
  }

  case object testSamples {
    import ohnosequences.cosas.types._

    val nousers = user := Container[core.TitanVertex](Iterable())

    def vertices[V <: AnyVertex](v: V): V := TitanVertices =
      v := Container(twitterGraph.query.has("type", v.label).vertices.asTitanVertices)

    def edges[E <: AnyEdge](e: E): E := TitanEdges =
      e := Container(twitterGraph.query.has("label", e.label).edges.asTitanEdges)

    val users = vertices(user)
    val tweets = vertices(tweet)
    val postEdges = edges(posted)

    // val names = name := Container[String](Iterable("@eparejatobes", "@laughedelic", "@evdokim"))
    // val ages = age := Container[Integer](Iterable(95, 5, 22))
    // val times = time := Container[String](Iterable(
    //   "27.10.2013", "20.3.2013", "19.2.2013", "13.11.2012", "15.2.2014",
    //   "7.2.2014", "23.2.2012", "7.7.2011", "22.6.2011"
    // ))

    val usrs = users.value.values.toList
    val edu = usrs(0)
    val alexey = usrs(1)
    val kim = usrs(2)
  }
  import testSamples._

  test("checking evals for the basic structure") {
    import t.evals.categoryStructure._
    import queries.categoryStructure._

    assertTaggedEq( eval(q_id)(users), users )
    assertTaggedEq( eval(q_comp1)(users), users )
    assertTaggedEq( eval(q_comp2)(users), users )
  }

  test("checking evals for the property structure") {
    import t.evals.categoryStructure._
    val ps = t.evals.propertyStructure(twitterGraph); import ps._
    import queries.propertyStructure._

    // assertTaggedEq( eval(q_getV)(users), ages )
    // assertTaggedEq( eval(q_lookupV)(names), users )
    // assertTaggedEq( eval(q_compV)(names), ages )

    // assertTaggedEq( eval(q_getE)(postEdges), times )
    // assertTaggedEq( eval(q_lookupE)(times), postEdges )
    // assertTaggedEq( eval(q_compE)(postEdges), postEdges )
  }

  test("checking evals for the tensor structure") {
    import t.evals.categoryStructure._
    val ts = t.evals.tensorStructure(twitterGraph); import ts._
    import queries.tensorStructure._

    assertTaggedEq( eval(q_tensor)(users ⊗ users ⊗ users), users ⊗ users ⊗ users )
    assertTaggedEq( eval(q_dupl)(users ⊗ users), users ⊗ users ⊗ users )
    assertTaggedEq( eval(q_match)(users ⊗ users), users )
    assertTaggedEq( eval(q_comp)(users ⊗ users), users )
  }

  test("checking evals for the biproduct structure") {
    import t.evals.categoryStructure._
    import t.evals.biproductStructure._
    import queries.biproductStructure._

    assertTaggedEq( eval(q_inj)(tweets), nousers ⊕ nousers ⊕ tweets )
    assertTaggedEq( eval(q_bip)(users ⊕ users ⊕ tweets), users ⊕ users ⊕ tweets )
    assertTaggedEq( eval(q_fork)(users ⊕ tweets), users ⊕ users ⊕ tweets )
    assertTaggedEq( eval(q_merge)(users ⊕ users),
      user := Container(users.value.values ++ users.value.values)
    )
    assertTaggedEq( eval(q_comp)(users ⊕ tweets),
      tweet := Container(tweets.value.values ++ tweets.value.values)
    )
  }

  test("checking evals for the graph structure") {
    import t.evals.categoryStructure._
    import t.evals.graphStructure._
    import queries.graphStructure._

    val repeated = user := Container[core.TitanVertex](
      Iterable(edu, edu, edu, edu, alexey, alexey, kim, kim, kim)
    )

    assertTaggedEq( eval(q_outV)(users), tweets )
    assertTaggedEq( eval(q_inV)(tweets), repeated )
    assertTaggedEq( eval(q_compV)(users), repeated )

    assertTaggedEq( eval(q_outE)(users), tweets )
    assertTaggedEq( eval(q_inE)(tweets), repeated )
    assertTaggedEq( eval(q_compE)(users), repeated )
  }

  test("checking evals for the predicate structure") {
    import t.evals.categoryStructure._
    import t.evals.predicateStructure._
    import queries.predicateStructure._

    val filtered = Container[core.TitanVertex](Iterable(edu, kim))

    assertTaggedEq( eval(q_quant)(users), pred := filtered )
    assertTaggedEq( eval(q_coerce)(pred := filtered), user := filtered )
    assertTaggedEq( eval(q_comp)(users), user := filtered )
  }

}
