## MODIFIED Requirements

### Requirement: Installers attached to a GitHub Release
The release workflow SHALL create a GitHub Release for the tag and attach all three installer files as downloadable assets.

#### Scenario: Release created with all assets
- **WHEN** all three build jobs complete successfully
- **THEN** a GitHub Release SHALL exist for the tag with `.dmg`, `.msi`, and `.deb` files attached, named using `PDF-Tapir` prefix
