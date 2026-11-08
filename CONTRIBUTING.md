# Contributing to Essentials

Thanks for wanting to help. Please read this page before writing any code — it saves both of us time.

## New features need an approved issue

**Do not open a pull request for a new feature unless there is an issue for it that is labeled `approved` and assigned to you.**

1. Open a [feature request](https://github.com/sameerasw/essentials/issues/new?template=feature_request.yml) describing the problem and your proposed solution.
2. Wait for it to be reviewed. It will be labeled `approved` (go ahead), `help wanted` (approved, open to anyone) or `declined`.
3. Comment on the issue that you'd like to work on it, and wait until it is assigned to you.
4. Agree the scope and design in the issue first: where the UI lives, what settings it adds, and which existing parts of the app it touches.
5. Open a pull request that links the issue with `Closes #123`.

Feature PRs without an approved, linked issue are labeled `needs-approved-issue` by a bot and will be closed without review. This isn't personal — Essentials is maintained by one person, and unplanned features are expensive to review and maintain long term.

### What does not need prior approval

- Bug fixes for an existing feature (linking a bug report is still appreciated)
- Translations
- Documentation and typo fixes

## Scope rules

- **One feature or fix per PR.** Don't bundle unrelated changes.
- **Keep it under ~1,000 changed lines.** Larger changes must be split into smaller PRs, or agreed in the issue first.
- **Shared parts need agreement in the issue first.** This includes `ScreenOffAccessibilityService`, the Dynamic Island and Duo models, gesture/touch handling, the keyboard (IME) and `SettingsRepository` structure.
- **No drive-by refactors** of code unrelated to your change.

## AI-assisted contributions

Using AI tools is fine, but you are responsible for every line you submit. You must understand the code, be able to explain it in review, and have tested it on a real device or emulator. Large, unreviewed AI-generated PRs will be closed.

## Pull request workflow

1. Branch from `develop` (e.g. `feature/my-feature` or `fix/issue-description`) and target `develop`.
2. Follow the conventions in the [Development Guide](docs/DEVELOPMENT_GUIDE.md).
3. Make sure `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` pass.
4. Fill in the pull request template completely, including screenshots or a recording for any UI change.

## Environment setup

1. Latest stable [Android Studio](https://developer.android.com/studio) and JDK 17 or higher.
2. `git clone https://github.com/sameerasw/essentials.git`
3. Many privileged features need [Shizuku](https://shizuku.rikka.app/) or Root for testing.

## Questions and ideas

For loose ideas or questions that aren't a concrete feature request yet, use [Discussions](https://github.com/sameerasw/essentials/discussions).
