package cn.edu.buaa.scs

import cn.edu.buaa.scs.model.Project
import kotlinx.coroutines.runBlocking
import org.bson.codecs.pojo.annotations.BsonId
import org.junit.jupiter.api.Test
import org.ktorm.jackson.KtormModule
import org.litote.kmongo.Id
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.div
import org.litote.kmongo.eq
import org.litote.kmongo.newId
import org.litote.kmongo.reactivestreams.KMongo
import org.litote.kmongo.util.KMongoConfiguration

class KmongoTest {

    private val client = KMongo.createClient("mongodb://mongoadmin:secret@localhost:27017").coroutine

    private val db = client.getDatabase("cloud")

    @Test
    fun test() {
        class LightSaber(val _id: Id<LightSaber> = newId())
        class Jedi(
            @BsonId @Suppress("unused") val key: Id<Jedi> = newId(),
            //set of typed ids, now I see what it is!
            @Suppress("unused") val sabers: Set<Id<LightSaber>> = emptySet()
        )

        val collection = db.getCollection<Jedi>()
        val lightSaber = LightSaber()
        runBlocking {
            collection.insertOne(Jedi(sabers = setOf(lightSaber._id)))
        }
    }

    @Test
    fun testInnerClass() {
        data class InnerClass(
            val _id: Id<InnerClass> = newId(),
            val name: String,
            val age: Int
        )

        data class OuterClass(
            val _Id: Id<OuterClass> = newId(),
            val innerList: List<InnerClass>
        )
        runBlocking {
            val col = db.getCollection<OuterClass>()
            col.insertOne(
                OuterClass(
                    innerList = listOf(
                        InnerClass(name = "a", age = 1),
                        InnerClass(name = "b", age = 2)
                    )
                )
            )
            val t = col.findOne(OuterClass::innerList / InnerClass::name eq "a")
            println(t)
        }
    }

    @Test
    fun updateTest() {
        runBlocking {
            data class UpdateData(
                val _id: Id<UpdateData> = newId(),
                val age: Int,
            )

            val col = db.getCollection<UpdateData>()
            col.deleteMany()
            col.insertOne(UpdateData(age = 7))
            val oldData = col.findOne(UpdateData::age eq 7) ?: throw Exception("not found oldData")
            val newUpdateData = UpdateData(age = 8)
            col.updateOneById(oldData._id, newUpdateData)
            col.find().toList().let { list ->
                assert(list.size == 1)
                assert(list[0].age == 8)
            }
        }
    }

    @Test
    fun transactionTest() {
        runBlocking {
            data class TransactionData(
                val _id: Id<TransactionData> = newId(),
                val age: Int,
            )

            val col = db.getCollection<TransactionData>()
            col.deleteMany()
            try {
                client.startSession().use { session ->
                    session.startTransaction()
                    col.insertOne(session, TransactionData(age = 7))
                    throw Exception()
                }
            } catch (e: Exception) {
                println(e.stackTraceToString())
            }
            assert(col.findOne(TransactionData::age eq 7) == null)
        }
    }

    @Test
    fun ktormTest() {
        runBlocking {
            KMongoConfiguration.registerBsonModule(KtormModule())
            data class A(
                @BsonId val _id: Id<A> = newId(),
                val project: Project
            )

            val col = db.getCollection<A>()
            col.deleteMany()
            col.insertOne(A(project = Project { this.name = "test-project" }))
            val projectList = col.find().toList()
            assert(projectList.isNotEmpty())
            println(projectList[0])
            val project =
                col.findOne(A::project.div(Project::name).eq("test-project"))?.project
                    ?: throw Exception("not found")
            println(project)
            col.deleteMany()
        }
    }

    @Test
    fun readNonTest() {
        runBlocking {
            suspend {
                data class A(
                    @BsonId val _id: Id<A> = newId(),
                )

                val col = db.getCollection<A>()
                col.deleteMany()
            }.invoke()
            suspend {
                data class A(
                    @BsonId val _id: Id<A> = newId(),
                )

                val col = db.getCollection<A>()
                col.insertOne(A())
            }.invoke()
            suspend {
                data class A(
                    @BsonId val _id: Id<A> = newId(),
                    val age: Int = 8,
                )

                val col = db.getCollection<A>()
                assert(col.find().toList().isNotEmpty())
                println(col.find().toList()[0])
            }.invoke()
            suspend {
                data class B(
                    val name: String,
                    val age: Int,
                )

                data class A(
                    @BsonId val _id: Id<A> = newId(),
                    val age: Int = 8,
                    val b: B?,
                )

                val col = db.getCollection<A>()
                assert(col.find().toList().isNotEmpty())
                println(col.find().toList()[0])
            }.invoke()
        }
    }
}