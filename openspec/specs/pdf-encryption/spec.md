## ADDED Requirements

### Requirement: User can encrypt an open PDF with a password
The application SHALL allow the user to encrypt the currently open PDF with a user-supplied password. Both the owner password and user password SHALL be set to the supplied value using AES-256 encryption.

#### Scenario: Encrypt via menu
- **WHEN** the user selects Document → Encrypt… and enters a non-empty password and confirms
- **THEN** the PDF is saved encrypted with the supplied password and a success notification is shown

#### Scenario: Encrypt cancelled
- **WHEN** the user selects Document → Encrypt… and dismisses the dialog or leaves the password blank
- **THEN** no save occurs and the document state is unchanged

#### Scenario: Encrypt already-encrypted document
- **WHEN** the user selects Document → Encrypt… on a document that is already encrypted
- **THEN** the application allows re-encrypting with a new password (overwriting the previous encryption)

### Requirement: Application prompts for password when opening an encrypted PDF
The application SHALL detect when a PDF file is password-protected and prompt the user for the password before loading the document.

#### Scenario: Open encrypted PDF
- **WHEN** the user opens a PDF file that requires a password
- **THEN** a password dialog is shown; on correct entry the document loads normally

#### Scenario: Wrong password on open
- **WHEN** the user opens a password-protected PDF and supplies an incorrect password
- **THEN** an error message is shown and the document does not load

#### Scenario: Cancel password prompt
- **WHEN** the user dismisses the password dialog without entering a password
- **THEN** the document is not opened and the application returns to its previous state

### Requirement: User can decrypt an open encrypted PDF
The application SHALL allow the user to remove encryption from the currently open encrypted PDF and save it without a password.

#### Scenario: Decrypt via menu
- **WHEN** the user selects Document → Decrypt… on an encrypted document and confirms
- **THEN** the PDF is saved without encryption and can be reopened without a password

#### Scenario: Decrypt unavailable on unencrypted document
- **WHEN** the document is not encrypted
- **THEN** the Document → Decrypt… menu item is disabled
