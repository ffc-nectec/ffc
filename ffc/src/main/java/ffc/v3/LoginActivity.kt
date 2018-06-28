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

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.os.Bundle
import android.view.View
import ffc.entity.Organization
import ffc.entity.Token
import ffc.entity.gson.toJson
import ffc.v3.R.string
import ffc.v3.api.FfcCentral
import ffc.v3.api.OrgService
import ffc.v3.util.assertThat
import ffc.v3.util.debug
import ffc.v3.util.debugToast
import ffc.v3.util.get
import ffc.v3.util.gone
import ffc.v3.util.notNullOrBlank
import ffc.v3.util.observe
import ffc.v3.util.put
import ffc.v3.util.viewModel
import ffc.v3.util.visible
import kotlinx.android.synthetic.main.activity_login.password
import kotlinx.android.synthetic.main.activity_login.password_layout
import kotlinx.android.synthetic.main.activity_login.submit
import kotlinx.android.synthetic.main.activity_login.username
import kotlinx.android.synthetic.main.activity_login.username_layout
import me.piruin.spinney.Spinney
import okhttp3.Credentials
import org.jetbrains.anko.contentView
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.indeterminateProgressDialog
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.toast
import org.joda.time.DateTime
import retrofit2.dsl.enqueue
import retrofit2.dsl.then
import java.nio.charset.Charset

class LoginActivity : BaseActivity() {

  private val organization by lazy { findViewById<Spinney<Organization>>(R.id.org) }
  private val orgService = FfcCentral().service<OrgService>()
  private val viewModel: LoginViewModel by lazy { viewModel<LoginViewModel>() }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_login)

    val authorize = defaultSharedPreferences.get<Token>("token")
    if (authorize?.isNotExpire == true) {
      debugToast("Use last token")
      FfcCentral.TOKEN = authorize.token.toString()
      startActivity(intentFor<MapsActivity>())
      finish()
      return
    }

    organization.gone()
    organization.setItemPresenter { item, _ -> (item as ffc.entity.Organization).name }
    organization.setOnItemSelectedListener { _, selectedItem, _ ->
      viewModel.choosedOrg.value = selectedItem
    }

    submit.setOnClickListener {
      try {
        assertThat(isOnline) { getString(R.string.please_check_connectivity) }
        doLogin(username.text.toString(), password.text.toString())
      } catch (assert: IllegalArgumentException) {
        toast(assert.message ?: "Error")
      }
    }
    if (BuildConfig.DEBUG) {
      submit.setOnLongClickListener {
        username.setText("admin0")
        password.setText("1234admin0")
        true
      }
    }

    if (viewModel.orgList.value?.isEmpty() == true)
      requestMyOrg()
    else {
      organization.setItems(viewModel.orgList.value!!)
      organization.visible()
    }

    if (viewModel.choosedOrg.value != null) {
      organization.selectedItem = viewModel.choosedOrg.value
    } else {
      username_layout.gone()
      password_layout.gone()
      submit.gone()
    }

    viewModel.orgList.observe(this) {
      organization.setItems(it!!)
      organization.visible()

      if (it.size == 1) organization.selectedItem = it[0]
    }

    viewModel.choosedOrg.observe(this) {
      val visible = if (it != null) View.VISIBLE else View.GONE
      username_layout.visibility = visible
      password_layout.visibility = visible
      submit.visibility = visible
    }
  }

  private fun doLogin(username: String?, password: String?) {
    assertThat(username.notNullOrBlank()) { getString(string.no_username) }
    assertThat(password.notNullOrBlank()) { getString(string.no_password) }

    val basicToken =
      Credentials.basic(username!!.trim(), password!!.trim(), Charset.forName("UTF-8"))
    debug("Basic Auth = %s", basicToken)

    val dialog = indeterminateProgressDialog(getString(string.checking_identity))
    val org = viewModel.choosedOrg.value!!
    orgService.createAuthorize(org.id.toLong(), basicToken).enqueue {
      always {
        dialog.dismiss()
      }
      onSuccess {
        val authorize = body()!!
        if (authorize.expireDate == null)
          authorize.expireDate = DateTime.now().plusDays(1)
        debugToast("Authorize ${authorize.toJson()}")
        FfcCentral.TOKEN = authorize.token.toString()
        defaultSharedPreferences.edit()
          .put("token", authorize)
          .put("org", org)
          .apply()
        startActivity(intentFor<MapsActivity>())
      }
      onError {
        snackbar(contentView!!, string.identification_error)
      }
      onFailure {
        it.printStackTrace()
        toast(it.message!!)
      }
    }
  }

  override fun onConnectivityChanged(isConnect: Boolean, message: String) {
    //super.onConnectivityChanged(isConnect, "please check internet connection")
  }

  private fun requestMyOrg() {
    try {
      assertThat(isOnline) { getString(R.string.please_check_connectivity) }

      orgService.myOrg().then {
        viewModel.orgList.value = it
      }.catch { _, t ->
        requestOrgList()
        t?.let { toast(it.message ?: it.toString()) }
      }
    } catch (offline: IllegalArgumentException) {
      contentView?.let {
        indefiniteSnackbar(it, R.string.please_check_connectivity, R.string.retry) {
          requestMyOrg()
        }
      }
    }
  }

  private fun requestOrgList() {
    orgService.listOrgs().then {
      if (it.isEmpty()) {
        toast(string.not_found_org)
        viewModel.orgList.value = listOf()
      } else {
        viewModel.orgList.value = it
      }
    }.catch { res, t ->
      t?.let { toast(it.message ?: it.toString()) }
      contentView?.let {
        indefiniteSnackbar(it, R.string.cannnot_load_organization, R.string.retry) {
          requestOrgList()
        }
      }
    }
  }

  class LoginViewModel : ViewModel() {
    val orgList: MutableLiveData<List<Organization>> by lazy {
      MutableLiveData<List<Organization>>().apply {
        value = listOf()
      }
    }
    val choosedOrg: MutableLiveData<Organization> by lazy { MutableLiveData<Organization>() }
  }
}
