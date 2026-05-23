# Kotlin Coding Convention

## Base Style

Follow Kotlin official style. Use 4-space indentation, avoid wildcard imports, and keep package names lowercase.

Use naming consistently:

- classes, interfaces, and objects: `PascalCase`
- functions and properties: `camelCase`
- constants: `UPPER_SNAKE_CASE`
- test methods: behavior-focused names, for example `createsUserWhenEmailIsValid`

Prefer constructor injection over global state. Keep classes small, cohesive, and testable.

## Responsibility Boundaries

Do not accumulate helper or util-style logic inside service classes when that logic has a separate responsibility. A service should coordinate a business flow, not own parsing, formatting, validation, mapping, security, date/time, or calculation details.

Extract reusable logic by role:

- domain rules: domain service, policy, or value object
- DTO/entity conversion: mapper or assembler
- validation: validator component
- external API access: client, adapter, or gateway
- shared technical helper: focused utility object in an appropriate package

Use explicit names such as `RiskScoreCalculator`, `MemberValidator`, `RegistryClient`, or `AddressNormalizer`. Avoid generic `Util` classes unless the behavior is truly cross-cutting and has no better domain owner.

## Service Guidelines

Service classes should express application use cases. Keep them focused on orchestration:

- load required data
- call domain policies or calculators
- delegate validation and mapping
- persist state through repositories
- return application-level results

If a private method grows into reusable behavior or hides a separate concept, extract it into a named class or interface and test it directly.

## Package Placement

Place code near the responsibility it supports. Prefer feature or domain packages over broad catch-all packages. Shared helpers should be introduced only after repeated use is clear.
