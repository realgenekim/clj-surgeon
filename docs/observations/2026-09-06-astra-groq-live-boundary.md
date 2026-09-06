# Astra: induced primary refusal to live Groq transport

Recorded 2026-09-06T08:26:57.350919+00:00. One preregistered attempt; passed, no retry.

An injected primary HTTP429 at the existing transport seam caused the unchanged transport client to load the runtime Groq key and send one actual request to the pinned direct Groq endpoint. There was no primary network request or primary credential read. Model `openai/gpt-oss-120b`, upstream Groq, finish_reason stop and exact requested sentinel all matched. The two dispatch records are explicitly labeled synthetic primary and actual fallback.

Transport wall0.4143s, external process wall0.5150s, parentexit0, no timeout, receipt exists. Groq reported84prompt tokens and175completion tokens, including159reasoning tokens. Cost is unknown; do not substitute zero. The synthetic429 has no provider usage and must not be counted as a paid request despite the generic dispatch field saying request_started. Exactly one paid-network dispatch occurred.

The wrapper froze client/wrapper hashes, exclusively reserved attempt-001, limited its shared deadline to30s and external process to35s, and allowed4096outputtokens per route. Keys were neither printed nor persisted. All source and endpoints remained unchanged.

This establishes induced-refusal/live-transport composition only. It does not establish a real primary outage, mission-level failover, candidate correctness or a speed advantage. Spark execution remains unsupported. Local retained preregistration, pins, parent status and receipt: `/var/tmp/forge/astra-groq-boundary-fx/`.
