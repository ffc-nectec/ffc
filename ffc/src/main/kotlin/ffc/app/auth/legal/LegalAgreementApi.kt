package ffc.app.auth.legal

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface LegalAgreementApi {

    @GET("legal/{type}/latest")
    fun latest(@Path("type") type: LegalType): Call<String>

    @GET("org/{orgId}/user/{userId}/agreement/{type}/latest")
    fun checkAgreement(@Path("type") type: LegalType,
                       @Path("userId") userId: String,
                       @Path("orgId") orgId: String): Call<Unit>

    @PUT("org/{orgId}/user/{userId}/agreement/{type}/{version}")
    fun agreeWith(@Path("type") type: LegalType,
                  @Path("version") version: String,
                  @Path("userId") userId: String,
                  @Path("orgId") orgId: String): Call<Unit>
}
