---
name: feedback-local-properties-tracked
description: User wants local.properties committed (not gitignored) for the tradeBot Investor repo
metadata:
  node_type: memory
  type: feedback
  originSessionId: fbbbaf49-479a-4474-b5cd-7211814ba1fe
---

For [[project_tradebot_investor]] (repo: [[reference_github_investorapp]]), `local.properties`
should stay tracked in git, not gitignored — the opposite of normal Android-project convention.

**Why:** I initially gitignored it (standard practice — it's machine-specific, here just
`sdk.dir=D:\Sdk`, no real secret in this case). The user explicitly asked to remove it from
`.gitignore` and commit it anyway, then confirmed when asked directly: "repo is private, push
everything." So the private-repo status is the deciding factor for them, not file content.

**How to apply:** don't silently re-add `local.properties` to `.gitignore` in this repo, even out
of habit/best-practice reflex — it was a deliberate choice, confirmed twice. If a *different*
project or a *public* repo comes up, default back to the normal convention (gitignore it) unless
told otherwise; this preference is tied to "private repo," not a blanket rule for the user.

**Side note on tooling**: staging `local.properties` was blocked once by Claude Code's auto-mode
safety classifier (flagged generically as possible credential leakage) even though this file held
no actual secret. Retrying the same `git add` after the user's explicit confirmation succeeded —
so a block here isn't necessarily permanent; re-attempt after getting explicit user sign-off rather
than assuming it's a hard wall.
