/*
 * Copyright (c) 2018 NECTEC
 *   National Electronics and Computer Technology Center, Thailand
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ffc.v3

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap
import com.google.maps.android.data.geojson.GeoJsonLayer
import com.google.maps.android.data.geojson.GeoJsonPointStyle
import ffc.entity.gson.toJson
import ffc.v3.R.raw
import ffc.v3.api.FfcCentral
import ffc.v3.api.PlaceService
import ffc.v3.location.MarkLocationActivity
import ffc.v3.util.animateCameraTo
import ffc.v3.util.drawable
import ffc.v3.util.find
import ffc.v3.util.gone
import ffc.v3.util.moveCameraTo
import ffc.v3.util.toBitmap
import kotlinx.android.synthetic.main.activity_maps.addLocationButton
import me.piruin.geok.geometry.Point
import org.jetbrains.anko.dimen
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.toast
import org.json.JSONObject
import retrofit2.dsl.enqueue
import retrofit2.dsl.isNotFound

const val REQ_ADD_LOCATION = 10214

class MapsActivity : BaseActivity(), OnMapReadyCallback {

  private lateinit var map: GoogleMap

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_maps)

    addLocationButton.gone()

    supportFragmentManager.find<SupportMapFragment>(R.id.mapFragment).getMapAsync(this)
  }

  override fun onMapReady(googleMap: GoogleMap) {
    map = googleMap.apply {
      setPadding(0, dimen(R.dimen.maps_padding_top), 0, 0)
    }
    addLocationButton.setOnClickListener {
      startActivityForResult<MarkLocationActivity>(
        REQ_ADD_LOCATION,
        "target" to map.cameraPosition.target,
        "zoom" to map.cameraPosition.zoom
      )
    }
    showGeoJson()

    checkHouseNoLocation()

  }

  private fun checkHouseNoLocation() {
    val placeService = FfcCentral().service<PlaceService>()
    placeService.listHouseNoLocation(org.id.toLong()).enqueue {
      onSuccess {
        addLocationButton.show()
      }
      onClientError {
        when {
          isNotFound -> addLocationButton.hide()
        }
      }
    }
  }

  private fun showGeoJson() {
    val placeService = FfcCentral().service<PlaceService>()
    placeService.listHouseGeoJson(org.id.toLong()).enqueue {

      onSuccess {
        val coordinates = (body()!!.features[0].geometry as Point).coordinates
        map.animateCameraTo(coordinates.latitude, coordinates.longitude, 10.0f)
        with(GeoJsonLayer(map, JSONObject(body()!!.toJson()))) {
          features.forEach {
            it.pointStyle = GeoJsonPointStyle().apply {
              icon = if (it.getProperty("haveChronics") == "true") chronicHomeIcon else homeIcon
              title = "บ้านเลขที่ ${it.getProperty("no")}"
              snippet = it.getProperty("coordinates").trimMargin()
            }
          }
          setOnFeatureClickListener {
            startActivityForResult<HouseActivity>(
              REQ_ADD_LOCATION,
              "houseId" to it.getProperty("_id"))
          }
          addLayerToMap()
        }
      }

      onError {
        toast("Not success get geoJson ${code()} ")
        if (BuildConfig.DEBUG) {
          map.moveCameraTo(13.0, 102.1, 10.0f)
          GeoJsonLayer(map, raw.place, this@MapsActivity)
        }
      }

      onFailure {
        toast("${it.message}")
      }
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    when (requestCode) {
      REQ_ADD_LOCATION -> {
        if (resultCode == Activity.RESULT_OK) {
          map.clear()
          showGeoJson()
        }
      }
    }
  }

  private val homeIcon by lazy { fromBitmap(drawable(R.drawable.ic_home_black_24px).toBitmap()) }
  private val chronicHomeIcon by lazy { fromBitmap(drawable(R.drawable.ic_home_red_24px).toBitmap()) }
}
