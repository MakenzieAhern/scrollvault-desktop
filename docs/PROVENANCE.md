# Project provenance

## Original project

- Name: Virtual Scroll Access System (VSAS).
- Source: https://github.sydney.edu.au/SOFT2412-COMP9412-2025s2/A2-T04-05
- Imported snapshot: `a9e1992e86b76b8af9550329a0256f5a4b3d35de` (main).
- Original team, as listed in the source README: Aditya Bahl, Nicholas Huang Hong Shee, Yuchen Deng, and Lanfei Yan.
- Portfolio edition: https://github.com/MakenzieAhern/scrollvault-desktop

The source was cloned through Git. Its original history is preserved in a local Git bundle. This public repository starts with a curated snapshot because source files and history included personal account records, student identifiers, build outputs, and caches. No original authorship is inferred from the new snapshot's commit author.

## Evidence for Yuchen Deng's contribution

Source commit `6753f1ba31360c10d8b54f4209b17ad2b8fc6998` introduced the initial authentication service, user model and persistence layer, login and registration controllers/FXML, BCrypt password utility, and animated login/background changes. Subsequent source commits changed parts of those implementations. This is a team project, and commit counts do not establish each member's complete contribution.

The source README spells a contributor's name “Lanfei Yan”; an original contact screen used “Lanfei Yang”. This edition follows the README spelling.

## Public snapshot preparation

- Removed real user records, uploaded runtime data, student IDs, and generated artifacts from the published snapshot.
- Added an explicit synthetic demo generator. Demo addresses use the reserved `example.com` domain.
- Excluded an unused bundled hero image whose provenance was undocumented.
- Retained original source attribution and package names, while giving the public repository a functional name.
- Replaced the original test helpers and weak assertions where they concealed failures; documented the new verification separately.
- Kept migration and reliability improvements separate from historical contribution claims.

## Licensing

The original repository did not include a license granting general reuse. Its README's “License” paragraph described the educational context only. This portfolio copy therefore does not assign an MIT, Apache, or other open-source license to all contributors' work. Dependencies retain their own licenses.
