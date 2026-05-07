# File Map

This is an AI-oriented guide to project files. Use it to find the owner of a behavior before editing.

## Root Files

- `CLAUDE.md` - session rules, locked stack, architecture constraints, agent workflow.
- `README.md` - product summary, build commands, sprint summary, shipped status.
- `DEVELOPING.md` - setup, build, install, debug, manual verification guide.
- `CHANGELOG.md` - v1.0.0 features, known limitations, tech debt.
- `DECISIONS.md` - trade-offs and deferrals made during implementation.
- `docs/architecture.md` - existing architecture overview and diagrams.
- `settings.gradle.kts` - Gradle project/plugin management root.
- `build.gradle.kts` - root Gradle config.
- `gradle/libs.versions.toml` - all dependency/plugin versions.
- `gradle/wrapper/gradle-wrapper.properties` - wrapper distribution metadata.
- `package-lock.json` - present, but this is not a Node app. Do not infer frontend tooling from it.

## App Build And Resources

- `app/build.gradle.kts` - Android app config, signing, SDK levels, dependencies, BuildConfig update URLs.
- `app/proguard-rules.pro` - release shrinker rules.
- `app/src/main/AndroidManifest.xml` - permissions, app class, activity, service, receivers, FileProvider.
- `app/src/main/res/values/strings.xml` - UI strings and notification channel names.
- `app/src/main/res/values/themes.xml` - Android activity theme.
- `app/src/main/res/xml/backup_rules.xml` - Android backup exclusion config.
- `app/src/main/res/xml/data_extraction_rules.xml` - data extraction disabled.
- `app/src/main/res/xml/file_paths.xml` - FileProvider paths for update APKs.
- `app/src/main/res/drawable/*` and `mipmap-*/*` - launcher assets.

## Entry Points

- `callNestApp.kt` - Hilt application, WorkManager factory, Timber, notification channels, startup worker/service scheduling.
- `MainActivity.kt` - single Activity, Compose host, permission recheck on resume, deep-link route extra.

## Dependency Injection

- `di/AppModule.kt` - general app-level providers.
- `di/DatabaseModule.kt` - Room database and DAO providers.
- `di/RepositoryModule.kt` - binds repository interfaces to data implementations.
- `di/WorkerModule.kt` - worker-related bindings/providers.

## Domain Models

- `domain/model/Call.kt` - domain call record and call type enum.
- `domain/model/ContactMeta.kt` - per-number aggregate/contact metadata.
- `domain/model/Tag.kt` - tag model.
- `domain/model/Note.kt` - note model.
- `domain/model/FilterState.kt` - calls filtering state.
- `domain/model/AutoTagRule.kt` - rule metadata and priority.
- `domain/model/RuleCondition.kt` - sealed rule condition hierarchy.
- `domain/model/RuleAction.kt` - sealed rule action hierarchy.
- `domain/model/LeadScore.kt` - score result and weights.
- `domain/model/StatsModels.kt` - stats dashboard value objects.
- `domain/model/ExportConfig.kt` - export format/scope/columns config.
- `domain/model/SyncResult.kt` - sync success/partial/failure result.

## Repository Interfaces

- `domain/repository/CallRepository.kt` - calls, searches, filters, bookmarks, follow-ups.
- `domain/repository/ContactRepository.kt` - contact metadata and aggregates.
- `domain/repository/TagRepository.kt` - tag CRUD and assignment.
- `domain/repository/NoteRepository.kt` - notes and note history.
- `domain/repository/AutoTagRuleRepository.kt` - rule CRUD, reorder, preview, delete cleanup.
- `domain/repository/SettingsRepository.kt` - settings abstraction.
- `domain/repository/UpdateRepository.kt` - update state and actions.

## Domain Use Cases

- `SyncCallLogUseCase.kt` - main call-log import/enrichment pipeline.
- `SyncProgressBus.kt` - sync progress shared flow.
- `NormalizePhoneNumberUseCase.kt` - domain wrapper for normalization.
- `ResolveContactUseCase.kt` - contact resolution wrapper.
- `ComputeLeadScoreUseCase.kt` - score formula and manual override handling.
- `ComputeStatsUseCase.kt` - dashboard aggregates.
- `GenerateInsightsUseCase.kt` - rule-based stats insights.
- `ApplyAutoTagRulesUseCase.kt` - applies active rules to calls.
- `RuleConditionEvaluator.kt` - pure condition evaluation.
- `RuleActionApplier.kt` - mutates tags/bookmarks/follow-ups/score boosts for rule actions.
- `AutoSaveNameBuilder.kt` - locked auto-save display-name format.
- `AutoSaveContactUseCase.kt` - one-number system contact insert and metadata patch.
- `BulkSaveContactsUseCase.kt` - batch auto-save.
- `BulkSaveProgressBus.kt` - progress flow for bulk save UI.
- `DetectAutoSavedRenameUseCase.kt` - detects renamed auto-saved contacts.
- `ConvertToMyContactUseCase.kt` - promotes inquiry to normal contact.
- `ScheduleFollowUpUseCase.kt` - follow-up scheduling.
- `ExportUseCases.kt` - use-case wrappers for CSV/Excel/PDF/JSON/vCard export.
- `BackupUseCases.kt` - backup/restore wrappers.
- `UpdateUseCases.kt` - update check/download/install wrappers.
- `ResetAllDataUseCase.kt` - destructive reset flow.
- `DeferredUseCases.kt` - legacy/deferred placeholders. Avoid adding new reachable work here.

## Data: Room Database

- `data/local/callNestDatabase.kt` - Room database, version 2, entity/DAO list.
- `data/local/converter/Converters.kt` - Room type converters.
- `data/local/migration/Migrations.kt` - Room migrations.
- `data/local/mapper/Mappers.kt` - entity/domain mapping.
- `data/local/seed/DefaultTagsSeeder.kt` - default system tags.

## Data: Entities

- `CallEntity.kt` - enriched call row, primary key is CallLog system ID.
- `ContactMetaEntity.kt` - aggregate/contact state per normalized number.
- `TagEntity.kt` - tag row.
- `CallTagCrossRef.kt` - call/tag join plus appliedBy source.
- `NoteEntity.kt` - note row.
- `NoteHistoryEntity.kt` - note edit history.
- `FilterPresetEntity.kt` - saved filter preset row.
- `AutoTagRuleEntity.kt` - persisted rule with JSON conditions/actions.
- `RuleScoreBoostEntity.kt` - per-call per-rule score boost.
- `SearchHistoryEntity.kt` - recent search terms.
- `DocFeedbackEntity.kt` - in-app docs helpful/not-helpful feedback.
- `SkippedUpdateEntity.kt` - skipped self-update versions.
- `CallFts.kt` - FTS table for calls.
- `NoteFts.kt` - FTS table for notes.

## Data: DAOs

- `CallDao.kt` - calls CRUD, raw filters, FTS, aggregates, stats queries.
- `ContactMetaDao.kt` - contact metadata CRUD and lead-score rows.
- `TagDao.kt` - tag CRUD, usage counts, cross-ref operations.
- `NoteDao.kt` - notes, history, FTS, orphan attachment.
- `FilterPresetDao.kt` - saved filter presets.
- `AutoTagRuleDao.kt` - rule CRUD and active-rule ordering.
- `RuleScoreBoostDao.kt` - score boost insert/delete/sum.
- `SearchHistoryDao.kt` - recent searches.
- `DocFeedbackDao.kt` - docs feedback counts.
- `SkippedUpdateDao.kt` - skipped update records.

## Data: Repository Implementations

- `CallRepositoryImpl.kt` - maps calls between domain and Room; filtering/search.
- `ContactRepositoryImpl.kt` - contact metadata persistence.
- `TagRepositoryImpl.kt` - tags and assignment operations.
- `NoteRepositoryImpl.kt` - notes and edit history.
- `AutoTagRuleRepositoryImpl.kt` - rules plus application-level cascade cleanup.
- `SettingsRepositoryImpl.kt` - settings facade over DataStore.
- `UpdateRepositoryImpl.kt` - update state, skipped versions, checker/downloader/installer.
- `FilterQueryBuilder.kt` - dynamic SQL builder for `FilterState`.

## Data: Preferences And Events

- `SettingsDataStore.kt` - typed Preferences DataStore keys and setters.
- `SecurePrefs.kt` - encrypted backup passphrase storage.
- `UiEventBus.kt` - app-wide one-shot UI events from background paths.

## Data: System APIs

- `CallLogReader.kt` - reads Android `CallLog.Calls` rows.
- `PhoneNumberNormalizer.kt` - libphonenumber-backed E.164 normalization.
- `ContactsReader.kt` - resolves contact names and IDs.
- `ContactsWriter.kt` - writes RawContact, name, phone, group membership through `applyBatch`.
- `ContactGroupManager.kt` - finds/creates callNest contact group.
- `SimSlotResolver.kt` - resolves SIM slot/carrier from phone account ID.
- `PhoneStateMonitor.kt` - call state flow for real-time features.
- `CallContextResolver.kt` - finds latest call context for popup actions.

## Data: WorkManager And Receivers

- `CallSyncWorker.kt` - Hilt worker invoking `SyncCallLogUseCase`.
- `SyncScheduler.kt` - translates sync interval settings into WorkManager/AlarmManager scheduling.
- `BootCompletedReceiver.kt` - re-schedules work and real-time service after reboot.
- `SeedDefaultTagsWorker.kt` - seeds system tags.
- `LeadScoreRecomputeWorker.kt` - recomputes scores after settings/rules changes.
- `DailyBackupWorker.kt` - encrypted daily backup and retention.
- `DailySummaryWorker.kt` - daily summary notifications.
- `UpdateCheckWorker.kt` - weekly update checks.

## Data: Services And Alarms

- `CallEnrichmentService.kt` - foreground real-time call service.
- `ExactAlarmScheduler.kt` - exact/inexact follow-up alarm scheduling.
- `FollowUpAlarmReceiver.kt` - follow-up notification action receiver.
- `OverlayManager.kt` - WindowManager overlay lifecycle.
- `FloatingBubbleView.kt` - in-call bubble Android view.
- `PostCallPopupView.kt` - post-call popup Android view.

## Data: Export, Backup, Update

- `ExportShared.kt` - export filters, columns, destinations, shared row loading.
- `CsvExporter.kt` - CSV writer.
- `ExcelExporter.kt` - XLSX writer.
- `PdfExporter.kt` - PDF writer.
- `JsonExporter.kt` - JSON full database dump and restore DTOs.
- `VcardExporter.kt` - vCard writer.
- `BackupManager.kt` - encrypted backup/restore orchestration.
- `EncryptionHelper.kt` - PBKDF2/AES-GCM implementation.
- `UpdateManifest.kt` - update manifest DTOs.
- `UpdateChecker.kt` - manifest fetch and version comparison.
- `UpdateDownloader.kt` - DownloadManager download and SHA verification.
- `UpdateInstaller.kt` - FileProvider and package installer intent.
- `UpdateNotifier.kt` - update notification posting.

## Utilities

- `PermissionManager.kt` - runtime permission and overlay/exact-alarm state.
- `RealTimeServiceController.kt` - starts/stops real-time service based on settings and permissions.
- `OemBatteryGuide.kt` - OEM battery/autostart vendor detection and intents.
- `AssetDocsLoader.kt` - loads bundled markdown docs from assets.
- `AutoSavePatternMatcher.kt` - matches auto-saved contact naming patterns.

## UI Theme And Components

- `ui/theme/Color.kt` - Neo color tokens.
- `ui/theme/Theme.kt` - Compose theme.
- `ui/theme/Type.kt` - typography.
- `ui/theme/Shape.kt` - shapes.
- `ui/theme/NeoElevation.kt` - elevation tokens.
- `ui/theme/NeoShadows.kt` - shadow tokens.
- `ui/components/neo/*` - reusable neumorphic components: surface, card, buttons, icon buttons, chips, toggles, sliders, search bar, FAB, tab bar, top bar, bottom sheet, text field, progress bar, badge, avatar, divider, empty state, help icon, lead-score badge, shadow modifier.

## UI Navigation And Shared Layout

- `ui/navigation/Destinations.kt` - stable route declarations.
- `ui/navigation/callNestNavHost.kt` - top-level graph and startup gates.
- `ui/screen/shared/NeoScaffold.kt` - shared scaffold pattern.
- `ui/screen/PlaceholderScreen.kt` - generic placeholder.

## UI Screens

- `onboarding/*` - onboarding pager, VM, pages for welcome/features/permissions/OEM battery/first sync.
- `permission/*` - permission rationale and denied screens.
- `calls/*` - calls list, VM, filter sheet, row, bulk actions, unsaved pinned section, update banner.
- `calldetail/*` - detail screen, VM, hero, actions, stats, tags, notes, follow-up, history sections.
- `search/*` - full-screen search and recent search handling.
- `mycontacts/*` - saved contacts bucket.
- `inquiries/*` - auto-saved inquiry bucket and bulk-save progress dialog.
- `tags/*` - tag manager, picker, editor dialog.
- `autotagrules/*` - rules list/editor and condition/action components.
- `settings/*` - master settings and auto-save, real-time, lead-score, update settings.
- `stats/*` - stats dashboard, VM, charts.
- `export/*` - export wizard and steps.
- `backup/*` - backup/restore screen.
- `update/*` - update available/progress screen and VM.
- `followups/*` - follow-up list screen and VM.
- `bookmarks/*` - bookmarks list and reason dialog.
- `docs/*` - docs list/article screens.
- `home/*` - home tab.
- `more/*` - more tab.

## Complete UI Source Index

Navigation:

- `ui/navigation/callNestNavHost.kt` - app graph, startup routing, deep-link routing.
- `ui/navigation/Destinations.kt` - route constants and route builders.

Neo components:

- `ui/components/neo/LeadScoreBadge.kt` - cold/warm/hot badge.
- `ui/components/neo/NeoAvatar.kt` - avatar surface.
- `ui/components/neo/NeoBadge.kt` - small status badge.
- `ui/components/neo/NeoBottomSheet.kt` - restyled modal bottom sheet.
- `ui/components/neo/NeoButton.kt` - primary/secondary buttons.
- `ui/components/neo/NeoCard.kt` - neumorphic card.
- `ui/components/neo/NeoChip.kt` - chip control.
- `ui/components/neo/NeoDivider.kt` - divider.
- `ui/components/neo/NeoEmptyState.kt` - empty-state block.
- `ui/components/neo/NeoFAB.kt` - floating action button.
- `ui/components/neo/NeoHelpIcon.kt` - contextual docs/help affordance.
- `ui/components/neo/NeoIconButton.kt` - icon button.
- `ui/components/neo/NeoProgressBar.kt` - progress indicator.
- `ui/components/neo/NeoSearchBar.kt` - search input.
- `ui/components/neo/NeoSlider.kt` - slider.
- `ui/components/neo/NeoSurface.kt` - base neumorphic surface.
- `ui/components/neo/NeoTabBar.kt` - tab bar.
- `ui/components/neo/NeoTextField.kt` - text field.
- `ui/components/neo/NeoToggle.kt` - toggle.
- `ui/components/neo/NeoTopBar.kt` - top app bar.
- `ui/components/neo/ShadowModifier.kt` - shadow modifier implementation.

Screens:

- `ui/screen/PlaceholderScreen.kt` - generic placeholder.
- `ui/screen/autotagrules/AutoTagRulesScreen.kt` - rules list UI.
- `ui/screen/autotagrules/AutoTagRulesViewModel.kt` - rules list state/actions.
- `ui/screen/autotagrules/RuleEditorScreen.kt` - rule editor UI.
- `ui/screen/autotagrules/RuleEditorViewModel.kt` - rule editor state/actions/preview.
- `ui/screen/autotagrules/components/ActionRow.kt` - rule action editor row.
- `ui/screen/autotagrules/components/ConditionRow.kt` - rule condition editor row.
- `ui/screen/autotagrules/components/LivePreviewBox.kt` - matching-call preview.
- `ui/screen/backup/BackupScreen.kt` - backup/restore UI.
- `ui/screen/backup/BackupViewModel.kt` - backup/restore state/actions.
- `ui/screen/bookmarks/BookmarkReasonDialog.kt` - bookmark reason entry.
- `ui/screen/bookmarks/BookmarksScreen.kt` - bookmarked numbers/calls UI.
- `ui/screen/bookmarks/BookmarksViewModel.kt` - bookmark state/actions.
- `ui/screen/calldetail/CallDetailScreen.kt` - per-number detail screen.
- `ui/screen/calldetail/CallDetailViewModel.kt` - detail state/actions.
- `ui/screen/calldetail/sections/ActionBar.kt` - call/message/WhatsApp/save/block actions.
- `ui/screen/calldetail/sections/FollowUpDateTimeDialog.kt` - follow-up date/time picker.
- `ui/screen/calldetail/sections/FollowUpSection.kt` - follow-up card.
- `ui/screen/calldetail/sections/HeroCard.kt` - number/contact hero.
- `ui/screen/calldetail/sections/HistoryTimeline.kt` - call timeline.
- `ui/screen/calldetail/sections/NoteEditorDialog.kt` - note editor.
- `ui/screen/calldetail/sections/NotesJournal.kt` - notes list.
- `ui/screen/calldetail/sections/StatsCard.kt` - per-number stats.
- `ui/screen/calldetail/sections/TagsSection.kt` - tags section.
- `ui/screen/calls/BulkActionBar.kt` - bulk-select actions.
- `ui/screen/calls/CallRow.kt` - call row UI.
- `ui/screen/calls/CallsFilterSheet.kt` - filters UI.
- `ui/screen/calls/CallsFilterSheetHost.kt` - filter sheet host VM/composable.
- `ui/screen/calls/CallsScreen.kt` - main calls screen.
- `ui/screen/calls/CallsViewModel.kt` - calls state/actions.
- `ui/screen/calls/UnsavedPinnedSection.kt` - recent unsaved inquiries section.
- `ui/screen/calls/components/UpdateBanner.kt` - update available banner.
- `ui/screen/docs/DocsArticleScreen.kt` - single markdown article UI.
- `ui/screen/docs/DocsListScreen.kt` - article list UI.
- `ui/screen/docs/DocsViewModel.kt` - docs state/actions/feedback.
- `ui/screen/export/ExportScreen.kt` - export wizard shell.
- `ui/screen/export/ExportViewModel.kt` - export wizard state/actions.
- `ui/screen/export/steps/ColumnsStep.kt` - export columns step.
- `ui/screen/export/steps/DateRangeStep.kt` - export date step.
- `ui/screen/export/steps/DestinationStep.kt` - export destination step.
- `ui/screen/export/steps/FormatStep.kt` - export format step.
- `ui/screen/export/steps/ScopeStep.kt` - export scope step.
- `ui/screen/followups/FollowUpsScreen.kt` - follow-up list UI.
- `ui/screen/followups/FollowUpsViewModel.kt` - follow-up state/actions.
- `ui/screen/home/HomeScreen.kt` - home tab.
- `ui/screen/inquiries/BulkSaveProgressDialog.kt` - bulk-save progress UI.
- `ui/screen/inquiries/InquiriesScreen.kt` - auto-saved inquiries UI.
- `ui/screen/inquiries/InquiriesViewModel.kt` - inquiries state/actions.
- `ui/screen/more/MoreScreen.kt` - more tab.
- `ui/screen/mycontacts/MyContactsScreen.kt` - saved contacts UI.
- `ui/screen/mycontacts/MyContactsViewModel.kt` - saved contacts state/actions.
- `ui/screen/onboarding/OnboardingScreen.kt` - onboarding pager shell.
- `ui/screen/onboarding/OnboardingViewModel.kt` - onboarding state/actions.
- `ui/screen/onboarding/pages/FeaturesPage.kt` - onboarding feature page.
- `ui/screen/onboarding/pages/FirstSyncPage.kt` - onboarding first-sync page.
- `ui/screen/onboarding/pages/OemBatteryPage.kt` - onboarding OEM battery page.
- `ui/screen/onboarding/pages/PermissionsPage.kt` - onboarding permissions page.
- `ui/screen/onboarding/pages/WelcomePage.kt` - onboarding welcome page.
- `ui/screen/permission/PermissionDeniedScreen.kt` - permanently denied UI.
- `ui/screen/permission/PermissionRationaleScreen.kt` - runtime permission rationale UI.
- `ui/screen/search/SearchScreen.kt` - search UI.
- `ui/screen/search/SearchViewModel.kt` - search state/actions.
- `ui/screen/settings/AutoSaveSettingsScreen.kt` - auto-save settings UI.
- `ui/screen/settings/AutoSaveSettingsViewModel.kt` - auto-save settings state/actions.
- `ui/screen/settings/LeadScoringSettingsScreen.kt` - lead score settings UI.
- `ui/screen/settings/LeadScoringSettingsViewModel.kt` - lead score settings state/actions.
- `ui/screen/settings/RealTimeSettingsScreen.kt` - real-time settings UI.
- `ui/screen/settings/RealTimeSettingsViewModel.kt` - real-time settings state/actions.
- `ui/screen/settings/SettingsScreen.kt` - master settings UI.
- `ui/screen/settings/SettingsViewModel.kt` - master settings state/actions.
- `ui/screen/settings/UpdateSettingsScreen.kt` - update settings UI.
- `ui/screen/settings/UpdateSettingsViewModel.kt` - update settings state/actions.
- `ui/screen/shared/NeoScaffold.kt` - shared screen scaffold.
- `ui/screen/stats/StatsScreen.kt` - stats dashboard UI.
- `ui/screen/stats/StatsViewModel.kt` - stats state/actions.
- `ui/screen/stats/charts/DailyVolumeChart.kt` - daily volume chart.
- `ui/screen/stats/charts/HourlyHeatmap.kt` - hourly heatmap chart.
- `ui/screen/stats/charts/TopNumbersList.kt` - top numbers list.
- `ui/screen/stats/charts/TypeDonut.kt` - call-type donut chart.
- `ui/screen/tags/TagEditorDialog.kt` - tag create/edit dialog.
- `ui/screen/tags/TagPickerSheet.kt` - tag picker sheet.
- `ui/screen/tags/TagsManagerScreen.kt` - tag management UI.
- `ui/screen/tags/TagsManagerViewModel.kt` - tag management state/actions.
- `ui/screen/update/UpdateAvailableScreen.kt` - update state/progress UI.
- `ui/screen/update/UpdateViewModel.kt` - update state/actions.

## UI Utilities

- `DateFormatter.kt` - UI date formatting.
- `DurationFormatter.kt` - duration formatting.
- `PhoneNumberFormatter.kt` - display formatting.
- `MarkdownRenderer.kt` - minimal markdown renderer for notes/docs.

## Tests

- `AutoSaveNameBuilderTest.kt` - auto-save naming.
- `AutoSavePatternMatcherTest.kt` - auto-save rename pattern matching.
- `GenerateInsightsUseCaseTest.kt` - stats insight generation.

## In-App Docs Articles

- `01-getting-started.md`
- `02-permissions.md`
- `03-auto-save.md`
- `04-my-contacts-vs-inquiries.md`
- `05-tags-and-rules.md`
- `06-filtering-and-search.md`
- `07-floating-bubble.md`
- `08-post-call-popup.md`
- `09-lead-scoring.md`
- `10-export.md`
- `11-backup-restore.md`
- `12-oem-battery.md`
- `13-not-default-dialer.md`
- `14-self-update.md`
- `15-privacy.md`

## Claude Local Files

- `.claude/agents/callNest-android-engineer.md` - feature/code implementation agent prompt.
- `.claude/agents/callNest-build-fixer.md` - build-fix agent prompt.
- `.claude/agents/callNest-ui-builder.md` - Compose/UI agent prompt.
- `.claude/agents/callNest-test-writer.md` - test-writing agent prompt.
- `.claude/settings.json` - Claude project settings.
- `.claude/docs/*` - this AI-oriented documentation set.
