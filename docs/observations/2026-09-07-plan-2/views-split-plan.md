# views.clj split plan (derived from historical commit c4299615; parent 5ef17667)

Source: src/cfp_scheduler_killer/views.clj (4594 lines, 129 top-level defs). Destination namespaces (20) and the forms each receives, BY NAME as the historical commit placed them:

## cfp-scheduler-killer.views.auth (src/cfp_scheduler_killer/views/auth.clj)
- landing-page
- login-page

## cfp-scheduler-killer.views.avatar (src/cfp_scheduler_killer/views/avatar.clj)
- ^:private
- initials
- pool-face

## cfp-scheduler-killer.views.committee (src/cfp_scheduler_killer/views/committee.clj)
- committee-card
- committee-page
- member-row

## cfp-scheduler-killer.views.communications (src/cfp_scheduler_killer/views/communications.clj)
- comms-page
- inform-page
- letter-block

## cfp-scheduler-killer.views.dashboard (src/cfp_scheduler_killer/views/dashboard.clj)
- alert-rows-partial
- checklist-item
- dash-days-left
- dash-feed-talk
- dash-recent-line
- dash-rel-time
- event-dashboard-page
- event-dashboard-region

## cfp-scheduler-killer.views.event-setup (src/cfp_scheduler_killer/views/event_setup.clj)
- ^:private
- ^:private
- ^:private
- ^:private
- ^:private
- event-details-page
- event-marquee
- events-list-page
- new-event-page
- slug-status

## cfp-scheduler-killer.views.format (src/cfp_scheduler_killer/views/format.clj)
- ^:private
- ^:private
- ^:private
- ^:private
- ->instant
- ->local-date
- cfp-public-url
- fmt-cfp-window
- fmt-close-date
- fmt-date
- fmt-date-range
- fmt-instant
- fmt-when
- not-blank
- relative-when

## cfp-scheduler-killer.views.form-builder (src/cfp_scheduler_killer/views/form_builder.clj)
- fb-post
- field-form-fields
- field-row
- finish-cfp-bar
- form-builder-page
- form-edit-panel
- form-fields-region
- form-grid-region
- form-preview-region
- type-label

## cfp-scheduler-killer.views.form-controls (src/cfp_scheduler_killer/views/form_controls.clj)
- answer-input
- field-error
- field-errors
- req-mark

## cfp-scheduler-killer.views.integrations (src/cfp_scheduler_killer/views/integrations.clj)
- api-docs-page
- exports-page
- mask-webhook-url
- settings-page
- slack-form

## cfp-scheduler-killer.views.live-drafts (src/cfp_scheduler_killer/views/live_drafts.clj)
- cfp-draft-status
- cfp-note
- portal-draft-status

## cfp-scheduler-killer.views.log (src/cfp_scheduler_killer/views/log.clj)
- log-page
- log-region
- log-summary

## cfp-scheduler-killer.views.organizer-layout (src/cfp_scheduler_killer/views/organizer_layout.clj)
- breadcrumb
- dev-strip
- event-resume-path
- event-setup-done?
- header
- organizer-shell
- sb-group
- sb-link
- sb-out
- sidebar
- time-travel-bar
- whoami-strip

## cfp-scheduler-killer.views.people (src/cfp_scheduler_killer/views/people.clj)
- person-page
- profile-links

## cfp-scheduler-killer.views.portal (src/cfp_scheduler_killer/views/portal.clj)
- edit-form
- portal-page
- portal-submission
- profile-form
- status-pill
- task-row

## cfp-scheduler-killer.views.public-cfp (src/cfp_scheduler_killer/views/public_cfp.clj)
- ^:private
- ^:private
- cfp-about-you
- cfp-closed-notice
- cfp-page
- cfp-success-page
- md-inline
- md-lite
- render-markdown
- speaker-input

## cfp-scheduler-killer.views.replay (src/cfp_scheduler_killer/views/replay.clj)
- replay-page
- replay-progress-bar

## cfp-scheduler-killer.views.review (src/cfp_scheduler_killer/views/review.clj)
- board-page
- board-qs
- board-region
- board-row
- capture-page
- chair-on-event?
- coverage-bar
- fmt-mean
- fmt-stars
- inform-banner
- notice-region
- opinions-block
- private-note-block
- row-controls
- row-controls
- sort-chip
- sort-click
- sort-th
- star-form
- star-histogram
- status-chip
- submission-detail-page
- submissions-sparkline
- track-chip

## cfp-scheduler-killer.views.schedule (src/cfp_scheduler_killer/views/schedule.clj)
- agenda-page
- block-card
- conflict-chips
- day-tab
- placed-card
- place-form
- room-options
- schedule-grid
- schedule-page
- schedule-status-bar

## cfp-scheduler-killer.views.shell (src/cfp_scheduler_killer/views/shell.clj)
- datastar-script
- favicon-data-uri
- page-shell
- versioned

## Unmapped (the commit dropped or renamed them): fb-tags, submissions-page — keep them in a fitting destination namespace and say which.

## Callers at the parent commit (must compile after the split): src/cfp_scheduler_killer/server.clj (the main one), test/cfp_scheduler_killer/{comms_test,forms_test,polish_test,views_test}.clj

## Oracle: (1) bin/kaocha unit --fail-fast green; (2) src/cfp_scheduler_killer/views.clj no longer exists; (3) every form above is defined exactly once, in its listed namespace (grep oracle: for each name, exactly one '(def… <name>' under src/cfp_scheduler_killer/views/<file>); (4) the historical architecture guard test/cfp_scheduler_killer/view_architecture_test.clj (copied into the worktree by the caller at the end) passes: expected namespace set, acyclic view graph via clj-kondo analysis, foundation/public namespace rules.
