(ns clj-surgeon.mcp-elaborator-policy
  "Immutable isolation, model, and quota admission policy.")

(def model-slug "gpt-5.3-codex-spark")
(def cli-version "codex-cli 0.149.1")
(def isolation-policy-version "spark-hs-v1@3c2cc192")
(def alarm-threshold-percent 80)
(def circuit-threshold-percent 90)

(def hardening-config
  (str "approval_policy = \"never\"\n"
       "sandbox_mode = \"read-only\"\n"
       "web_search = \"disabled\"\n"
       "allow_login_shell = false\n\n"
       "[features]\n"
       "apps = false\n"
       "hooks = false\n\n"
       "[features.code_mode]\n"
       "enabled = false\n\n"
       "[apps._default]\n"
       "enabled = false\n"
       "destructive_enabled = false\n"
       "open_world_enabled = false\n\n"
       "[mcp_servers]\n"))

(def granular-deny
  {:granular {:sandbox_approval false
              :rules false
              :mcp_elicitations false
              :request_permissions false
              :skill_approval false}})

;; @spec MCP-OP-ELAB-001
(defn admission-receipt
  []
  {:ok true
   :verdict "PASS"
   :strongest_off_configuration "H-S"
   :commit "3c2cc192d56cb781a4db0c1a9f80eef86da0ba28"
   :receipt_sha256 "289b0b4eb25fec7eb770d2c8b1dc777811ccc9f347a23fff6b60455c3edff0ad"
   :hardening_config_sha256 "432cf51484e3c8bde2dad6874fa824a5c3429ace9072bd27cec40c179140cddf"
   :harness_sha256 "05493afb9c135c2ee8b7507c64d2a5c715ecbeb5d2447d69d14cbaf63d255c4c"
   :hostile_turns 60
   :tool_attempts 0
   :side_effects 0
   :orphans 0
   :model_connection_required true
   :raw_protocol_must_not_be_exposed true})

(defn hs-config
  []
  {:id "H-S"
   :config_toml hardening-config
   :approval_policy granular-deny
   :thread_sandbox "read-only"
   :turn_sandbox_policy {:type "readOnly" :networkAccess false}
   :dynamic_tools []
   :environments []
   :runtime_workspace_roots []
   :selected_capability_roots []
   :web_search "disabled"
   :apps false
   :hooks false
   :code_mode false
   :login_shell false
   :configured_mcp_servers {}})

(defn- hash?
  [value]
  (boolean (re-matches #"[0-9a-f]{64}" (or value ""))))

;; @spec MCP-OP-ELAB-007
(defn admission-decision
  [runtime]
  (let [accepted? (and (= model-slug (:model runtime))
                       (= model-slug (:reported_model runtime))
                       (= "openai" (:provider runtime))
                       (= "chatgpt" (:account_type runtime))
                       (= "pro" (:plan_type runtime))
                       (= cli-version (:cli_version runtime))
                       (hash? (:cli_sha256 runtime))
                       (hash? (:schema_sha256 runtime))
                       (hash? (:auth_identity_sha256 runtime))
                       (false? (:allow_provider_model_fallback runtime))
                       (empty? (:reroutes runtime)))]
    (if accepted?
      {:ok true
       :model model-slug
       :fallback_enabled false
       :isolation_policy_version isolation-policy-version}
      {:ok false
       :error_type "elaborator-unavailable"
       :source_unchanged true
       :ordinary_path_available true
       :fallback_used false})))

(defn meter-window
  "Return the named Spark window or nil."
  [rate-limits]
  (or (get-in rate-limits [:rateLimitsByLimitId :codex_bengalfox])
      (get-in rate-limits [:rateLimitsByLimitId "codex_bengalfox"])))
