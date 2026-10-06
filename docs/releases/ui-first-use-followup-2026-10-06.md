# First use, request feedback and quieter follow-up

October 6, 2026. Native development UI; genuine handoff and physical-device
acceptance remain open.

## Changes

- Empty Home explicitly says to choose DropRun in another app's share menu,
  then choose a project. The existing empty-state layout and scrolling remain.
- Task action feedback says “Processing request” and “Request confirmed”.
  Reopening a preview and clearing a cached copy no longer say “Decision saved”.
  Preview readiness still comes from its separate actual status.
- Follow-up uses the existing ghost button style. Ready previews remain the
  primary lime action, delivery files keep their outlined entry, and follow-up
  is a quieter action. Button type, target, callback, busy state, pressed and
  keyboard-focus feedback are preserved.
- The bilingual README includes one explicitly illustrative note connecting a
  design reference to a concrete change in an existing project. No outcome is
  claimed for that example.

## Actual verification

The existing native capture test passed 1/1 in 12.684s across English/Chinese
and light/dark at normal font. All four windows reached known `DESTROYED`;
their captured guard observations were zero. All four original 320 × 640 PNGs
were directly viewed and copied exactly to the brand/website assets, with
memory markers and system pixels retained.

The existing large-text reading/plan test passed 1/1 in 3.648s across
English/light and Chinese/dark at 200%. Result text, preview, file and follow-up
actions remained individually reachable; the expanded report remained readable.
Plan eligibility and busy-disabled controls remained. Both windows reached
known `DESTROYED`; two actual final guard rows were zero.

No business action was invoked. Device facts and all 121 Android source files
plus four Gradle files matched before/after. Android JVM24 passed; lint had
zero errors and 30 existing warnings. OS/IME settings were unchanged.

The empty-state and request-message edits were reviewed as localized copy and
compiled in this build. These probes do not exercise empty Home, the async
operation or its success toast; the fixture prohibits those business actions.
No new physical performance or real preview result is claimed.

## Source and stills

Development build 0.5.8-dev/code21; Task source SHA256
`78cac5ab647583fde8dbc2c7f5fc49031d37fa96312951f8c4f3182c5755443e`;
Main source
`4ada0f5b394141979fcd221a426634345cd9c2c47fa8965e7a67c750e98aebba`.
App APK SHA256
`acfde93a96fe3cd03246612ed53b8bd1062fe16736b66dd44557784ad71f6ed8`;
test APK
`3f1639aeeeb3e3beb7ee047a90c45d66b68514725a62ae03c9d5f28022899138`.

[English/light](../../assets/brand/source-delivery-followup-en-light-20261006.png)
· [dark](../../assets/brand/source-delivery-followup-en-dark-20261006.png)
· [中文/浅色](../../assets/brand/source-delivery-followup-zh-light-20261006.png)
· [深色](../../assets/brand/source-delivery-followup-zh-dark-20261006.png)

These captures replace the current display references, while the earlier reading
captures remain available with their original source identity. The signed
0.5.8-rc.1 download remains unchanged pending the next candidate.
