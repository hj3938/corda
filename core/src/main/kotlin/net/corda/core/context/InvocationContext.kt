package net.corda.core.context

import net.corda.core.contracts.ScheduledStateRef
import net.corda.core.identity.CordaX500Name
import net.corda.core.serialization.CordaSerializable
import java.security.Principal

/**
 * Models the information needed to trace an invocation in Corda.
 * Includes initiating actor, origin, trace information, and optional external trace information to correlate clients' IDs.
 *
 * @param origin origin of the invocation.
 * @param trace Corda invocation trace.
 * @param actor acting agent of the invocation, used to derive the security principal.
 * @param externalTrace optional external invocation trace for cross-system logs correlation.
 * @param impersonatedActor optional impersonated actor, used for logging but not for authorisation.
 */
@CordaSerializable
data class InvocationContext(val origin: Origin, val trace: Trace, val actor: Actor?, val externalTrace: Trace? = null, val impersonatedActor: Actor? = null) {

    companion object {

        /**
         * Creates an [InvocationContext] with a [Trace] that defaults to a [java.util.UUID] as value and [java.time.Instant.now] timestamp.
         */
        @JvmStatic
        fun newInstance(origin: Origin, trace: Trace = Trace.newInstance(), actor: Actor? = null, externalTrace: Trace? = null, impersonatedActor: Actor? = null) = InvocationContext(origin, trace, actor, externalTrace, impersonatedActor)

        /**
         * Creates an [InvocationContext] with [Origin.RPC] origin.
         */
        @JvmStatic
        fun rpc(actor: Actor, trace: Trace = Trace.newInstance(), externalTrace: Trace? = null, impersonatedActor: Actor? = null): InvocationContext = newInstance(Origin.RPC(actor), trace, actor, externalTrace, impersonatedActor)

        /**
         * Creates an [InvocationContext] with [Origin.Peer] origin.
         */
        @JvmStatic
        fun peer(party: CordaX500Name, trace: Trace = Trace.newInstance(), externalTrace: Trace? = null, impersonatedActor: Actor? = null): InvocationContext = newInstance(Origin.Peer(party), trace, null, externalTrace, impersonatedActor)

        /**
         * Creates an [InvocationContext] with [Origin.Service] origin.
         */
        @JvmStatic
        fun service(serviceClassName: String, owningLegalIdentity: CordaX500Name, trace: Trace = Trace.newInstance(), externalTrace: Trace? = null): InvocationContext = newInstance(Origin.Service(serviceClassName, owningLegalIdentity), trace, null, externalTrace)

        /**
         * Creates an [InvocationContext] with [Origin.Scheduled] origin.
         */
        @JvmStatic
        fun scheduled(scheduledState: ScheduledStateRef, trace: Trace = Trace.newInstance(), externalTrace: Trace? = null): InvocationContext = newInstance(Origin.Scheduled(scheduledState), trace, null, externalTrace)

        /**
         * Creates an [InvocationContext] with [Origin.Shell] origin.
         */
        @JvmStatic
        fun shell(trace: Trace = Trace.newInstance(), externalTrace: Trace? = null): InvocationContext = InvocationContext(Origin.Shell, trace, null, externalTrace)
    }

    /**
     * Associated security principal.
     */
    fun principal(): Principal = origin.principal()
}

/**
 * Models an initiator in Corda, can be a user, a service, etc.
 */
@CordaSerializable
data class Actor(val id: Id, val serviceId: AuthServiceId, val owningLegalIdentity: CordaX500Name) {

    companion object {
        @JvmStatic
        fun service(serviceClassName: String, owningLegalIdentity: CordaX500Name): Actor = Actor(Id(serviceClassName), AuthServiceId("SERVICE"), owningLegalIdentity)
    }

    /**
     * Actor id.
     */
    @CordaSerializable
    data class Id(val value: String)

}

/**
 * Invocation origin for tracing purposes.
 */
@CordaSerializable
sealed class Origin {

    /**
     * Returns the [Principal] for a given [Actor].
     */
    abstract fun principal(): Principal

    /**
     * Origin was an RPC call.
     */
    data class RPC(private val actor: Actor) : Origin() {

        override fun principal() = Principal { actor.id.value }
    }

    /**
     * Origin was a message sent by a [Peer].
     */
    data class Peer(val party: CordaX500Name) : Origin() {

        override fun principal() = Principal { party.toString() }
    }

    /**
     * Origin was a Corda Service.
     */
    data class Service(val serviceClassName: String, val owningLegalIdentity: CordaX500Name) : Origin() {

        override fun principal() = Principal { serviceClassName }
    }

    /**
     * Origin was a scheduled activity.
     */
    data class Scheduled(val scheduledState: ScheduledStateRef) : Origin() {

        override fun principal() = Principal { "Scheduler" }
    }

    // TODO When proper ssh access enabled, add username/use RPC?
    /**
     * Origin was the Shell.
     */
    object Shell : Origin() {

        override fun principal() = Principal { "Shell User" }
    }
}