---
description: Scaffold a new feature across all layers (entity → DAO → repo → use case → ViewModel → screen → navigation). Pass the feature name as the argument.
argument-hint: <FeatureName>
---

Scaffold a new feature for CallVault. Argument: `$ARGUMENTS` is the feature name in PascalCase (e.g. `OrderHistory`).

Spawn the `callvault-android-engineer` agent with this brief:

> "Scaffold a new feature `$ARGUMENTS` for CallVault. Project root: `/home/primathon/Documents/p_projet/a_APP/4. callVault/`. Read CLAUDE.md first. Generate the minimum viable surface across layers, matching existing conventions:
>
> 1. Domain model `domain/model/$ARGUMENTS.kt` (data class).
> 2. Domain repository interface `domain/repository/${ARGUMENTS}Repository.kt` with `observeAll(): Flow<List<$ARGUMENTS>>` placeholder.
> 3. Room entity `data/local/entity/${ARGUMENTS}Entity.kt` (only if persisting state — ask if unclear).
> 4. DAO `data/local/dao/${ARGUMENTS}Dao.kt` with insert/observeAll/deleteAll.
> 5. Repository impl `data/repository/${ARGUMENTS}RepositoryImpl.kt` mapping entity ↔ domain.
> 6. ViewModel `ui/screen/${arguments-lowercase}/${ARGUMENTS}ViewModel.kt` with `StateFlow<${ARGUMENTS}UiState>`.
> 7. Screen `ui/screen/${arguments-lowercase}/${ARGUMENTS}Screen.kt` with `NeoScaffold`, top bar, empty/loading/error states, at least one `@Preview`.
> 8. Navigation entry: add `Destinations.$ARGUMENTS` to `ui/navigation/Destinations.kt` and register in `CallVaultNavHost.kt`.
> 9. Hilt: bind the repo impl to its interface in `di/RepositoryModule.kt`.
> 10. Bump Room version to current+1 and add migration if you added an entity.
>
> Match conventions strictly. KDoc on every public class. Real strings via stringResource (add to res/values/strings.xml). No mock data outside @Preview. Report files added by directory."

After the agent reports, surface a one-paragraph summary and suggest `/build` to verify compilation.
