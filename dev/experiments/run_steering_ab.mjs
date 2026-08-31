#!/usr/bin/env node

import {
  chmodSync,
  copyFileSync,
  existsSync,
  mkdirSync,
  mkdtempSync,
  readFileSync,
  readdirSync,
  rmSync,
  statSync,
  writeFileSync,
} from "node:fs";
import { createServer, request as httpRequest } from "node:http";
import { homedir, tmpdir } from "node:os";
import { dirname, join, relative, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { spawn, spawnSync } from "node:child_process";
import { createHash } from "node:crypto";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const repoRoot = resolve(scriptDir, "../..");
const resultRoot = join(repoRoot, "bench/results/2026-08-31-steering-ab");
const rawRoot = join(resultRoot, "raw");
const fixtureRoot = join(scriptDir, "fixtures/steering_ab");
const freeze = JSON.parse(readFileSync(join(resultRoot, "freeze.json"), "utf8"));
const sourceAuth = join(process.env.CODEX_HOME || join(homedir(), ".codex"), "auth.json");
const TURN_TIMEOUT_MS = 180_000;
const SIGNAL_THRESHOLD = freeze.signal.threshold_saved_tokens_estimate;
const apiKeyNames = [
  "OPENAI_API_KEY",
  "CODEX_API_KEY",
  "CODEX_ACCESS_TOKEN",
  "OPENAI_ORG_ID",
  "OPENAI_PROJECT_ID",
];

const edit1From = readFixture("edit1-from.clj");
const edit1To = readFixture("edit1-to.clj");
const edit2From = readFixture("edit2-from.clj");
const edit2To = readFixture("edit2-to.clj");

const schedules = {
  pilot: freeze.pilot,
  main: freeze.main_schedule,
  bonus: freeze.bonus_schedule,
};

function readFixture(name) {
  return readFileSync(join(fixtureRoot, name), "utf8").replace(/\n$/, "");
}

function sourceFor(first, second) {
  return `(ns bench.steering)\n\n${first}\n\n${second}\n`;
}

const sourceBefore = sourceFor(edit1From, edit2From);
const sourceAfterEdit1 = sourceFor(edit1To, edit2From);
const sourceFinal = sourceFor(edit1To, edit2To);

function sha256(value) {
  return createHash("sha256").update(value).digest("hex");
}

function encoded(value) {
  return Buffer.from(JSON.stringify(value));
}

function canonical(value) {
  if (Array.isArray(value)) return value.map(canonical);
  if (value && typeof value === "object") {
    return Object.fromEntries(
      Object.keys(value).sort().map((key) => [key, canonical(value[key])]),
    );
  }
  return value;
}

function canonicalBytes(value) {
  return Buffer.byteLength(JSON.stringify(canonical(value)), "utf8");
}

function round3(value) {
  return Math.round(value * 1000) / 1000;
}

function median(values) {
  if (values.length === 0) return null;
  const sorted = [...values].sort((a, b) => a - b);
  const middle = Math.floor(sorted.length / 2);
  return sorted.length % 2 ? sorted[middle] : (sorted[middle - 1] + sorted[middle]) / 2;
}

function sanitize(value, key = "") {
  if (/^(access_?token|refresh_?token|id_?token|authorization|email|client_?secret|secret)$/i.test(key)) {
    return "<redacted>";
  }
  if (Array.isArray(value)) return value.map((item) => sanitize(item));
  if (value && typeof value === "object") {
    return Object.fromEntries(
      Object.entries(value).map(([childKey, childValue]) => [
        childKey,
        sanitize(childValue, childKey),
      ]),
    );
  }
  if (typeof value === "string") {
    return value
      .replace(/Bearer\s+[A-Za-z0-9._~+/=-]+/gi, "Bearer <redacted>")
      .replace(/sk-[A-Za-z0-9_-]+/g, "sk-<redacted>");
  }
  return value;
}

function requireFact(condition, message) {
  if (!condition) throw new Error(message);
}

function writeJson(path, value) {
  writeFileSync(path, `${JSON.stringify(sanitize(value), null, 2)}\n`);
}

function ordinaryArguments(workspace, from = edit2From, to = edit2To) {
  return {
    workspace_root: workspace,
    edits: [{
      file: "src/bench/steering.clj",
      within: { form: "wall-policy" },
      from,
      to,
      matches: 1,
    }],
  };
}

function compactArguments(digest) {
  return {
    confirm: digest,
    fill: { "arguments.edits[0].to": edit2To },
  };
}

function edit1Arguments(workspace) {
  return {
    workspace_root: workspace,
    edits: [{
      file: "src/bench/steering.clj",
      within: { form: "seed-marker" },
      from: edit1From,
      to: edit1To,
      matches: 1,
    }],
  };
}

function inspectArguments(workspace) {
  return {
    workspace_root: workspace,
    requests: [{
      id: "wall-policy-descriptor",
      operation: "forms",
      file: "src/bench/steering.clj",
      forms: ["wall-policy"],
      expect: { forms: 1 },
    }],
    expect: { requests: 1, files: 1 },
  };
}

function prompt1(workspace) {
  return [
    "Complete EDIT 1 now. Use edit_clojure exactly once through the ordinary full from/to route shown below.",
    "Do not inspect first, do not use confirm/fill, do not use shell or native file editing, and do not narrate before the call.",
    `Exact edit_clojure arguments JSON: ${JSON.stringify(edit1Arguments(workspace))}`,
    "After the successful receipt, respond only DONE.",
  ].join("\n");
}

function prompt2(workspace) {
  return [
    "Complete EDIT 2 now. Choose the best advertised clj-surgeon route from the evidence already present in this same session.",
    "Do not use shell or native file editing. Make one exact mutation in src/bench/steering.clj within wall-policy, then respond only DONE.",
    "FROM (exact complete form):",
    edit2From,
    "TO (exact complete form):",
    edit2To,
    `Workspace: ${workspace}`,
  ].join("\n");
}

function parsePayload(buffer) {
  const text = buffer.toString("utf8");
  if (text.trimStart().startsWith("{")) {
    return { kind: "json", value: JSON.parse(text), text };
  }
  const lines = text.split("\n");
  const index = lines.findIndex((line) => line.startsWith("data: "));
  if (index >= 0) {
    return { kind: "sse", value: JSON.parse(lines[index].slice(6)), lines, index, text };
  }
  return null;
}

function encodePayload(parsed) {
  if (parsed.kind === "json") return encoded(parsed.value);
  const lines = [...parsed.lines];
  lines[parsed.index] = `data: ${JSON.stringify(parsed.value)}`;
  return Buffer.from(lines.join("\n"));
}

function structured(response) {
  return response?.result?.structuredContent || response?.result?.structured_content || {};
}

function requestUpstream(url, body, incomingHeaders = {}) {
  return new Promise((resolvePromise, rejectPromise) => {
    const headers = {
      "content-type": incomingHeaders["content-type"] || "application/json",
      accept: incomingHeaders.accept || "application/json, text/event-stream",
      "content-length": Buffer.byteLength(body),
    };
    if (incomingHeaders["mcp-session-id"]) {
      headers["mcp-session-id"] = incomingHeaders["mcp-session-id"];
    }
    const req = httpRequest(url, { method: "POST", headers }, (response) => {
      const chunks = [];
      response.on("data", (chunk) => chunks.push(chunk));
      response.on("end", () => resolvePromise({
        status: response.statusCode,
        headers: response.headers,
        body: Buffer.concat(chunks),
      }));
    });
    req.on("error", rejectPromise);
    req.end(body);
  });
}

async function startProxy({ arm, episode, upstreamUrl, workspace }) {
  const events = [];
  const state = {
    prepared: null,
    signalExposed: false,
    upstreamSessionHashes: new Set(),
  };
  const startNs = process.hrtime.bigint();
  const record = (kind, detail) => events.push({
    monotonic_ms: round3(Number(process.hrtime.bigint() - startNs) / 1_000_000),
    kind,
    ...sanitize(detail),
  });

  const server = createServer(async (request, response) => {
    try {
      const chunks = [];
      for await (const chunk of request) chunks.push(chunk);
      const body = Buffer.concat(chunks);
      const requestValue = body.length ? JSON.parse(body.toString("utf8")) : null;
      const session = request.headers["mcp-session-id"];
      if (session) state.upstreamSessionHashes.add(sha256(session));
      record("client-request", {
        method: requestValue?.method,
        id: requestValue?.id,
        params: requestValue?.params,
        session_sha256: session ? sha256(session) : null,
        body_sha256: sha256(body),
      });

      const upstream = await requestUpstream(upstreamUrl, body, request.headers);
      let emittedBody = upstream.body;
      const parsed = upstream.body.length ? parsePayload(upstream.body) : null;
      const isEdit1 = requestValue?.method === "tools/call"
        && requestValue?.params?.name === "edit_clojure"
        && requestValue?.params?.arguments?.edits?.[0]?.within?.form === "seed-marker";
      const editSucceeded = parsed && structured(parsed.value).ok === true
        && structured(parsed.value).committed === true;

      if (isEdit1 && editSucceeded && !state.prepared) {
        requireFact(session, "edit 1 response had no MCP session id on its request");
        const internalRequest = {
          jsonrpc: "2.0",
          id: `proxy-prepare-${episode}`,
          method: "tools/call",
          params: { name: "inspect_clojure", arguments: inspectArguments(workspace) },
        };
        const internalBody = encoded(internalRequest);
        const internal = await requestUpstream(upstreamUrl, internalBody, {
          "content-type": "application/json",
          accept: "application/json, text/event-stream",
          "mcp-session-id": session,
        });
        const internalParsed = parsePayload(internal.body);
        const internalStructured = structured(internalParsed?.value);
        const digest = internalStructured.prepared_confirmation?.descriptor_sha256;
        requireFact(/^[0-9a-f]{64}$/.test(digest || ""), "proxy preparation returned no digest");
        const typedBytes = canonicalBytes(ordinaryArguments(workspace));
        const confirmFillBytes = canonicalBytes(compactArguments(digest));
        const savedTokensEstimate = Math.floor((typedBytes - confirmFillBytes) / 4);
        state.prepared = {
          digest,
          typed_bytes: typedBytes,
          confirm_fill_bytes: confirmFillBytes,
          saved_tokens_estimate: savedTokensEstimate,
        };
        record("internal-prepare", {
          request: internalRequest,
          response_status: internal.status,
          response_sha256: sha256(internal.body),
          prepared_alternative: state.prepared,
        });

        if (arm === "S" && savedTokensEstimate >= SIGNAL_THRESHOLD) {
          parsed.value.result.structuredContent.prepared_alternative = state.prepared;
          emittedBody = encodePayload(parsed);
          state.signalExposed = true;
        }
      }

      const retainedHeaders = { ...upstream.headers };
      delete retainedHeaders["transfer-encoding"];
      delete retainedHeaders.connection;
      retainedHeaders["content-length"] = emittedBody.length;
      response.writeHead(upstream.status, retainedHeaders);
      response.end(emittedBody);
      record("client-response", {
        method: requestValue?.method,
        id: requestValue?.id,
        status: upstream.status,
        upstream_body_sha256: sha256(upstream.body),
        emitted_body_sha256: sha256(emittedBody),
        modified: !emittedBody.equals(upstream.body),
      });
    } catch (error) {
      record("proxy-error", { error: String(error), stack: error.stack });
      response.writeHead(500, { "content-type": "application/json" });
      response.end(JSON.stringify({ error: String(error) }));
    }
  });
  await new Promise((resolvePromise) => server.listen(0, "127.0.0.1", resolvePromise));
  const address = server.address();
  return {
    url: `http://127.0.0.1:${address.port}/mcp`,
    events,
    state,
    close: () => new Promise((resolvePromise) => server.close(resolvePromise)),
  };
}

class AppServer {
  constructor({ codexHome, workspace, transcript }) {
    this.transcript = transcript;
    this.pending = new Map();
    this.turnItems = new Map();
    this.agentMessages = new Map();
    this.turnCompletions = new Map();
    this.waiters = new Map();
    this.reroutes = [];
    this.nextId = 1;
    this.buffer = "";
    this.stderr = "";
    this.startNs = process.hrtime.bigint();
    const env = { ...process.env, CODEX_HOME: codexHome };
    for (const key of apiKeyNames) delete env[key];
    this.child = spawn("codex", ["app-server", "--listen", "stdio://"], {
      cwd: workspace,
      env,
      stdio: ["pipe", "pipe", "pipe"],
    });
    this.child.stdout.setEncoding("utf8");
    this.child.stdout.on("data", (chunk) => this.onData(chunk));
    this.child.stderr.setEncoding("utf8");
    this.child.stderr.on("data", (chunk) => { this.stderr += chunk; });
    this.child.on("error", (error) => {
      for (const pending of this.pending.values()) pending.reject(error);
    });
  }

  record(direction, message) {
    this.transcript.push({
      monotonic_ms: round3(Number(process.hrtime.bigint() - this.startNs) / 1_000_000),
      direction,
      message: sanitize(message),
    });
  }

  onData(chunk) {
    this.buffer += chunk;
    while (this.buffer.includes("\n")) {
      const newline = this.buffer.indexOf("\n");
      const line = this.buffer.slice(0, newline).trim();
      this.buffer = this.buffer.slice(newline + 1);
      if (!line) continue;
      try { this.onMessage(JSON.parse(line)); }
      catch (error) { this.record("decode-error", { line, error: String(error) }); }
    }
  }

  onMessage(message) {
    this.record("receive", message);
    if (Object.hasOwn(message, "id") && this.pending.has(message.id)) {
      const pending = this.pending.get(message.id);
      this.pending.delete(message.id);
      if (message.error) pending.reject(new Error(JSON.stringify(message.error)));
      else pending.resolve(message.result);
      return;
    }
    const params = message.params || {};
    const turnId = params.turnId || params.turn?.id;
    if (message.method === "item/completed" && turnId) {
      const item = params.item || {};
      const items = this.turnItems.get(turnId) || [];
      items.push(item);
      this.turnItems.set(turnId, items);
      if (item.type === "agentMessage") {
        const texts = this.agentMessages.get(turnId) || [];
        texts.push(item.text || "");
        this.agentMessages.set(turnId, texts);
      }
    }
    if (message.method === "model/rerouted") this.reroutes.push(params);
    if (message.method === "turn/completed" && params.turn?.id) {
      this.turnCompletions.set(params.turn.id, {
        turn: params.turn,
        receivedNs: process.hrtime.bigint(),
      });
      const waiter = this.waiters.get(params.turn.id);
      if (waiter) {
        this.waiters.delete(params.turn.id);
        clearTimeout(waiter.timer);
        waiter.resolve(this.turnCompletions.get(params.turn.id));
      }
    }
  }

  request(method, params) {
    const id = this.nextId++;
    const message = { method, id, params };
    this.record("send", message);
    return new Promise((resolvePromise, rejectPromise) => {
      this.pending.set(id, { resolve: resolvePromise, reject: rejectPromise });
      this.child.stdin.write(`${JSON.stringify(message)}\n`);
    });
  }

  notify(method, params) {
    const message = params === undefined ? { method } : { method, params };
    this.record("send", message);
    this.child.stdin.write(`${JSON.stringify(message)}\n`);
  }

  waitTurn(turnId) {
    if (this.turnCompletions.has(turnId)) return Promise.resolve(this.turnCompletions.get(turnId));
    return new Promise((resolvePromise, rejectPromise) => {
      const timer = setTimeout(() => {
        this.waiters.delete(turnId);
        rejectPromise(new Error(`turn ${turnId} timed out after ${TURN_TIMEOUT_MS}ms`));
      }, TURN_TIMEOUT_MS);
      this.waiters.set(turnId, { resolve: resolvePromise, reject: rejectPromise, timer });
    });
  }

  async runTurn(threadId, text, model, effort) {
    const sentNs = process.hrtime.bigint();
    const started = await this.request("turn/start", {
      threadId,
      input: [{ type: "text", text, text_elements: [] }],
      model,
      effort,
    });
    const completion = await this.waitTurn(started.turn.id);
    return {
      id: started.turn.id,
      status: completion.turn.status,
      wall_ms: round3(Number(completion.receivedNs - sentNs) / 1_000_000),
      items: this.turnItems.get(started.turn.id) || [],
      agent_messages: this.agentMessages.get(started.turn.id) || [],
    };
  }

  async close() {
    this.child.stdin.end();
    const exit = await new Promise((resolvePromise) => {
      const timer = setTimeout(() => resolvePromise(null), 5000);
      this.child.once("exit", (code, signal) => {
        clearTimeout(timer);
        resolvePromise({ code, signal });
      });
    });
    if (!exit) this.child.kill("SIGTERM");
    return exit || { code: null, signal: "SIGTERM" };
  }
}

function toolCalls(turn) {
  return turn.items.filter((item) => item.type === "mcpToolCall" && item.server === "clj-surgeon");
}

function normalizeArguments(value) {
  if (typeof value === "string") {
    try { return JSON.parse(value); } catch { return value; }
  }
  return value;
}

function inspectRequestsWallPolicy(args) {
  if (!args || typeof args !== "object") return false;
  return (args.requests || []).some((request) =>
    request.file === "src/bench/steering.clj"
    && Array.isArray(request.forms)
    && request.forms.length === 1
    && request.forms[0] === "wall-policy");
}

function successfulMutation(call) {
  const receipt = call?.result?.structuredContent || call?.result?.structured_content || {};
  return call.tool === "edit_clojure"
    && call.status === "completed"
    && receipt.ok === true
    && receipt.committed === true;
}

function scoreEpisode({ id, phase, arm, model, effort, workspace, sourcePath, proxy, threadStart, turn1, turn2, app }) {
  const edit1Calls = toolCalls(turn1);
  const edit2Calls = toolCalls(turn2);
  const mutationIndex = edit2Calls.findIndex(successfulMutation);
  const throughMutation = mutationIndex >= 0 ? edit2Calls.slice(0, mutationIndex + 1) : edit2Calls;
  const normalized = throughMutation.map((call) => ({ ...call, arguments: normalizeArguments(call.arguments) }));
  const usedConfirmFill = normalized.some((call) => call.tool === "edit_clojure"
    && call.arguments && typeof call.arguments === "object"
    && typeof call.arguments.confirm === "string"
    && call.arguments.fill && typeof call.arguments.fill === "object");
  const requestedDescriptor = normalized.some((call) =>
    call.tool === "inspect_clojure" && inspectRequestsWallPolicy(call.arguments));
  const ordinaryMutation = normalized.some((call) => call.tool === "edit_clojure"
    && call.arguments && typeof call.arguments === "object"
    && Array.isArray(call.arguments.edits)
    && !Object.hasOwn(call.arguments, "confirm"));
  const route = usedConfirmFill
    ? "confirm-fill"
    : requestedDescriptor ? "descriptor-request" : ordinaryMutation ? "ordinary-full" : "other";
  const emissionBytes = normalized.reduce((sum, call) => sum + canonicalBytes(call.arguments), 0);
  const finalSource = readFileSync(sourcePath, "utf8");
  const exact = sha256(finalSource) === freeze.fixture.source_final_sha256;
  const explicitWrongSubject = normalized.some((call) => {
    if (call.tool !== "edit_clojure" || !call.arguments || typeof call.arguments !== "object") return false;
    if (!Array.isArray(call.arguments.edits)) return false;
    return call.arguments.edits.some((edit) =>
      edit.file !== "src/bench/steering.clj" || edit.within?.form !== "wall-policy");
  });
  const signalMentions = turn2.agent_messages.filter((text) =>
    /prepared_alternative|saved_tokens_estimate|confirm_fill_bytes|typed_bytes|receipt signal/i.test(text));
  const edit1Ordinary = edit1Calls.length === 1
    && edit1Calls[0].tool === "edit_clojure"
    && Array.isArray(normalizeArguments(edit1Calls[0].arguments)?.edits)
    && !Object.hasOwn(normalizeArguments(edit1Calls[0].arguments), "confirm");

  return {
    schema: "clj-surgeon.steering-ab-cell.v1",
    id,
    phase,
    arm,
    model: {
      requested: model,
      reasoning: effort,
      reported: threadStart.model,
      provider: threadStart.modelProvider,
      fallback_allowed: false,
      reroutes: app.reroutes,
    },
    session: {
      thread_id: threadStart.thread.id,
      same_thread_two_turns: true,
      upstream_mcp_session_sha256: [...proxy.state.upstreamSessionHashes],
      one_upstream_session: proxy.state.upstreamSessionHashes.size === 1,
    },
    signal: {
      exposed: proxy.state.signalExposed,
      prepared_alternative: proxy.state.prepared,
      threshold: SIGNAL_THRESHOLD,
    },
    edit1: {
      ordinary_full_from_to: edit1Ordinary,
      exact_after_receipt: true,
      turn_status: turn1.status,
      wall_ms: turn1.wall_ms,
    },
    edit2: {
      route,
      primary_qualifies: usedConfirmFill || requestedDescriptor,
      used_confirm_fill: usedConfirmFill,
      requested_descriptor: requestedDescriptor,
      caller_emission_bytes: emissionBytes,
      wall_ms: turn2.wall_ms,
      exact,
      final_sha256: sha256(finalSource),
      expected_sha256: freeze.fixture.source_final_sha256,
      wrong_subject: explicitWrongSubject || !exact,
      tool_calls: normalized.map((call) => ({
        tool: call.tool,
        status: call.status,
        arguments: call.arguments,
        successful_mutation: successfulMutation(call),
      })),
    },
    qualitative_signal_mentions: signalMentions,
    final_messages: turn2.agent_messages,
    app_server_stderr: app.stderr.trim(),
  };
}

async function waitForFile(path, process, timeoutMs = 60_000) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    if (existsSync(path) && statSync(path).size > 0) return;
    if (process.exitCode !== null) throw new Error(`process exited before readiness: ${process.exitCode}`);
    await new Promise((resolvePromise) => setTimeout(resolvePromise, 100));
  }
  throw new Error(`timed out waiting for ${path}`);
}

async function startMcpServer() {
  const serverDir = join(rawRoot, "server");
  mkdirSync(serverDir, { recursive: true });
  const readyFile = join(serverDir, "ready.edn");
  requireFact(!existsSync(readyFile), `server readiness file already exists: ${readyFile}`);
  const stdout = [];
  const stderr = [];
  const args = [
    "-J-Xms64m", "-J-Xmx512m", "-X:clj-surgeon/mcp",
    ":project-dir", JSON.stringify(repoRoot),
    ":nrepl-port", ":none",
    ":port", "0",
    ":ready-file", JSON.stringify(readyFile),
    ":receipt-dir", JSON.stringify(join(serverDir, "receipts")),
    ":telemetry", ":metrics",
    ":telemetry-dir", JSON.stringify(join(serverDir, "telemetry")),
    ":run-id", JSON.stringify("steering-ab-20260831"),
  ];
  const child = spawn("clojure", args, { cwd: repoRoot, stdio: ["ignore", "pipe", "pipe"] });
  child.stdout.setEncoding("utf8");
  child.stdout.on("data", (chunk) => stdout.push(chunk));
  child.stderr.setEncoding("utf8");
  child.stderr.on("data", (chunk) => stderr.push(chunk));
  await waitForFile(readyFile, child);
  const ready = readFileSync(readyFile, "utf8");
  const match = ready.match(/:url\s+"([^"]+)"/);
  requireFact(match, `could not parse MCP URL from ${ready}`);
  return {
    url: match[1],
    child,
    stop: async () => {
      child.kill("SIGTERM");
      await new Promise((resolvePromise) => {
        const timer = setTimeout(() => { child.kill("SIGKILL"); resolvePromise(); }, 5000);
        child.once("exit", () => { clearTimeout(timer); resolvePromise(); });
      });
      writeFileSync(join(serverDir, "stdout.log"), stdout.join(""));
      writeFileSync(join(serverDir, "stderr.log"), stderr.join(""));
    },
  };
}

function makeCodexHome(proxyUrl) {
  const isolatedRoot = mkdtempSync(join(tmpdir(), "steering-ab-codex."));
  const codexHome = join(isolatedRoot, "codex-home");
  mkdirSync(codexHome, { mode: 0o700 });
  copyFileSync(sourceAuth, join(codexHome, "auth.json"));
  chmodSync(join(codexHome, "auth.json"), 0o600);
  writeFileSync(join(codexHome, "config.toml"), [
    "[mcp_servers.clj-surgeon]",
    `url = ${JSON.stringify(proxyUrl)}`,
    "required = true",
    'enabled_tools = ["inspect_clojure", "edit_clojure"]',
    'default_tools_approval_mode = "approve"',
    "startup_timeout_sec = 10",
    "tool_timeout_sec = 60",
    "",
  ].join("\n"), { mode: 0o600 });
  return { isolatedRoot, codexHome };
}

async function runEpisode({ entry, phase, cellIndex, mcpUrl }) {
  const cell = `cell-${String(cellIndex).padStart(3, "0")}`;
  const cellDir = join(rawRoot, cell);
  requireFact(!existsSync(cellDir), `refusing to overwrite cell: ${cellDir}`);
  const workspace = join(cellDir, "workspace");
  const sourcePath = join(workspace, "src/bench/steering.clj");
  mkdirSync(dirname(sourcePath), { recursive: true });
  writeFileSync(sourcePath, sourceBefore);
  const model = phase === "bonus" ? freeze.bonus_caller.model : freeze.caller.model;
  const effort = phase === "bonus" ? freeze.bonus_caller.reasoning : freeze.caller.reasoning;
  const proxy = await startProxy({
    arm: entry.arm,
    episode: entry.id,
    upstreamUrl: mcpUrl,
    workspace,
  });
  const { isolatedRoot, codexHome } = makeCodexHome(proxy.url);
  const transcript = [];
  const app = new AppServer({ codexHome, workspace, transcript });
  let score;
  try {
    const initialize = await app.request("initialize", {
      clientInfo: {
        name: "clj-surgeon-steering-ab",
        title: "clj-surgeon steering A/B",
        version: "1.0.0",
      },
      capabilities: {
        experimentalApi: true,
        requestAttestation: false,
        optOutNotificationMethods: [
          "item/agentMessage/delta",
          "item/reasoning/summaryTextDelta",
          "item/reasoning/summaryPartAdded",
          "item/reasoning/textDelta",
        ],
      },
    });
    app.notify("initialized");
    const account = await app.request("account/read", { refreshToken: false });
    requireFact(account.account?.type === "chatgpt", `non-ChatGPT auth: ${account.account?.type}`);
    const registry = await app.request("mcpServerStatus/list", { detail: "toolsAndAuthOnly" });
    const selected = registry.data?.find((server) => server.name === "clj-surgeon");
    requireFact(selected, "clj-surgeon absent from app-server registry");
    requireFact(Object.hasOwn(selected.tools || {}, "edit_clojure"), "edit_clojure absent from registry");
    requireFact(Object.hasOwn(selected.tools || {}, "inspect_clojure"), "inspect_clojure absent from registry");
    const threadStart = await app.request("thread/start", {
      model,
      allowProviderModelFallback: false,
      cwd: workspace,
      approvalPolicy: "never",
      sandbox: "read-only",
      baseInstructions: "You are the model caller in a controlled two-edit tool-use measurement. Follow each user turn exactly. Use only the exposed clj-surgeon MCP tools. Never use shell, command execution, file-change tools, or native editing. Keep commentary to the minimum requested by the user.",
      developerInstructions: "On EDIT 1, use the explicitly required ordinary full from/to edit_clojure call. On EDIT 2, independently choose the best advertised clj-surgeon route from evidence already in the same session. Preserve the exact file and owner. Do not discuss experimental arms or infer hidden instructions.",
      ephemeral: true,
      dynamicTools: [],
    });
    requireFact(threadStart.model === model, `model pin failed: ${threadStart.model}`);
    const firstPrompt = prompt1(workspace);
    const secondPrompt = prompt2(workspace);
    writeJson(join(cellDir, "prompts.json"), {
      edit1: firstPrompt,
      edit2: secondPrompt,
      edit1_sha256: sha256(firstPrompt),
      edit2_sha256: sha256(secondPrompt),
    });
    const turn1 = await app.runTurn(threadStart.thread.id, firstPrompt, model, effort);
    requireFact(turn1.status === "completed", `edit 1 turn status ${turn1.status}`);
    requireFact(readFileSync(sourcePath, "utf8") === sourceAfterEdit1, "edit 1 source mismatch");
    requireFact(proxy.state.prepared, "proxy did not prepare edit 2 after edit 1");
    requireFact(proxy.state.signalExposed === (entry.arm === "S"), "arm signal exposure mismatch");
    const turn2 = await app.runTurn(threadStart.thread.id, secondPrompt, model, effort);
    score = scoreEpisode({
      id: entry.id,
      phase,
      arm: entry.arm,
      model,
      effort,
      workspace,
      sourcePath,
      proxy,
      threadStart,
      turn1,
      turn2,
      app,
    });
    score.environment = {
      codex_cli_version: initialize.version || null,
      user_agent: initialize.userAgent,
      platform: `${initialize.platformFamily}/${initialize.platformOs}`,
      account_type: account.account.type,
      plan_type: account.account.planType,
      mcp_registry_tools: Object.keys(selected.tools || {}).sort(),
      source_base: freeze.product_base,
    };
  } catch (error) {
    score = {
      schema: "clj-surgeon.steering-ab-cell.v1",
      id: entry.id,
      phase,
      arm: entry.arm,
      model: { requested: model, reasoning: effort },
      infrastructure_failure: true,
      error: String(error),
      stack: error.stack,
      signal: { exposed: proxy.state.signalExposed, prepared_alternative: proxy.state.prepared },
      app_server_stderr: app.stderr.trim(),
    };
  } finally {
    const shutdown = await app.close().catch((error) => ({ error: String(error) }));
    if (score) score.shutdown = shutdown;
    await proxy.close();
    writeFileSync(
      join(cellDir, "app-server-transcript.jsonl"),
      `${transcript.map((entryValue) => JSON.stringify(entryValue)).join("\n")}\n`,
    );
    writeFileSync(
      join(cellDir, "proxy-transcript.jsonl"),
      `${proxy.events.map((entryValue) => JSON.stringify(entryValue)).join("\n")}\n`,
    );
    writeFileSync(join(cellDir, "source-final.clj"), readFileSync(sourcePath, "utf8"));
    writeJson(join(cellDir, "score.json"), score);
    rmSync(isolatedRoot, { recursive: true, force: true });
  }
  return score;
}

function aggregateScores(pilot, main, bonus, stoppedAfterPilot) {
  const byArm = Object.fromEntries(["C", "S"].map((arm) => {
    const cells = main.filter((cell) => cell.arm === arm && !cell.infrastructure_failure);
    return [arm, {
      n: cells.length,
      primary_qualifying: cells.filter((cell) => cell.edit2.primary_qualifies).length,
      primary_rate: cells.length ? cells.filter((cell) => cell.edit2.primary_qualifies).length / cells.length : null,
      exact: cells.filter((cell) => cell.edit2.exact).length,
      wrong_subject: cells.filter((cell) => cell.edit2.wrong_subject).length,
      median_emission_bytes: median(cells.map((cell) => cell.edit2.caller_emission_bytes)),
      median_wall_ms: median(cells.map((cell) => cell.edit2.wall_ms)),
      routes: cells.map((cell) => ({ id: cell.id, route: cell.edit2.route })),
    }];
  }));
  const lift = byArm.S.primary_rate === null || byArm.C.primary_rate === null
    ? null
    : round3((byArm.S.primary_rate - byArm.C.primary_rate) * 100);
  const exactnessGate = byArm.S.n === 6 && byArm.C.n === 6
    && byArm.S.exact === 6 && byArm.C.exact === 6;
  const wrongSubjectGate = byArm.S.wrong_subject + byArm.C.wrong_subject === 0;
  const primaryActionable = !stoppedAfterPilot && exactnessGate && wrongSubjectGate;
  return {
    schema: "clj-surgeon.steering-ab-aggregate.v1",
    preregistration_commit: "b36999d4e4029088a2d3ea00a835fc6dd21dfb4e",
    product_base: freeze.product_base,
    pilot: {
      cells: pilot.map((cell) => ({
        id: cell.id,
        qualifies: cell.edit2?.primary_qualifies ?? null,
        route: cell.edit2?.route ?? null,
        infrastructure_failure: cell.infrastructure_failure || false,
      })),
      control_qualifying: pilot.filter((cell) => cell.edit2?.primary_qualifies).length,
      broken_fixture: stoppedAfterPilot,
    },
    main: {
      by_arm: byArm,
      steering_lift_percentage_points: lift,
      exactness_gate: exactnessGate,
      wrong_subject_gate: wrongSubjectGate,
      primary_actionable: primaryActionable,
      kill_rule_triggered: primaryActionable ? lift < 25 : null,
    },
    bonus: bonus.map((cell) => ({
      id: cell.id,
      arm: cell.arm,
      route: cell.edit2?.route ?? null,
      qualifies: cell.edit2?.primary_qualifies ?? null,
      exact: cell.edit2?.exact ?? null,
      emission_bytes: cell.edit2?.caller_emission_bytes ?? null,
      wall_ms: cell.edit2?.wall_ms ?? null,
    })),
    qualitative_signal_mentions: [...pilot, ...main, ...bonus].flatMap((cell) =>
      (cell.qualitative_signal_mentions || []).map((text) => ({ id: cell.id, arm: cell.arm, text }))),
  };
}

function renderReport(aggregate) {
  if (aggregate.pilot.broken_fixture) {
    return `# Receipt steering A/B result\n\nVerdict: fixture broken by the preregistered sub-ceiling rule; both control pilot cells independently selected the prepared route. The confirmatory cohort did not run.\n`;
  }
  const control = aggregate.main.by_arm.C;
  const signal = aggregate.main.by_arm.S;
  const gate = aggregate.main.primary_actionable ? "passed" : "failed";
  const kill = aggregate.main.kill_rule_triggered === null
    ? "not evaluated"
    : aggregate.main.kill_rule_triggered ? "triggered" : "not triggered";
  return [
    "# Receipt steering A/B result",
    "",
    `Primary: S ${signal.primary_qualifying}/${signal.n}; C ${control.primary_qualifying}/${control.n}; lift ${aggregate.main.steering_lift_percentage_points} percentage points.`,
    `Safety gates: ${gate}. Exactness S ${signal.exact}/${signal.n}, C ${control.exact}/${control.n}; wrong-subject ${signal.wrong_subject + control.wrong_subject}.`,
    `Preregistered <25pp kill: ${kill}.`,
    "",
    `Median edit-2 caller emission: S ${signal.median_emission_bytes} bytes; C ${control.median_emission_bytes} bytes.`,
    `Median edit-2 wall: S ${signal.median_wall_ms} ms; C ${control.median_wall_ms} ms.`,
    "",
    `Sol routes S: ${signal.routes.map((row) => `${row.id}=${row.route}`).join(", ")}.`,
    `Sol routes C: ${control.routes.map((row) => `${row.id}=${row.route}`).join(", ")}.`,
    "",
    `Spark bonus: ${aggregate.bonus.map((row) => `${row.id}=${row.route}`).join(", ") || "not run"}.`,
    "",
    `Qualitative signal mentions: ${aggregate.qualitative_signal_mentions.length}.`,
    "",
  ].join("\n");
}

function artifactFiles(root) {
  const found = [];
  for (const name of readdirSync(root)) {
    if (name === "artifact-manifest.sha256") continue;
    const path = join(root, name);
    if (statSync(path).isDirectory()) found.push(...artifactFiles(path));
    else found.push(path);
  }
  return found.sort();
}

function writeManifest() {
  const lines = artifactFiles(resultRoot).map((path) =>
    `${sha256(readFileSync(path))}  ${relative(resultRoot, path)}`);
  writeFileSync(join(resultRoot, "artifact-manifest.sha256"), `${lines.join("\n")}\n`);
}

function selfTest() {
  const expected = freeze.fixture;
  requireFact(sha256(`${edit1From}\n`) === expected.edit1_from.sha256, "edit1-from hash drift");
  requireFact(sha256(`${edit1To}\n`) === expected.edit1_to.sha256, "edit1-to hash drift");
  requireFact(sha256(`${edit2From}\n`) === expected.edit2_from.sha256, "edit2-from hash drift");
  requireFact(sha256(`${edit2To}\n`) === expected.edit2_to.sha256, "edit2-to hash drift");
  requireFact(sha256(sourceBefore) === expected.source_before_sha256, "source-before hash drift");
  requireFact(sha256(sourceAfterEdit1) === expected.source_after_edit1_sha256, "source-after-edit1 hash drift");
  requireFact(sha256(sourceFinal) === expected.source_final_sha256, "source-final hash drift");
  const workspace = join(rawRoot, "cell-000/workspace");
  const typedBytes = canonicalBytes(ordinaryArguments(workspace));
  const compactBytes = canonicalBytes(compactArguments("a".repeat(64)));
  requireFact(typedBytes === freeze.signal.typed_bytes, `typed byte drift: ${typedBytes}`);
  requireFact(compactBytes === freeze.signal.confirm_fill_bytes, `compact byte drift: ${compactBytes}`);
  requireFact(Math.floor((typedBytes - compactBytes) / 4) === freeze.signal.saved_tokens_estimate,
    "saved-token estimate drift");
  requireFact(schedules.main.filter((row) => row.arm === "S").length === 6, "S schedule drift");
  requireFact(schedules.main.filter((row) => row.arm === "C").length === 6, "C schedule drift");
  return { ok: true, typed_bytes: typedBytes, confirm_fill_bytes: compactBytes };
}

async function proxySelfTest() {
  const digest = "b".repeat(64);
  let internalPrepares = 0;
  const upstreamBodies = [];
  const upstream = createServer(async (request, response) => {
    const chunks = [];
    for await (const chunk of request) chunks.push(chunk);
    const body = Buffer.concat(chunks);
    const value = JSON.parse(body.toString("utf8"));
    if (value.method === "initialize") {
      response.writeHead(200, {
        "content-type": "application/json",
        "mcp-session-id": "proxy-self-test-session",
      });
      response.end(JSON.stringify({ jsonrpc: "2.0", id: value.id, result: { serverInfo: { name: "clj-surgeon" } } }));
      return;
    }
    if (value.id && String(value.id).startsWith("proxy-prepare-")) {
      internalPrepares += 1;
      response.writeHead(200, { "content-type": "application/json" });
      response.end(JSON.stringify({
        jsonrpc: "2.0",
        id: value.id,
        result: {
          content: [{ type: "text", text: "prepared" }],
          structuredContent: {
            ok: true,
            prepared_confirmation: { descriptor_sha256: digest },
          },
        },
      }));
      return;
    }
    const ordinary = Buffer.from(JSON.stringify({
      jsonrpc: "2.0",
      id: value.id,
      result: {
        content: [{ type: "text", text: "ordinary product receipt" }],
        structuredContent: { ok: true, committed: true, verification_complete: true },
      },
    }));
    upstreamBodies.push(ordinary);
    response.writeHead(200, { "content-type": "application/json" });
    response.end(ordinary);
  });
  await new Promise((resolvePromise) => upstream.listen(0, "127.0.0.1", resolvePromise));
  const upstreamUrl = `http://127.0.0.1:${upstream.address().port}/mcp`;
  const workspace = join(rawRoot, "cell-000/workspace");
  try {
    for (const arm of ["C", "S"]) {
      const proxy = await startProxy({ arm, episode: `self-${arm}`, upstreamUrl, workspace });
      try {
        const init = await requestUpstream(proxy.url, encoded({
          jsonrpc: "2.0", id: 1, method: "initialize", params: {},
        }));
        const session = init.headers["mcp-session-id"];
        const edit = await requestUpstream(proxy.url, encoded({
          jsonrpc: "2.0",
          id: 2,
          method: "tools/call",
          params: { name: "edit_clojure", arguments: edit1Arguments(workspace) },
        }), {
          "mcp-session-id": session,
          "content-type": "application/json",
          accept: "application/json, text/event-stream",
        });
        const receipt = structured(parsePayload(edit.body).value);
        requireFact((arm === "S") === Object.hasOwn(receipt, "prepared_alternative"),
          `proxy self-test signal mismatch for ${arm}`);
        requireFact(receipt.prepared_alternative?.saved_tokens_estimate === 444 || arm === "C",
          "proxy self-test signal arithmetic drift");
        requireFact(proxy.state.signalExposed === (arm === "S"),
          `proxy state mismatch for ${arm}`);
        const responseEvent = proxy.events.findLast((event) =>
          event.kind === "client-response" && event.method === "tools/call");
        requireFact(responseEvent.modified === (arm === "S"),
          `control receipt was not byte-transparent for ${arm}`);
      } finally {
        await proxy.close();
      }
    }
    requireFact(internalPrepares === 2, `expected two internal prepares, got ${internalPrepares}`);
    return { ok: true, internal_prepares: internalPrepares, upstream_receipts: upstreamBodies.length };
  } finally {
    await new Promise((resolvePromise) => upstream.close(resolvePromise));
  }
}

async function main() {
  const selfTestOnly = process.argv.includes("--self-test");
  const self = selfTest();
  if (selfTestOnly) {
    const proxy = await proxySelfTest();
    process.stdout.write(`${JSON.stringify({ ...self, proxy })}\n`);
    return;
  }
  requireFact(existsSync(sourceAuth), `subscription auth file not found: ${sourceAuth}`);
  const login = spawnSync("codex", ["login", "status"], { encoding: "utf8" });
  requireFact(login.status === 0 && /Logged in using ChatGPT/.test(`${login.stdout}${login.stderr}`),
    "refusing to run without ChatGPT subscription authentication");
  requireFact(!existsSync(rawRoot), `refusing to overwrite existing raw result directory: ${rawRoot}`);
  mkdirSync(rawRoot, { recursive: true });
  const mcp = await startMcpServer();
  const pilot = [];
  const mainScores = [];
  const bonus = [];
  let cellIndex = 0;
  let stoppedAfterPilot = false;
  try {
    for (const entry of schedules.pilot) {
      const score = await runEpisode({ entry, phase: "pilot", cellIndex, mcpUrl: mcp.url });
      pilot.push(score);
      process.stdout.write(`${entry.id} ${entry.arm} ${score.edit2?.route || score.error}\n`);
      cellIndex += 1;
    }
    requireFact(pilot.every((cell) => !cell.infrastructure_failure), "pilot infrastructure failure; retain and rerun per preregistration");
    stoppedAfterPilot = pilot.every((cell) => cell.edit2.primary_qualifies);
    if (!stoppedAfterPilot) {
      for (const entry of schedules.main) {
        const score = await runEpisode({ entry, phase: "main", cellIndex, mcpUrl: mcp.url });
        mainScores.push(score);
        process.stdout.write(`${entry.id} ${entry.arm} ${score.edit2?.route || score.error}\n`);
        cellIndex += 1;
      }
      for (const entry of schedules.bonus) {
        const score = await runEpisode({ entry, phase: "bonus", cellIndex, mcpUrl: mcp.url });
        bonus.push(score);
        process.stdout.write(`${entry.id} ${entry.arm} ${score.edit2?.route || score.error}\n`);
        cellIndex += 1;
      }
    }
  } finally {
    await mcp.stop();
  }
  const aggregate = aggregateScores(pilot, mainScores, bonus, stoppedAfterPilot);
  writeJson(join(resultRoot, "aggregate.json"), aggregate);
  writeFileSync(join(resultRoot, "report.md"), renderReport(aggregate));
  writeManifest();
  process.stdout.write(`${JSON.stringify(aggregate.main, null, 2)}\n`);
}

await main();
