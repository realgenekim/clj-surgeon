(ns cfp-scheduler-killer.seed
  "The seed form installed on every new event — the EAIS Charlotte field defs,
   transcribed from docs/design/form-builder.md §'The EAIS Charlotte seed form'.

   A form is a VECTOR OF FIELD DEFINITIONS, as data (EDN here, JSONB in
   Postgres). One definition drives public rendering, malli validation, board
   columns, exports and 'copy from last year'.

   Design rules that this data encodes (form-builder.md):
     1. Field IDs are FOREVER. Labels rename freely; never re-key.
     2. `:locked true`  — core field, cannot be deleted by the builder UI.
     3. `:private true` — PC-only; never shown on the public page or exports.
     4. `:required` is a per-field toggle, not a type.
     5. Submissions snapshot these defs, so mid-CFP edits can't corrupt old rows.")

(def field-types
  "The field types this slice's renderer/validator knows about."
  #{:text :markdown :textarea :select :url :email :file :group})

(def eais-charlotte-form
  "The seed form: 11 session fields + the repeatable speaker block."
  [{:id :talk-title
    :type :text
    :label "Talk title"
    :placeholder "How 400 underwriters at a 150-year-old insurer ship with AI every day"
    :required true
    :locked true
    :max-length 255}

   {:id :abstract
    :type :markdown
    :label "Abstract"
    :help "Published as-is if your talk is accepted."
    :placeholder "In 18 months we went from a Copilot pilot to agents opening 40% of our PRs. This talk walks through the three failed rollouts before the one that stuck: what we measured, the $4.2M we banked, and the operating-model changes that made it durable."
    :required true
    :locked true
    :max-length 5000}

   {:id :session-format
    :type :select
    ;; :widget is a RENDERING hint, never a type — the value is still one of
    ;; :options, so switching radio<->dropdown never changes what gets stored.
    :widget :radio
    :label "Session format"
    :required true
    :options ["Experience Report" "SME talk" "Panel"]}

   {:id :track
    :type :select
    :label "Track"
    :help "Where your talk lives in the program — the committee filters by it, and it becomes the schedule's lanes."
    :required true
    :options ["Leadership & Organizational Change" "Developer Practices"
              "Individual Productivity" "AI Models"
              "Infrastructure & Operations" "Architecture"]}

   {:id :org-size
    :type :select
    :label "Organization size"
    :help "Helps the committee balance the program across company sizes."
    :required true
    :options ["<1,000" "1,000–10,000" ">10,000"]}

   {:id :industry
    :type :select
    :label "Industry"
    :required true
    :options ["Financial Services" "Insurance" "Healthcare" "Retail"
              "Manufacturing" "Technology" "Telecom" "Government"
              "Energy" "Transportation & Logistics" "Other"]}

   {:id :ai-transformation-history
    :type :textarea
    :label "When did your organization begin its AI transformation, and what is deployed in production today?"
    :placeholder "Copilot pilots in 2024; today 40% of PRs are agent-assisted and claims triage runs on a fine-tuned model in production."
    :required true
    :max-length 2000}

   {:id :measurable-outcomes
    :type :textarea
    :label "What measurable outcomes can you share? (adoption %, cycle time, cost, revenue)"
    :help "Specific numbers beat adjectives."
    :placeholder "Deploy frequency 2×, claims cycle time down 35%, $4.2M annualized savings."
    :required true
    :max-length 2000}

   {:id :advice-to-peer
    :type :textarea
    :label "What would you tell a peer CTO to do differently?"
    :placeholder "Start with the workflow, not the model — we lost six months tuning a model nobody's process could absorb."
    :required false
    :max-length 2000}

   {:id :business-co-presenter
    :type :text
    :label "Business co-presenter? (name/title — or would you add one?)"
    :help "Talks pairing a technologist with a business partner are especially welcome."
    :placeholder "Maria Chen, VP Claims Operations"
    :required false
    :max-length 255}

   {:id :prior-talk-video
    :type :url
    :label "Link to a video of a prior talk"
    :required false}

   {:id :notes-to-committee
    :type :textarea
    :label "Notes to the Planning Committee"
    :help "Private — never shown publicly or in exports."
    :required false
    :private true
    :max-length 2000}

   {:id :speakers
    :type :group
    :label "Speakers"
    :repeatable true
    :locked true
    :min-count 1
    :fields [{:id :speaker-name    :type :text  :label "Name"            :required true :locked true :max-length 120}
             {:id :speaker-email   :type :email :label "Email"           :required true :locked true :max-length 255}
             {:id :speaker-title   :type :text  :label "Title / tagline" :required true :max-length 180}
             {:id :speaker-org     :type :text  :label "Organization"    :required true :max-length 180}
             {:id :speaker-bio     :type :markdown :label "Bio"          :required true :max-length 2000}
             {:id :speaker-headshot :type :file :label "Headshot"        :required false :accept ["image/png" "image/jpeg"]}
             {:id :speaker-linkedin :type :url  :label "LinkedIn"        :required false}
             {:id :speaker-sessionize-url
              :type :url
              :label "Have a Sessionize profile? Paste the URL"
              :help "We'll prefill the fields above from it."
              :required false}]}])

(def default-event-settings
  "Settings JSONB installed on a new event (form-builder.md §Settings +
   domain-model.md). Statuses are the 7-valued vocabulary; visibility is open."
  {:review-visibility  "open"
   ;; Accept Queue / Decline Queue are INTERNAL triage — they are never
   ;; communicated to a speaker. Only the three terminal decisions
   ;; (Accepted / Waitlisted / Declined) have letters and can be informed.
   ;; Waitlisted comes from Sessionize's own board (docs/sessionize-recon §04).
   :statuses           ["Accepted" "Waitlisted" "Accept Queue" "Pending"
                        "Decline Queue" "Declined" "Withdrawn" "Draft"]
   :submissions-per-person-cap 3
   :reminder-email-enabled true
   ;; Installed on a submission the moment its acceptance is COMMUNICATED —
   ;; not when it is accepted. A speaker who hasn't been told has no tasks.
   :default-speaker-tasks
   [{:key "confirm-bio" :type "check"
     :label "Confirm your bio and tagline are how you want them printed"}
    {:key "headshot" :type "url"
     :label "Headshot URL (300×300 or larger)"}
    {:key "slides-url" :type "url"
     :label "Link to your slides (Google Slides, Dropbox, anything we can open)"}]})

(def default-committee
  "Every event auto-spawns exactly one committee. Scope :all = Gene's flow, the
   degenerate case with zero extra UI (domain-model.md)."
  {:name "Program Committee"
   :scope {:all true}
   :coverage-target 2})
