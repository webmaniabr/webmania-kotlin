# Synthetic regression fixtures

The operation and schema fixtures are shared with the Java SDK regression corpus. SocketFixture and FileFixtures provide local HTTP/TLS responses and request counters. These inputs use synthetic data and contain no production credentials, company records or real fiscal documents.

Encrypted PDF and ZIP samples exercise encryption metadata without decryption. Their expected hashes are recorded in file-fixtures.json.

The language consumer tests exercise scoped clients, typed models, exact numbers, presence and compiler boundaries. Fixture reuse does not establish independent runtime coverage.
