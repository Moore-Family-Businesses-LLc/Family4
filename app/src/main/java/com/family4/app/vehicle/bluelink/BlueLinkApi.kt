package com.family4.app.vehicle.bluelink

import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit interface for the Hyundai BlueLink Connected Car API.
 *
 * BASE URL: https://prd.ca-ccapi.hyundai.com:8080
 *
 * All endpoints require an Authorization header: "Bearer <access_token>"
 * Populated automatically by [BlueLinkAuthManager] via an OkHttp interceptor.
 *
 * ⚠️  ACCESS GATED: You need a CLIENT_ID from developer.hyundai.com.
 *     Add to local.properties: BLUELINK_CLIENT_ID=xxx BLUELINK_CLIENT_SECRET=xxx
 *     Until then the repository returns stub/demo data so the UI always has
 *     something to display.
 */
interface BlueLinkApi {

    // ── Auth ──────────────────────────────────────────────────────────────

    @FormUrlEncoded
    @POST("/api/v1/user/oauth2/token")
    suspend fun exchangeToken(
        @Field("grant_type")    grantType:    String = "authorization_code",
        @Field("code")          code:         String,
        @Field("redirect_uri")  redirectUri:  String,
        @Field("client_id")     clientId:     String,
        @Field("client_secret") clientSecret: String
    ): Response<BlueLinkTokenResponse>

    @FormUrlEncoded
    @POST("/api/v1/user/oauth2/token")
    suspend fun refreshToken(
        @Field("grant_type")    grantType:    String = "refresh_token",
        @Field("refresh_token") refreshToken: String,
        @Field("client_id")     clientId:     String,
        @Field("client_secret") clientSecret: String
    ): Response<BlueLinkTokenResponse>

    // ── Vehicles ──────────────────────────────────────────────────────────

    @GET("/api/v1/vcs/ccsp/vehicles")
    suspend fun getVehicles(
        @Header("Authorization") auth: String
    ): Response<BlueLinkVehicleListResponse>

    // ── Vehicle status ────────────────────────────────────────────────────

    @GET("/api/v1/vcs/ccsp/vehicleStatus/{vinId}")
    suspend fun getVehicleStatus(
        @Header("Authorization") auth: String,
        @Path("vinId") vinId: String
    ): Response<BlueLinkStatusResponse>

    // ── EV / Battery (EV & PHEV models only) ─────────────────────────────

    @GET("/api/v1/vcs/ccsp/vehicles/{vinId}/status/latest/charge")
    suspend fun getChargeStatus(
        @Header("Authorization") auth: String,
        @Path("vinId") vinId: String
    ): Response<BlueLinkChargeResponse>

    // ── Remote commands ───────────────────────────────────────────────────

    @POST("/api/v1/vcs/ccsp/vehicles/{vinId}/control/door")
    suspend fun controlDoor(
        @Header("Authorization") auth: String,
        @Path("vinId") vinId: String,
        @Body body: DoorControlRequest
    ): Response<Unit>

    @POST("/api/v1/vcs/ccsp/vehicles/{vinId}/control/engine")
    suspend fun controlEngine(
        @Header("Authorization") auth: String,
        @Path("vinId") vinId: String,
        @Body body: EngineControlRequest
    ): Response<Unit>

    @POST("/api/v1/vcs/ccsp/vehicles/{vinId}/control/climate")
    suspend fun controlClimate(
        @Header("Authorization") auth: String,
        @Path("vinId") vinId: String,
        @Body body: ClimateControlRequest
    ): Response<Unit>

    @POST("/api/v1/vcs/ccsp/vehicles/{vinId}/control/charge")
    suspend fun controlCharging(
        @Header("Authorization") auth: String,
        @Path("vinId") vinId: String,
        @Body body: ChargeControlRequest
    ): Response<Unit>
}

// ── Request / Response DTOs ──────────────────────────────────────────────────

data class BlueLinkTokenResponse(
    val access_token: String,
    val refresh_token: String,
    val token_type: String,
    val expires_in: Int
)

data class BlueLinkVehicleListResponse(
    val vehicles: List<BlueLinkVehicle>
)

data class BlueLinkVehicle(
    val vehicleId: String,
    val vin:        String,
    val nickname:   String,
    val modelName:  String,
    val modelYear:  String,
    val colorName:  String
)

data class BlueLinkStatusResponse(
    val vehicleStatus: BlueLinkVehicleStatus
)

data class BlueLinkVehicleStatus(
    val engine:      Boolean    = false,
    val doorLocked:  Boolean    = true,
    val odometer:    Int?       = null,
    val fuelLevel:   Double?    = null,
    val tirePressure: TirePressure? = null,
    val latitude:    Double?    = null,
    val longitude:   Double?    = null
)

data class TirePressure(
    val frontLeft:  Double?,
    val frontRight: Double?,
    val rearLeft:   Double?,
    val rearRight:  Double?
)

data class BlueLinkChargeResponse(
    val evStatus: BlueLinkEvStatus
)

data class BlueLinkEvStatus(
    val batteryStatus:      Double  = 0.0,  // SOC %
    val remainingChargeTime: Int    = 0,    // minutes
    val estimatedRange:     Double  = 0.0,  // km
    val chargePortConnected: Boolean = false,
    val charging:           Boolean = false
)

data class DoorControlRequest(val action: String)          // "lock" | "unlock"
data class EngineControlRequest(val action: String)        // "start" | "stop"
data class ClimateControlRequest(val action: String, val temperature: Int = 22)
data class ChargeControlRequest(val action: String)        // "start" | "stop"
