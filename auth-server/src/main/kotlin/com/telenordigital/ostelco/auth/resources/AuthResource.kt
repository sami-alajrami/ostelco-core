package com.telenordigital.ostelco.auth.resources

import com.google.firebase.auth.FirebaseAuth
import org.slf4j.LoggerFactory
import java.util.*
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.core.Response


@Path("/auth")
class AuthResource {

    private val LOG = LoggerFactory.getLogger(AuthResource::class.java)

    @GET
    @Path("/token")
    fun getAuthToken(@HeaderParam("X-MSISDN") msisdn: String): Response {
        val additionalClaims = HashMap<String, String>()
        additionalClaims["msisdn"] = msisdn
        val customToken = FirebaseAuth.getInstance().createCustomTokenAsync(getUid(msisdn)).get()
        return Response.ok(customToken).build()
    }

    /**
     * As of now, `msisdn` is considered as `user-id`.
     * This is subjected to change in future.
     */
    private fun getUid(msisdn: String): String {
        return msisdn;
    }
}
