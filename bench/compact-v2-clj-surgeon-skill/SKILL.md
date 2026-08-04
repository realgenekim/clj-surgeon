---
name: clj-surgeon
description: Read and edit Clojure structurally with exact clj-surgeon commands, low source output, and reviewed writes.
---

# clj-surgeon compact exact skill

Use `clj-surgeon` from `PATH`. Use these exact command shapes:

```bash
# Known top-level name or containing line; never run :ls first.
clj-surgeon :op :show-form :file FILE :form NAME
clj-surgeon :op :show-form :file FILE :line LINE

# Distinctive text but unknown form: first rg -n, then :show-form :line.
rg -n 'TEXT' FILE

# Structural pattern with unknown parent; :inside NAME is optional narrowing.
clj-surgeon :op :grep-form :file FILE :match 'FORM'

# Nested edit: run these as separate commands and review the plan between them.
clj-surgeon :op :replace-subform :file FILE :inside NAME \
  :match 'FORM' :with 'FORM' :plan-out /tmp/clj-surgeon-plan.edn
clj-surgeon :op :replace-subform! :plan /tmp/clj-surgeon-plan.edn
```

Do not chain plan and apply. Verify after applying. Stop on nonzero exit or EDN
`:error`. A `case` clause is sibling syntax, not a wrapper list; match its
independently readable value expression.
