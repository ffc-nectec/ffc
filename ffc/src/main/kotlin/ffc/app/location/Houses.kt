package ffc.app.location

import ffc.api.ApiErrorException
import ffc.api.FfcCentral
import ffc.app.isDev
import ffc.app.person.mockPerson
import ffc.app.util.RepoCallback
import ffc.app.util.TaskCallback
import ffc.entity.House
import ffc.entity.Organization
import ffc.entity.Person
import ffc.entity.update
import me.piruin.geok.geometry.Point
import retrofit2.dsl.enqueue

interface Houses {

    fun house(id: String, callbackDsl: RepoCallback<House>.() -> Unit)
}

interface HouseManipulator {

    fun commit(callbackDsl: TaskCallback<House>.() -> Unit)
}

fun housesOf(org: Organization): Houses = if (isDev) DummyHouses() else ApiHouses(org)

private class ApiHouses(val org: Organization) : Houses {

    val api = FfcCentral().service<PlaceService>()

    override fun house(id: String, callbackDsl: RepoCallback<House>.() -> Unit) {
        val callback = RepoCallback<House>().apply(callbackDsl)
        api.get(org.id, id).enqueue {
            onSuccess {
                callback.onFound!!.invoke(body()!!)
            }
            onError {
                callback.onFail!!.invoke(ApiErrorException(this))
            }
            onFailure {
                callback.onFail!!.invoke(it)
            }
        }
    }
}

private class ApiHouseManipulator(val org: Organization, val house: House) : HouseManipulator {

    val api = FfcCentral().service<PlaceService>()

    override fun commit(callbackDsl: TaskCallback<House>.() -> Unit) {
        val callback = TaskCallback<House>().apply(callbackDsl)
        api.updateHouse(org.id, house).enqueue {
            onSuccess {
                callback.result(body()!!)
            }
            onError {
                callback.expception!!.invoke(ApiErrorException(this))
            }
            onFailure {
                callback.expception!!.invoke(it)
            }
        }
    }
}

private class DummyHouses(val house: House? = null) : Houses, HouseManipulator {

    override fun commit(callbackDsl: TaskCallback<House>.() -> Unit) {
        house!!.update {}
        val callback = TaskCallback<House>().apply(callbackDsl)
        callback.result(house!!)
    }

    override fun house(id: String, callbackDsl: RepoCallback<House>.() -> Unit) {
        val callback = RepoCallback<House>().apply(callbackDsl)
        callback.onFound!!.invoke(House().apply {
            no = "112 อุทธยานวิทยาศาสตร์"
            location = Point(13.0, 102.0)
        })
    }
}

internal fun House.resident(orgId: String, callbackDsl: RepoCallback<List<Person>>.() -> Unit) {
    val callback = RepoCallback<List<Person>>().apply(callbackDsl)

    if (isDev) {
        callback.onFound!!.invoke(listOf(mockPerson))
    } else {
        FfcCentral().service<HouseApi>().personInHouse(orgId, this.id).enqueue {
            onSuccess { callback.onFound!!.invoke(body()!!) }
            onClientError { callback.onNotFound!!.invoke() }
            onServerError { callback.onFail!!.invoke(ApiErrorException(this)) }
            onFailure { callback.onFail!!.invoke(it) }
        }
    }
}

internal fun House.editor(org: Organization): HouseManipulator {
    return if (isDev) DummyHouses(this) else ApiHouseManipulator(org, this)
}
