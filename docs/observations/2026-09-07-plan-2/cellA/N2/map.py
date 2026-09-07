MAPPING = {
 'auth': ['landing-page','login-page'],
 'avatar': ['face-pool-size','initials','pool-face'],
 'committee': ['committee-card','committee-page','member-row'],
 'communications': ['comms-page','inform-page','letter-block'],
 'dashboard': ['alert-rows-partial','checklist-item','dash-days-left','dash-feed-talk','dash-recent-line','dash-rel-time','event-dashboard-page','event-dashboard-region'],
 'event-setup': ['default-start-date','default-end-date','example-name','example-location','example-website','event-details-page','event-marquee','events-list-page','new-event-page','slug-status'],
 'format': ['date-fmt','datetime-fmt','when-fmt','iso-date-fmt','->instant','->local-date','cfp-public-url','fmt-cfp-window','fmt-close-date','fmt-date','fmt-date-range','fmt-instant','fmt-when','not-blank','relative-when'],
 'form-builder': ['fb-post','field-form-fields','field-row','finish-cfp-bar','form-builder-page','form-edit-panel','form-fields-region','form-grid-region','form-preview-region','type-label','fb-tags'],
 'form-controls': ['answer-input','field-error','field-errors','req-mark'],
 'integrations': ['api-docs-page','exports-page','mask-webhook-url','settings-page','slack-form'],
 'live-drafts': ['cfp-draft-status','cfp-note','portal-draft-status'],
 'log': ['log-page','log-region','log-summary'],
 'organizer-layout': ['breadcrumb','dev-strip','event-resume-path','event-setup-done?','header','organizer-shell','sb-group','sb-link','sb-out','sidebar','time-travel-bar','whoami-strip'],
 'people': ['person-page','profile-links'],
 'portal': ['edit-form','portal-page','portal-submission','profile-form','status-pill','task-row'],
 'public-cfp': ['speaker-inputs','md-token','cfp-about-you','cfp-closed-notice','cfp-page','cfp-success-page','md-inline','md-lite','render-markdown','speaker-input'],
 'replay': ['replay-page','replay-progress-bar'],
 'review': ['board-page','board-qs','board-region','board-row','capture-page','chair-on-event?','coverage-bar','fmt-mean','fmt-stars','inform-banner','notice-region','opinions-block','private-note-block','row-controls','row-controls*','sort-chip','sort-click','sort-th','star-form','star-histogram','status-chip','submission-detail-page','submissions-sparkline','track-chip','submissions-page'],
 'schedule': ['agenda-page','block-card','conflict-chips','day-tab','placed-card','place-form','room-options','schedule-grid','schedule-page','schedule-status-bar'],
 'shell': ['datastar-script','favicon-data-uri','page-shell','versioned'],
}
NS2FILE = {k: k.replace('-','_') for k in MAPPING}
FOUNDATION = {'avatar','form-controls','format','live-drafts','organizer-layout','shell'}
PUBLIC = {'auth','portal','public-cfp'}
