package ffc.app.person

import ffc.api.FfcCentral
import ffc.api.ServerErrorException
import ffc.app.isDev
import ffc.app.util.RepoCallback
import ffc.entity.Person
import ffc.entity.ThaiCitizenId
import org.joda.time.LocalDate
import retrofit2.Retrofit
import retrofit2.dsl.enqueue
import retrofit2.dsl.then

interface Persons {

    fun person(personId: String, dsl: RepoCallback<Person>.() -> Unit)

    fun add(person: Person, callback: (Person?, Throwable?) -> Unit)
}

private class InMemoryPersons : Persons {
    override fun person(personId: String, dsl: RepoCallback<Person>.() -> Unit) {
        val callback = RepoCallback<Person>().apply(dsl)
        callback.always?.invoke()
        val person = repository.firstOrNull { person -> person.id == personId }
        if (person != null) {
            callback.onFound!!.invoke(person)
        } else {
            callback.onNotFound!!.invoke()
        }
    }

    override fun add(person: Person, callback: (Person?, Throwable?) -> Unit) {
        repository.add(person)
        callback(person, null)
    }

    companion object {
        val repository: MutableList<Person> = mutableListOf(mockPerson)
    }
}

private class ApiPersons(val orgId: String) : Persons {

    val api = FfcCentral().service<PersonsApi>()

    override fun add(person: Person, callback: (Person?, Throwable?) -> Unit) {
        api.post(orgId, person).then {
            callback(it, null)
        }.catch { res, t ->
            res?.let { callback(null, ServerErrorException(it)) }
            t?.let { callback(null, it) }
        }
    }

    override fun person(personId: String, dsl: RepoCallback<Person>.() -> Unit) {
        val callback = RepoCallback<Person>().apply(dsl)
        api.get(orgId, personId).enqueue {
            always { callback.always?.invoke() }
            onSuccess {
                callback.onFound!!.invoke(body()!!)
            }
            onError {
                if (code() == 404)
                    callback.onNotFound!!.invoke()
                else
                    callback.onFail!!.invoke(ServerErrorException(this))
            }
            onFailure { callback.onFail!!.invoke(it) }
        }
    }
}

val mockPerson = Person("5b9770e029191b0004c91a56").apply {
    birthDate = LocalDate.parse("1988-02-15")
    prename = "นาย"
    firstname = "พิรุณ"
    lastname = "พานิชผล"
    sex = Person.Sex.MALE
    identities.add(ThaiCitizenId("1145841548789"))
}

fun persons(orgId: String): Persons = if (isDev) InMemoryPersons() else ApiPersons(orgId)
