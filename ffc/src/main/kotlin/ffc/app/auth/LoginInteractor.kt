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

package ffc.app.auth

import ffc.api.FfcCentral
import ffc.app.auth.exception.LoginErrorException
import ffc.app.auth.exception.LoginFailureException
import ffc.entity.Organization
import okhttp3.Credentials
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import retrofit2.dsl.enqueue
import java.nio.charset.Charset

internal class LoginInteractor(
    var presenter: LoginPresenter,
    var auth: Authentication,
    relogin: Boolean = false
) {
    private val orgService = FfcCentral().service<OrgService>()

    init {
        if (relogin) {
            orgService.listOrgs(auth.org!!.name).enqueue {
                onSuccess {
                    org = body()!!.get(0)
                }
                onError {
                    presenter.showOrgSelector()
                }
                onFailure {
                    presenter.showOrgSelector()
                    presenter.onError(LoginFailureException(it.message ?: "Something wrong"))
                }
            }
        } else {
            if (auth.isLoggedIn) {
                FfcCentral.token = auth.token
                presenter.onLoginSuccess()
            } else {
                presenter.showOrgSelector()
            }
        }
    }

    var org: Organization? = null
        set(value) {
            field = value
            if (value != null) presenter.onOrgSelected(value)
        }

    private val utf8 = Charset.forName("UTF-8")

    fun doLogin(username: String, password: String) {
        check(org != null) { "Must set org before" }
        val basicToken = Credentials.basic(username.trim(), password.trim(), utf8)
        orgService.createAuthorize(org!!.id, basicToken, EmptyJsonBody()).enqueue {
            onSuccess {
                val authorize = body()!!
                FfcCentral.token = authorize.token
                auth.org = org
                auth.token = authorize.token
                auth.user = authorize.user
                users().user(username) { user, t ->
                    if (user != null) {
                        presenter.onLoginSuccess()
                    } else {
                        presenter.onError(t
                            ?: IllegalStateException("เกิดข้อผิดพลาดไม่สามารถระบุได้"))
                    }
                }
            }
            onError {
                presenter.onError(LoginErrorException(this))
            }
            onFailure {
                presenter.onError(LoginFailureException(it.message
                    ?: "เกิดข้อผิดพลาดไม่สามารถระบุได้"))
            }
        }
    }

    /**
     * Prevent 400 Bad Request because content-length is require when using http1.1
     */
    private class EmptyJsonBody : RequestBody() {
        override fun contentType(): MediaType? = MediaType.parse("application/json")

        override fun contentLength(): Long = 0

        override fun writeTo(sink: BufferedSink) {
            sink.emit()
        }
    }
}
