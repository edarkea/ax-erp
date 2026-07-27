# Active organization context

Inject `ActiveOrganizationService` in later business modules.

Use `requireActiveCompany()` when an operation is company-scoped and
`requireActiveBranch()` only when a branch is mandatory. The context is session-scoped,
stores only record IDs, and never changes persistent default access records.

Services receiving a company-owned entity must still compare its company with
`requireActiveCompany()` and reject mismatches.
