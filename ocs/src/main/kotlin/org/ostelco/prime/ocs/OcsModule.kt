package org.ostelco.prime.ocs

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.setup.Environment
import org.hibernate.validator.constraints.NotEmpty
import org.ostelco.prime.analytics.DataConsumptionInfoPublisher
import org.ostelco.prime.disruptor.ClearingEventHandler
import org.ostelco.prime.disruptor.EventProducerImpl
import org.ostelco.prime.disruptor.OcsDisruptor
import org.ostelco.prime.events.EventProcessor
import org.ostelco.prime.module.PrimeModule
import org.ostelco.prime.thresholds.ThresholdChecker

@JsonTypeName("ocs")
class OcsModule : PrimeModule {

    @JsonProperty("config")
    lateinit var config: OcsConfig

    override fun init(env: Environment) {

        val disruptor = OcsDisruptor()

        // Disruptor provides RingBuffer, which is used by Producer
        val producer = EventProducerImpl(disruptor.disruptor.ringBuffer)

        // OcsSubscriberServiceSingleton uses Producer to produce events for incoming requests from Client App
        OcsPrimeServiceSingleton.init(producer)

        // OcsService uses Producer to produce events for incoming requests from P-GW
        val ocsService = OcsService(producer)

        // OcsServer assigns OcsService as handler for gRPC requests
        val server = OcsGrpcServer(8082, ocsService.asOcsServiceImplBase())

        val dataConsumptionInfoPublisher = DataConsumptionInfoPublisher(
                config.projectId,
                config.topicId)

        val thresholdChecker = ThresholdChecker(config.lowBalanceThreshold)

        // Events flow:
        //      Producer:(OcsService, Subscriber)
        //          -> Handler:(OcsState)
        //              -> Handler:(OcsService, Subscriber, AnalyticsPublisher)
        //                  -> Clear

        disruptor.disruptor
                .handleEventsWith(OcsState())
                .then(ocsService.asEventHandler(), EventProcessor(), thresholdChecker, dataConsumptionInfoPublisher)
                .then(ClearingEventHandler())

        // dropwizard starts Analytics events publisher
        env.lifecycle().manage(dataConsumptionInfoPublisher)
        // dropwizard starts disruptor
        env.lifecycle().manage(disruptor)
        // dropwizard starts server
        env.lifecycle().manage(server)
    }
}

class OcsConfig {

    @NotEmpty
    @JsonProperty("projectId")
    lateinit var projectId: String

    @NotEmpty
    @JsonProperty("topicId")
    lateinit var topicId: String


    @NotEmpty
    @JsonProperty("lowBalanceThreshold")
    var lowBalanceThreshold: Long = 0
}
